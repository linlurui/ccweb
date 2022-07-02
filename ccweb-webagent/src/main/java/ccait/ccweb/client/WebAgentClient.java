package ccait.ccweb.client;


import ccait.ccweb.RestAgent;
import ccait.ccweb.config.FeignConfig;
import ccait.ccweb.model.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(name = "ccweb-agent", path = "api/{datasource}", fallback = WebAgentFallback.class, configuration = { FeignConfig.class })
interface WebAgentClient {
    @RequestMapping( value = "test/agent", method = {RequestMethod.POST}  )
    ResponseData<RestAgent> test(@PathVariable String datasource, @RequestBody RestAgent restAgent);

    @RequestMapping( value = "test/sign", method = {RequestMethod.POST}  )
    ResponseData<String> sign(@PathVariable String datasource, @RequestBody RestAgent restAgent);

    @RequestMapping( value = "callback/{datasource}/{table}/{id}", method = {RequestMethod.POST}  )
    Object callback(@PathVariable String datasource, @PathVariable String table, @PathVariable String id, @RequestBody Map data);
}
