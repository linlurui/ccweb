package ccait.ccweb.controllers;

import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.model.AppConfig;
import ccait.ccweb.service.AppConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

@RestController
@RequestMapping(value = { "api/{datasource}/config" })
public class AppConfigController {

    @Autowired
    AppConfigService appConfigService;

    /***
     * app-config
     * @return
     */
    @AccessCtrl
    @ResponseBody
    @RequestMapping( value = "list/{applicationName}", method = RequestMethod.POST  )
    public List<AppConfig> getAppConfigs(@RequestBody Properties properties, @PathVariable String applicationName) throws SQLException {
        return appConfigService.getAppConfigList(properties, applicationName);
    }
}
