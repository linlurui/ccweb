package ccait.ccweb.service;

import ccait.ccweb.model.AppConfig;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

@Component
public class AppConfigService {
    public List<AppConfig> getAppConfigList(Properties properties, String applicationName) throws SQLException {
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
}
