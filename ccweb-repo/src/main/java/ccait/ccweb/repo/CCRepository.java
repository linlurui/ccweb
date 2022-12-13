package ccait.ccweb.repo;

import ccait.ccweb.entites.DefaultEntity;
import entity.query.Queryable;
import entity.query.core.DBTransaction;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.tool.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.util.Hashtable;
import java.util.Map;

@Scope("prototype")
@org.springframework.stereotype.Repository
public class CCRepository {

    private static final Logger log = LoggerFactory.getLogger( CCRepository.class );

    private boolean hasSession = false;

    private ThreadLocal<Hashtable<String, DataSource>> dataSourceMap = new ThreadLocal();

    public CCRepository() {
        init();
    }

    @PostConstruct
    private void init() {
        if(dataSourceMap.get() == null) {
            dataSourceMap.set(new Hashtable<>());
        }
    }

    public CCRepository create() {
        return new CCRepository();
    }

    public void openSession() throws Exception {
        if(hasSession) {
            return;
        }
        releaseSession();
        hasSession = true;
        Queryable queryable = new DefaultEntity();
        DataSource ds = queryable.dataSource();
        ds.beginTransaction();
        dataSourceMap.get().put(ds.getId(), ds);
    }

    public <T extends Queryable> T get(T obj) throws Exception {
        if(!hasSession) {
            return obj;
        }
        DataSource ds = DataSourceFactory.getInstance().getDataSource(obj.getClass());
        if(!dataSourceMap.get().containsKey(ds.getId())) {
            dataSourceMap.get().put(ds.getId(), ds);
        }

        if(obj instanceof Queryable) {
            Queryable<T> queryable = (Queryable<T>) obj;
            queryable.setConnection(dataSourceMap.get().get(ds.getId()).getConnection(!hasSession));
            queryable.setTransaction(dataSourceMap.get().get(ds.getId()).getTransaction());
        }
        return obj;
    }

    public <T> T get(Class<T> clazz) throws Exception {
        if(!hasSession) {
            return  ReflectionUtils.getInstance(clazz);
        }

        DataSource ds = DataSourceFactory.getInstance().getDataSource(clazz);
        if(!dataSourceMap.get().containsKey(ds.getId())) {
            dataSourceMap.get().put(ds.getId(), ds);
        }

        Queryable<T> queryable = (Queryable<T>) ReflectionUtils.getInstance(clazz);
        queryable.setConnection(dataSourceMap.get().get(ds.getId()).getConnection(!hasSession));
        queryable.setTransaction(dataSourceMap.get().get(ds.getId()).getTransaction());

        return (T) queryable;
    }

    public <T extends Queryable> Connection getConnection(Class<T> clazz) throws Exception {
        DataSource ds = DataSourceFactory.getInstance().getDataSource(clazz);
        if(!dataSourceMap.get().containsKey(ds.getId())) {
            dataSourceMap.get().put(ds.getId(), ds);
        }

        return dataSourceMap.get().get(ds.getId()).getConnection(!hasSession);
    }

    public <T extends Queryable> DBTransaction getDBTransaction(Class<T> clazz) throws Exception {
        DataSource ds = DataSourceFactory.getInstance().getDataSource(clazz);
        if(!dataSourceMap.get().containsKey(ds.getId())) {
            dataSourceMap.get().put(ds.getId(), ds);
        }

        return dataSourceMap.get().get(ds.getId()).getTransaction();
    }

    public void rollback() {
        releaseSession();
    }

    public void commit() {
        if(dataSourceMap.get() == null) {
            dataSourceMap.set(new Hashtable<>());
        }
        for(Map.Entry<String, DataSource> item : dataSourceMap.get().entrySet()) {
            item.getValue().commit();
        }
        releaseSession();
    }

    private void releaseSession() {
        if(dataSourceMap.get() == null) {
            dataSourceMap.set(new Hashtable<>());
        }
        for(Map.Entry<String, DataSource> item : dataSourceMap.get().entrySet()) {
            item.getValue().rollback();
        }
        hasSession = false;
        dataSourceMap.get().clear();
    }
}
