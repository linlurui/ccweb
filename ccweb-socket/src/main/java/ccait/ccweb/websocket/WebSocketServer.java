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
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.core.ApplicationConfig;

import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.tomcat.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;


@Component
@ServerEndpoint(value="/ccws", configurator=GetHttpSessionConfigurator.class)
@ConditionalOnProperty(prefix = "websocket", name = "enable", havingValue = "true")
public class WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final AtomicInteger OnlineCount = new AtomicInteger(0);
    private final static Map<String, Session> sessionMap = new HashMap<>();
    private final static Map<String, UserModel> sessionUserMap = new HashMap<>();

    private UserModel currentUser;

    @Value("${ccweb.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${ccweb.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Value("${ccweb.table.reservedField.roleId:roleId}")
    private String roleIdField;

    @Value("${ccweb.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${ccweb.table.userGroupRole:userGroupRole}")
    private String userGroupRoleTable;

    @Value("${ccweb.encoding:UTF-8}")
    private String encoding;

    //连接超时
    public static final long MAX_TIME_OUT = 60 * 60 * 24 * 1000;

    @PostConstruct
    public void init() {
        aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", aesPublicKey);
        groupIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.groupId}", groupIdField);
        userPathField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userPath}", userPathField);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);
        userGroupRoleTable = ApplicationConfig.getInstance().get("${ccweb.table.userGroupRole}", userGroupRoleTable);
        encoding = ApplicationConfig.getInstance().get("${ccweb.encoding}", encoding);
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig conf) {

        session.setMaxIdleTimeout( MAX_TIME_OUT );

        HttpSession httpSession = getHttpSession(conf);

        try {
            if(conf.getUserProperties().containsKey(httpSession.getId())) {
                UserModel userModel = JsonUtils.parse(conf.getUserProperties().get(httpSession.getId()).toString(), UserModel.class);
                if(userModel != null) {
                    httpSession.setAttribute(httpSession.getId() + LOGIN_KEY, userModel);
                }
            }

            if (httpSession.getAttribute(httpSession.getId() + LOGIN_KEY) != null) {
                log.info(httpSession.getAttribute(httpSession.getId() + LOGIN_KEY).toString());
                if(httpSession.getAttribute(httpSession.getId() + LOGIN_KEY) instanceof String) {
                    currentUser = JsonUtils.parse(httpSession.getAttribute(httpSession.getId() + LOGIN_KEY).toString(), UserModel.class);
                }
                else {
                    currentUser = (UserModel) httpSession.getAttribute(httpSession.getId() + LOGIN_KEY);
                }

                if(currentUser != null) {
                    sessionUserMap.put(httpSession.getId(), currentUser);
                    sessionMap.put(((WsSession)session).getHttpSessionId(), session);

                    int cnt = OnlineCount.incrementAndGet(); // 在线数加1
                    log.info(LOG_PRE_SUFFIX_BY_SOCKET + String.format("有连接加入，当前连接数为：%s", cnt));
                }
            }
        }
        catch (Exception e) {
            log.error("HttpSession Error=====>", e);
        }
    }

    private HttpSession getHttpSession(EndpointConfig conf) {
        String sessionKey = HttpSession.class.getName();
        if(!conf.getUserProperties().containsKey(sessionKey)) {
            return null;
        }

        if(!(conf.getUserProperties().get(sessionKey) instanceof HttpSession)) {
            return null;
        }

        return (HttpSession) conf.getUserProperties().get(sessionKey);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        try {
            List<String> ids = sessionMap.entrySet().stream()
                    .filter(a->a.getValue().getId().equals(session.getId()))
                    .map(b->b.getKey()).collect(Collectors.toList());

            if(ids != null) {
                for (String id :ids) {
                    sessionUserMap.remove(id);
                    sessionMap.remove(id);
                }
            }
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }

        int cnt = OnlineCount.decrementAndGet();
        log.info(LOG_PRE_SUFFIX_BY_SOCKET + String.format("有连接关闭，当前连接数为：%s", cnt));
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param text
     * 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String text, Session session) {
        log.info(LOG_PRE_SUFFIX_BY_SOCKET + String.format("来自客户端的消息：%s", text));
        try
        {
            if(!sessionMap.containsKey(((WsSession)session).getHttpSessionId())) {
                sendMessage(session, LangConfig.getInstance().get("login_please"));
                return;
            }
            if(StringUtils.isEmpty( text )) {
                sendMessage(session, LangConfig.getInstance().get("message_can_not_be_empty"));
                return;
            }

            MessageBody body = JsonUtils.parse(text, MessageBody.class);
            if(StringUtils.isEmpty(body.getMessage())) {
                sendMessage(session, LangConfig.getInstance().get("message_can_not_be_empty"));
                return;
            }

            if(body != null) {
                switch (body.getSendMode()) {
                    case ALL:
                        sendToAll(body.getMessage());
                        break;
                    case USER:
                        sendToUser(body.getReceiver(), body.getMessage());
                        break;
                    case GROUP:
                        sendToGroup(body.getReceiver(), body.getMessage());
                        break;
                    case ROLE:
                        sendToRole(body.getReceiver(), body.getMessage());
                        break;
                }
                return;
            }

            throw new Exception("无效的消息格式");

        } catch ( Exception e )
        {
            log.error(LOG_PRE_SUFFIX_BY_SOCKET + String.format("接收消息发生错误：%s",e.getMessage()));

            sendMessage(session, e.getMessage());
        }
    }

    /**
     * 出现错误
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error(LOG_PRE_SUFFIX_BY_SOCKET + String.format("发生错误：%s，Session ID： %s",error.getMessage(),session.getId()));
    }

    private void sendToRole(ReceiverInfo receiver, String message) {
        List<Integer> userIdList = getUserIdList(receiver);
        List<Integer> finalUserIdList = userIdList;
        List<String> ids = sessionUserMap.entrySet().stream()
                .filter(a -> {
                    if(finalUserIdList.contains(a.getValue().getUserId())) {
                        return true;
                    }
                    return false;
                }).map(c->c.getKey()).collect(Collectors.toList());

        List<Session> list = sessionMap.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    private void sendToGroup(ReceiverInfo receiver, String message) {
        List<Integer> userIdList = getUserIdList(receiver);
        List<Integer> finalUserIdList = userIdList;
        List<String> ids = sessionUserMap.entrySet().stream()
                .filter(a -> {
                    if(finalUserIdList.contains(a.getValue().getUserId())) {
                        return true;
                    }
                    return false;
                }).map(c->c.getKey()).collect(Collectors.toList());

        List<Session> list = sessionMap.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    private void sendToUser(ReceiverInfo receiver, String message) {

        List<String> ids = sessionUserMap.entrySet().stream()
                .filter(a->receiver.getUsernames().contains(a.getValue().getUsername()))
                .map(b->b.getKey()).collect(Collectors.toList());

        List<Session> list = sessionMap.entrySet().stream()
                .filter(a->ids.contains(a.getKey())).map(b->b.getValue())
                .collect(Collectors.toList());

        for (Session session : list) {
            sendMessage(session, message);
        }
    }

    public List<Integer> getUserIdList(ReceiverInfo receiver) {
        UserGroupRoleModel userGroupModel = new UserGroupRoleModel();
        String aesFields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.fields}", "");
        List<String> aesFieldList = StringUtils.splitString2List(aesFields, ",");
        String groupIdString = receiver.getGroupId();
        if(aesFieldList.stream()
                .filter(b-> b.equalsIgnoreCase(String.format("%s.%s", userGroupRoleTable, groupIdField)) ||
                        b.equalsIgnoreCase(groupIdField)).isParallel()) {
            groupIdString = EncryptionUtil.decryptByAES(groupIdString, aesPublicKey);
        }

        userGroupModel.setGroupId(Integer.parseInt(groupIdString));
        List<Integer> userIdList = null;

        int roleId = 0;
        if(StringUtils.isNotEmpty(receiver.getRoleId())) {
            roleId = Integer.parseInt(receiver.getRoleId());
        }

        int groupId = 0;
        if(StringUtils.isNotEmpty(receiver.getGroupId())) {
            groupId = Integer.parseInt(receiver.getGroupId());
        }

        try {
            if(roleId > 0 && groupId > 0) {
                userIdList = userGroupModel
                        .where(groupIdField + "=#{groupId}")
                        .and(roleIdField + "=#{roleId}")
                        .groupby("")
                        .select(userIdField)
                        .query(Integer.class);
            }
            else if(groupId > 0) {
                userIdList = userGroupModel
                        .where(groupIdField + "=#{groupId}")
                        .groupby("")
                        .select(userIdField)
                        .query(Integer.class);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return userIdList;
    }

    /**
     * 发送消息，实践表明，每次浏览器刷新，session会发生变化。
     * @param session
     * @param message
     */
    public static void sendMessage(Session session, String message) {
        try {
            if(session == null) {
                return;
            }

            if(!session.isOpen()) {
                return;
            }

            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX_BY_SOCKET + String.format("发送消息出错：%s, 消息内容：%s", e.getMessage(), message));
            e.printStackTrace();
        }
    }

    /**
     * 群发消息
     * @param message
     * @throws IOException
     */
    public static void sendToAll(String message) throws Exception {
        for (Map.Entry<String, Session> session : sessionMap.entrySet()) {
            sendMessage(session.getValue(), message);
        }
    }

    /**
     * 指定Session发送消息
     * @param httpSessionId
     * @param message
     * @throws IOException
     */
    public static void sendMessage(String httpSessionId,String message) throws Exception {
        Session session = sessionMap.get( httpSessionId );
        if(session!=null){
            sendMessage(session, message);
        }
        else{
            log.warn(LOG_PRE_SUFFIX_BY_SOCKET + String.format("没有找到你指定ID的会话：%s",httpSessionId));
        }
    }

    public static List<UserModel> getUsers() {
        return sessionUserMap.entrySet().stream().map(a-> a.getValue()).collect(Collectors.toList());
    }
}
