/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.context;

import ccait.ccweb.dynamic.DynamicClassBuilder;
import ccait.ccweb.entites.PrimaryKeyInfo;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.entites.TableInfo;
import ccait.ccweb.utils.StaticVars;
import entity.query.ColumnInfo;
import entity.query.Queryable;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.tool.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static ccait.ccweb.utils.StaticVars.*;
import static ccait.ccweb.utils.StaticVars.CURRENT_DATASOURCE;

@Order(-55555)
public final class EntityContext {

    @Value(value = "${ccweb.suffix:Entity}")
    private String suffix;

    private static final Logger log = LoggerFactory.getLogger( EntityContext.class );

    private final Map<String, String> tableMap = new HashMap<String, String>();

    private static final Map<Class<?>, List<Field>> objectFieldsMap = new HashMap<Class<?>, List<Field>>() ;

    private static final Map<String, Map<String, List<ColumnInfo>>> tableColumnsMap = new HashMap<String, Map<String, List<ColumnInfo>>>();

    @PostConstruct
    private void postConstruct() {

        initTableColumnsCache();
        org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
        String[] names = app.getBeanNamesForAnnotation(Tablename.class);
        if(names != null) {
            for(String name : names) {

                String key = name;
                if(StringUtils.isNotEmpty(suffix) && name.length() > suffix.length() &&
                        name.substring(name.length() - suffix.length()).equals(suffix)) {
                    key = name.substring(0, name.length() - suffix.length());
                }

                tableMap.put(key.toLowerCase(), name);
            }
        }

        log.info(LOG_PRE_SUFFIX + "实体类上下文 ： [EntityContext]", "初始化完成");
    }

    public static Object getEntity(String tablename, List<String> fieldList) throws SQLException {
        return getEntity(getCurrentDatasourceId(), tablename, fieldList);
    }

    public static Object getEntity(String datasource, String tablename, List<String> fieldList) {

        Object bean = getEntity(tablename);
        if(bean == null) {
            if(fieldList == null) {
                return null;
            }

            List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
            for(String item : fieldList) {

                ColumnInfo col = new ColumnInfo();
                col.setColumnName(item);
                col.setType(String.class);

                if("id".equals(item.toLowerCase())) {
                    col.setIsPrimaryKey(true);
                }

                columns.add(col);
            }

            bean = DynamicClassBuilder.create(datasource, tablename, columns);
        }

        return bean;
    }

    public static Object getEntity(String tablename, QueryInfo queryInfo) throws SQLException {

        Object bean = getEntity(tablename);
        if(bean == null) {
            bean = DynamicClassBuilder.create(tablename, queryInfo);
        }

        return bean;
    }

    public static Object getEntity(String tablename, Map<String, Object> data, String id) throws SQLException {

        data.put("id", id);
        return getEntity(tablename, data);
    }

    public static Object getEntity(String tablename, Map<String, Object> data) throws SQLException {

        Object bean = getEntity(tablename);
        if(bean == null) {
            bean = DynamicClassBuilder.create(tablename, data);
        }

        return bean;
    }

    public static Object getEntityId(String datasourceId, String tablename, String id) throws SQLException {
        Object bean = getEntity(tablename);
        if(bean == null) {
            List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

            addColumnById(datasourceId, tablename, id, columns);

            bean = DynamicClassBuilder.create(tablename, columns);
        }

        return bean;
    }

    public static ColumnInfo getPrimaryKey(String datasourceId, String table) {
        if(!tableColumnsMap.containsKey(datasourceId)) {
            return null;
        }

        if(!tableColumnsMap.get(datasourceId).containsKey(table)) {
            return null;
        }

        Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                .filter(a->a.getIsPrimaryKey()==true).findAny();

        if(optional.isPresent()) {
            ColumnInfo info = optional.get();
            if(info == null) {
                return null;
            }

            return info;
        }

        return null;
    }

    public static boolean hasColumn(String datasourceId, String table, String columnName) {
        if(!tableColumnsMap.containsKey(datasourceId)) {
            return false;
        }

        if(!tableColumnsMap.get(datasourceId).containsKey(table)) {
            return false;
        }

        Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                .filter(a->a.getColumnName().equals(columnName)).findAny();

        if(optional.isPresent()) {
            ColumnInfo info = optional.get();
            if(info == null) {
                return false;
            }

            return true;
        }

        return false;
    }

    private static void addColumnById(String datasourceId, String table, String id, List<ColumnInfo> columns) {

        String fieldname = "id";
        if(tableColumnsMap.containsKey(datasourceId) && tableColumnsMap.get(datasourceId).containsKey(table)) {
            Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                    .filter(a -> a.getIsPrimaryKey() == true).findAny();

            if(optional.isPresent()) {
                ColumnInfo column = optional.get();
                fieldname = column.getColumnName();
            }
        }

        if(Pattern.matches("^[0-9]{1,32}$", id)) {
            columns.add(new ColumnInfo(fieldname, "integer", true));
        }

        else if(Pattern.matches("^\\d+$", id)) {
            columns.add(new ColumnInfo(fieldname, "long", true));
        }

        else {
            columns.add(new ColumnInfo(fieldname, "text", true));
        }
    }

    private static Object getEntity(String tablename) {
        tablename = tablename.trim();
        org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
        EntityContext entityContext = (EntityContext) app.getAutowireCapableBeanFactory().getBean("entityContext");

        Object bean = null;
        if(entityContext.tableMap.containsKey(tablename)) {
            String entityName = entityContext.tableMap.get(tablename);

            bean = app.getAutowireCapableBeanFactory().getBean(entityName);
        }

        return bean;
    }

    public static PrimaryKeyInfo getPrimaryKeyInfo(Object instance) {

        PrimaryKeyInfo result = new PrimaryKeyInfo();
        List<Field> fields = getFields(instance);
        for(Field fld : fields) {
            PrimaryKey ann = fld.getAnnotation(PrimaryKey.class);
            if(ann == null) {
                continue;
            }

            result.setPrimaryKey(ann);
            result.setField(fld);

            return result;
        }

        return null;
    }

    public static List<Field> getFields(Object instance) {

        Class<?> cls = instance.getClass();
        if(objectFieldsMap.containsKey(cls)) {
            return objectFieldsMap.get(cls);
        }

        List<Field> result = new ArrayList<Field>();
        Field[] fields = cls.getDeclaredFields();

        if(fields == null) {
            return result;
        }

        for(Field fld : fields) {
            result.add(fld);
        }

        if(result.size() > 0) {
            synchronized (objectFieldsMap) {
                objectFieldsMap.put(cls, result);
            }
        }

        return result;
    }

    public static String getCurrentTable() {
        Map map = ApplicationContext.getThreadLocalMap();
        if(!map.containsKey(CURRENT_TABLE)) {
            return "";
        }

        return map.get(CURRENT_TABLE).toString();
    }

    public static String getCurrentDatasourceId() throws SQLException, BeansException {
        String currentDatasource;
        if (ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        else {
            currentDatasource = DataSourceFactory.getInstance().getDataSource().getId();
        }

        return currentDatasource;
    }

    public static List<ColumnInfo> getColumns(String table) throws SQLException {
        return getColumns(getCurrentDatasourceId(), table);
    }

    public static List<ColumnInfo> getColumns(String datasource, String table) {

        if(!tableColumnsMap.containsKey(datasource) ||
                !tableColumnsMap.get(datasource).containsKey(table)) {

            List<ColumnInfo> columns = Queryable.getColumns(datasource, table);
            String primaryKey = Queryable.getPrimaryKey(datasource, table);
            for(ColumnInfo column : columns) {
                if(column.getColumnName().equals(primaryKey)) {
                    column.setIsPrimaryKey(Boolean.TRUE);
                }
            }
            tableColumnsMap.put(datasource, new HashMap<String, List<ColumnInfo>>());
            tableColumnsMap.get(datasource).put(table, columns);
        }

        return tableColumnsMap.get(datasource).get(table);
    }

    public static String getColumnName(Field fld) {
        Fieldname ann = fld.getAnnotation(Fieldname.class);
        if(ann == null) {
            return fld.getName();
        }

        return ApplicationConfig.getInstance().get(ann.value());
    }

    public void initTableColumnsCache() {
        try {
            String configPath = System.getProperty("user.dir") + "/src/main/resources/"
                    + ApplicationConfig.getInstance().get("entity.queryable.configFile", "db-config.xml");

            Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource(configPath);
            if(dsList == null || dsList.size() < 1) {
                //tomcat路径
                String property = System.getProperty("catalina.home");
                String path =property+ File.separator + "conf" + File.separator + "db-config.xml";
                dsList = DataSourceFactory.getInstance().getAllDataSource(path);
            }

            for(DataSource ds : dsList) {
                try {
                    if(ds.getConnection() == null) {
                        continue;
                    }

                    List<TableInfo> tablenames = Queryable.getTables(ds.getId(), TableInfo.class);
                    for (TableInfo tb : tablenames) {

                        if (StringUtils.isEmpty(tb.getTablename())) {
                            continue;
                        }

                        List<ColumnInfo> columns = Queryable.getColumns(ds.getId(), tb.getTablename());
                        String primaryKey = Queryable.getPrimaryKey(ds.getId(), tb.getTablename());
                        for(ColumnInfo column : columns) {
                            if(column.getColumnName().equals(primaryKey)) {
                                column.setIsPrimaryKey(Boolean.TRUE);
                            }
                        }
                        if (!tableColumnsMap.containsKey(ds.getId())) {
                            tableColumnsMap.put(ds.getId(), new HashMap<String, List<ColumnInfo>>());
                        }
                        tableColumnsMap.get(ds.getId()).put(tb.getTablename(), columns);
                    }
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
