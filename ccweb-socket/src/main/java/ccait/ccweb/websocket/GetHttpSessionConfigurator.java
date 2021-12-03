/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */

package ccait.ccweb.websocket;

import ccait.ccweb.model.UserModel;
import entity.tool.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.util.List;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;

@Configuration
public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator {

    private static final Logger log = LoggerFactory.getLogger( GetHttpSessionConfigurator.class );

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {

        if(request.getHttpSession() instanceof  HttpSession) {
            HttpSession httpSession = (HttpSession) request.getHttpSession();
            if (httpSession != null) { //将HttpSession赋值给WebSocketServer使用
                config.getUserProperties().put(HttpSession.class.getName(), httpSession);
            }
        }

        if(!request.getHeaders().containsKey("current_user")) {
            return;
        }

        try {
            List<UserModel> users = JsonUtils.parse(request.getHeaders().get("current_user").toString(), List.class);
            if(users.size() < 1) {
                return;
            }
            //将HttpSession赋值给WebSocketServer使用
            if(request.getHttpSession() instanceof  HttpSession) {
                HttpSession httpSession = (HttpSession) request.getHttpSession();
                if (httpSession != null) { //将HttpSession赋值给WebSocketServer使用
                    config.getUserProperties().put(((HttpSession)request.getHttpSession()).getId(), users.get(0));
                }
            }


        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
