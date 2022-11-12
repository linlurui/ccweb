package ccait.ccweb.processor;

import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.model.AppConfig;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import entity.query.ColumnInfo;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Resource
@Component
public class ApplicationConfigProcessor implements EnvironmentPostProcessor {

    private static final String ENCODING = "UTF-8";
    private static final Logger log = LoggerFactory.getLogger( ApplicationConfigProcessor.class );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {
        File file = getApplicationConfig(".yml");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadPropertiesByYaml(file));
            return;
        }

        file = getApplicationConfig(".yaml");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadPropertiesByYaml(file));
            return;
        }

        file = getApplicationConfig(".properties");
        if(file != null  && file.exists()) {
            setPropertys(configurableEnvironment, loadProperties(file));
            return;
        }
    }

    public Properties getApplicationPropertiesByResource() {
        URL url = this.getClass().getClassLoader().getResource("application.yml");
        if(url == null) {
            return null;
        }
        String path = url.getPath();
        File file = new File(path);
        if(file.exists()) {
            return this.loadPropertiesByYaml(file);
        }
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yml");
        if(inputStream != null) {
            return this.loadPropertiesByYaml(inputStream);
        }

        url = this.getClass().getClassLoader().getResource("application.yaml");
        if(url == null) {
            return null;
        }
        path = url.getPath();
        file = new File(path);
        if(file.exists()) {
            return this.loadPropertiesByYaml(file);
        }
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml");
        if(inputStream != null) {
            return this.loadPropertiesByYaml(inputStream);
        }

        return null;
    }

    public File getApplicationConfig(String suffix) {
        //tomcat路径
        String property = System.getProperty("catalina.home");
        String path =property+ File.separator + "conf" + File.separator+"application" + suffix;
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        else {
            file = new File(property+ File.separator + "config" + File.separator+"application" + suffix);
        }

        if (file.exists()) {
            return file;
        }
        else {
            file = new File(property + File.separator + "application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator + "conf"+File.separator+"application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir")  + File.separator + "config" + File.separator + "application" + suffix);
        }

        if (file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator + "application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator + "resources" + File.separator + "application" + suffix);
        }

        if(file.exists()) {
            return file;
        }
        else {
            file = new File(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "application" + suffix);
        }
        return file;
    }

    private void setPropertys(ConfigurableEnvironment configurableEnvironment, Properties properties) {
        Properties resource = getApplicationPropertiesByResource();
        if(resource != null) {
            properties = this.mergePropertys(resource, properties);
        }

        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();

        String configTable = ApplicationConfig.getInstance().get("${ccweb.app-config.table}", "");
        try {
            fillPropertiesFromDatabase(configTable, properties);
        }
        catch (Exception e) {
            System.out.println(String.format("读取配置数据表%s发生异常: %s", configTable, e.toString()));
            log.error(String.format("读取配置数据表%s发生异常", configTable), e);
        }

        //以外部配置文件为准
        propertySources.addFirst(new PropertiesPropertySource("Config", properties));
        //以application.properties文件为准
        //propertySources.addLast(new PropertiesPropertySource("Config", properties));
    }

    private Properties mergePropertys(Properties srcProperties, Properties targetProperties) {
        for(Object key : srcProperties.keySet()) {
            if(!targetProperties.containsKey(key)) {
                targetProperties.put(key, srcProperties.get(key));
                ApplicationConfig.getInstance().set(key.toString(), targetProperties.get(key).toString());
            }
        }

        return targetProperties;
    }

    private void fillPropertiesFromDatabase(String configTable, Properties properties) throws Exception {

        String applicationName = ApplicationConfig.getInstance().get("${spring.application.name}", "");

        if (StringUtils.isEmpty(applicationName) || StringUtils.isEmpty(configTable)) {
            return;
        }

        log.info(String.format("Fill properties from database table [%s] for %s...", configTable, applicationName));
        ensureConfigTable(configTable);
        List<AppConfig> appConfigs = getAppConfigList(properties, applicationName);
        for (AppConfig appConfig : appConfigs) {
            if (appConfig.getKey().startsWith("entity.datasource") ||
                    "ccweb.account".equalsIgnoreCase(appConfig.getKey()) ||
                    "ccweb.license".equalsIgnoreCase(appConfig.getKey())) {
                continue;
            }
            properties.setProperty(appConfig.getKey(), appConfig.getValue());
            ApplicationConfig.getInstance().set(appConfig.getKey(), appConfig.getValue());
        }
    }

    private List<AppConfig> getAppConfigList(Properties properties, String applicationName) throws SQLException {
        AppConfig appConfigEntity = new AppConfig();
        appConfigEntity.setService(applicationName);
        for(Object key : properties.keySet()) {
            appConfigEntity.setKey(key.toString());
            appConfigEntity.setValue(properties.getProperty(key.toString()));
            if(!appConfigEntity.where("[service]=#{service}").and("[key]=#{key}").exist()) {
                appConfigEntity.insert();
            }
        }

        List<AppConfig> appConfigs = appConfigEntity.where("[service]=#{service}").query();
        return appConfigs;
    }

    private void ensureConfigTable(String configTable) throws Exception {
        log.info("ensure config table!!!");
        List<String> tables = null;
        DataSource ds = ApplicationContext.getDefaultDataSource(null);
        if(StringUtils.isEmpty(ds.getId())) {
            return;
        }

        tables = Queryable.getTables(ds.getId()).stream().map(a-> a.getTableName()).collect(Collectors.toList());
        if(!tables.stream().anyMatch(a->a.equals(configTable))) {
            List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
            columns.add(new ColumnInfo(){{
                setColumnName("id");
                setIsPrimaryKey(true);
                setIsAutoIncrement(true);
                setCanNotNull(true);
                setMaxLength(32);
                setType(Integer.class);
                setColumnComment("主键");
            }});
            columns.add(new ColumnInfo(){{
                setColumnName("service");
                setCanNotNull(true);
                setMaxLength(64);
                setUnique(true);
                setType(String.class);
            }});
            columns.add(new ColumnInfo(){{
                setColumnName("key");
                setCanNotNull(true);
                setUnique(true);
                setMaxLength(650);
                setType(String.class);
            }});
            columns.add(new ColumnInfo(){{
                setColumnName("value");
                setType(String.class);
                setMaxLength(1024);
            }});

            log.info(ds.getId()+ "." + configTable + " has been creating!!!");
            if(!ApplicationContext.existTable(ds.getId(), configTable)) {
                Queryable.createTable(ds.getId(), configTable, columns);
            }
        }
    }

    private Properties loadProperties(File f) {
        FileSystemResource resource = new FileSystemResource(f);
        try {
            return PropertiesLoaderUtils.loadProperties(resource);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to load local settings from " + f.getAbsolutePath(), ex);
        }
    }

    public Properties loadPropertiesByYaml(File file) {
        try {
            return this.loadPropertiesByYaml(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            log.error("Can not find the application config ======>>> ", e);
        }

        return null;
    }

    public Properties loadPropertiesByYaml(InputStream stream) {
        final String DOT = ".";
        Properties result = new Properties();
        try {
            YAMLFactory yamlFactory = new YAMLFactory();
            YAMLParser parser = yamlFactory.createParser(
                    new InputStreamReader(stream, Charset.forName(ENCODING)));

            String key = "";
            String value = null;
            JsonToken token = parser.nextToken();
            while (token != null) {
                if (JsonToken.START_OBJECT.equals(token)) {
                    // do nothing
                } else if (JsonToken.FIELD_NAME.equals(token)) {
                    if (key.length() > 0) {
                        key = key + DOT;
                    }
                    key = key + parser.getCurrentName();

                    token = parser.nextToken();
                    if (JsonToken.START_OBJECT.equals(token)) {
                        continue;
                    }
                    value = parser.getText();
                    result.setProperty(key, value);

                    int dotOffset = key.lastIndexOf(DOT);
                    if (dotOffset > 0) {
                        key = key.substring(0, dotOffset);
                    }
                    value = null;
                } else if (JsonToken.END_OBJECT.equals(token)) {
                    int dotOffset = key.lastIndexOf(DOT);
                    if (dotOffset > 0) {
                        key = key.substring(0, dotOffset);
                    } else {
                        key = "";
                    }
                }
                token = parser.nextToken();
            }
            parser.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
