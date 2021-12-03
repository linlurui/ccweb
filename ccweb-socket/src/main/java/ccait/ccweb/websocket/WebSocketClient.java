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



import ccait.ccweb.config.LangConfig;
import entity.tool.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@Component
@ClientEndpoint
public class WebSocketClient extends Endpoint {
    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    //连接超时
    public static final long MAX_TIME_OUT = 60 * 60 * 24 * 1000;

    @Autowired
    protected HttpServletRequest request;

    @Value("${websocket.server:}")
    private String server;

    @Value("${websocket.port:}")
    private String port;

    @Value("${websocket.protocol:}")
    private String protocol;

    private String websocket_url;
    private Session session;

    @PostConstruct
    private void init() {
        websocket_url = protocol + "://" + server + ":" + port + "/ccws";
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        log.debug(LOG_PRE_SUFFIX + "连接成功");
        session.setMaxIdleTimeout(MAX_TIME_OUT);
        session.addMessageHandler(new MessageHandler.Whole<String>() {

            /** * 有返回信息时触发 * */
            @OnMessage
            @Override
            public void onMessage(String message) {
                if(LangConfig.getInstance().get("login_please").equals(message)) {
                    log.warn(message);
                    return;
                }
                log.info(LOG_PRE_SUFFIX + "返回信息：" + message);
            }
        });
    }

    @Override
    public void onError(Session session, Throwable t) {
        log.error(LOG_PRE_SUFFIX + "失败：" + t.getMessage(), t);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        log.warn(LOG_PRE_SUFFIX + "Websocket连接已关闭......");
    }

    public synchronized <T> void send(T data) {
        String message = "";
        if(data instanceof String) {
            message = data.toString();
        }

        else {
            message = JsonUtils.toJson(data);
        }

        try {
            if(session == null || !session.isOpen()) {
                session = connect(data);
            }
            session.getBasicRemote().sendText(message);// 发送信息
        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + String.format("WebSocket(%s)创建连接出错：%s", websocket_url, e.getMessage()), e);
        }
    }

    private <T> Session connect(T data) throws DeploymentException, IOException, URISyntaxException {

        if(StringUtils.isEmpty(websocket_url)) {
            throw new IOException("connection has been released!!!");
        }

        ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            public void beforeRequest(Map<String, List<String>> headers) {
                if(null != request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY)){
                    List<String> value = new ArrayList<>();
                    value.add(JsonUtils.toJson(request.getSession().getAttribute(request.getSession().getId() + LOGIN_KEY)));
                    // 设置header
                    headers.put("current_user", value);
                }
            }
        }).build();

        // 创建会话
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(WebSocketClient.class, clientEndpointConfig, new URI(websocket_url));
        session.getUserProperties().put(HttpSession.class.getName(), request.getSession());
        session.setMaxBinaryMessageBufferSize(1024000);
        session.setMaxTextMessageBufferSize(1024000);

        return session;
    }
}
