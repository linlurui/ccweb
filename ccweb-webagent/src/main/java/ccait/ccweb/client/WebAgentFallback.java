package ccait.ccweb.client;

import ccait.ccweb.RestAgent;
import ccait.ccweb.model.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebAgentFallback implements WebAgentClient {

    private ResponseData responseData = null;

    @Autowired
    void init() {
        responseData = new ResponseData();
        responseData.setMessage("network error");
        responseData.setStatus(400);
    }

    @Override
    public ResponseData<RestAgent> test(String datasource, RestAgent restAgent) {
        return responseData;
    }

    @Override
    public ResponseData<String> sign(String datasource, RestAgent restAgent) {
        return responseData;
    }

    @Override
    public Object callback(String datasource, String table, String id, Map data) {
        return responseData;
    }
}