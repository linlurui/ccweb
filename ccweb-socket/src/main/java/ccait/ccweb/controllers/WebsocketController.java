/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.controllers;


import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.websocket.MessageBody;
import ccait.ccweb.websocket.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;


@RestController
public class WebsocketController {

    private static final Logger log = LoggerFactory.getLogger(WebsocketController.class);

    @Autowired
    private WebSocketClient wsClient;

    @Autowired
    protected HttpServletRequest request;


    /***
     * send message
     * @return
     */
    @ResponseBody
    @AccessCtrl
    @RequestMapping( value = "api/{datasource}/message/send", method = RequestMethod.POST )
    public ResponseData sendMessage(@RequestBody MessageBody messageBody) {

        try {
            if(request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY) == null) {
                throw new Exception(LangConfig.getInstance().get("login_please"));
            }
            wsClient.send(messageBody);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseData(500, e.getMessage());
        }

        return new ResponseData();
    }
}
