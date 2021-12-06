/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.trigger;


import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.DownloadData;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.ClassUtils;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.ColumnInfo;
import entity.query.annotation.Fieldname;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;
import static ccait.ccweb.utils.StaticVars.VARS_PATH;

@Component
@Scope("prototype")
@Trigger(tablename = "${ccweb.table.user}")
@Order(Ordered.HIGHEST_PRECEDENCE+666)
public final class UserTableTrigger implements ITrigger {

    private static final Logger log = LoggerFactory.getLogger( UserTableTrigger.class );

    @Value("${ccweb.security.admin.username:admin}")
    private String admin;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${ccweb.security.encrypt.MD5.publicKey:ccait}")
    private String md5PublicKey;

    @Value("${ccweb.table.reservedField.createBy:createBy}")
    private String createByField;

    private boolean isInsert = false;

    @PostConstruct
    private void construct() {
        admin = ApplicationConfig.getInstance().get("${ccweb.security.admin.username}", admin);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);
        md5PublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MD5.publicKey}", md5PublicKey);
        createByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField);
    }

    @Override
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {

        isInsert = true;
        for(Map<String, Object> data : list) {
            List<String> keys = data.keySet().stream().collect(Collectors.toList());
            for (String key : keys) {
                String lowerKey = key.toLowerCase();
                if (userIdField.toLowerCase().equals(lowerKey)) {
                    data.remove(key);
                }

                if ("status".equals(lowerKey) || data.get(key) == null) {
                    data.remove(key);
                    data.put("status", 0);
                }

                if ("username".equals(lowerKey) || data.get(key) == null) {
                    if (data.get(key) == null) {
                        throw new Exception(LangConfig.getInstance().get("username_can_not_be_empty"));
                    }

                    UserModel user = new UserModel();
                    user.setUsername(data.get(key).toString());
                    if (user.where("[username]=#{username}").exist()) {
                        throw new Exception(String.format(LangConfig.getInstance().get("username_already_exist"), data.get(key)));
                    }

                    data.put("key", EncryptionUtil.md5(user.getUsername(), md5PublicKey, "UTF-8"));
                }
            }

            if (!data.containsKey("status")) {
                data.put("status", 0);
            }

            String key = EncryptionUtil.md5(data.get("username").toString(), md5PublicKey, "UTF-8");
            data.put("key", key);
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(list);
    }

    @Override
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        Map<String, Object> data = queryInfo.getData();
        List<String> keys = data.keySet().stream().collect(Collectors.toList());
        for (String key : keys) {
            String lowerKey = key.toLowerCase();
            if("username".equals(lowerKey)) {
                data.remove(key);
            }

            if(userIdField.toLowerCase().equals(lowerKey)) {
                data.remove(key);
            }
        }

        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if(user == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        Map<String, Object> map = ApplicationConfig.getInstance().getMap("ccweb.validation");
        if(keys.contains("password") && map != null && map.containsKey("password")) {
            if(user.getPassword().equals(data.get("password"))) {
                data.remove("password");
            }
            else if(data.containsKey("confirmPassword") && user.getPassword().equals(data.get("confirmPassword"))) {
                data.remove("confirmPassword");
            }
            else {
                throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("username_or_password_is_invalid"));
            }
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        String[] arr = request.getRequestURI().split("/");
        if("update".equals(arr[arr.length - 1].toLowerCase())) {
            wrapper.setPostParameter(queryInfo);
        }

        else {
            wrapper.setPostParameter(data);
        }

        Map<String, String> attrs = (Map<String, String>)request.getAttribute(VARS_PATH);
        if(attrs != null && attrs.containsKey(userIdField)) {
            request.setAttribute(VARS_PATH, attrs);
        }
    }

    @Override
    public void onDelete(String id, HttpServletRequest request) throws Exception {

        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if(user == null) {
            return;
        }

        Map<String, String> attrs = (Map<String, String>)request.getAttribute(VARS_PATH);

        if(admin.equals(user.getUsername())) {
            UserModel currentUser = new UserModel();
            currentUser.setUserId(Integer.parseInt(attrs.get(userIdField)));
            currentUser = currentUser.where("[id]=#{id}").first();
            if(currentUser != null && currentUser.getUsername().equals(admin)) {
                throw new Exception(LangConfig.getInstance().get("admin_can_not_be_removed"));
            }
        }

        if(attrs.get(userIdField).equals(user.getUserId().toString())) {
            throw new Exception(LangConfig.getInstance().get("can_not_delete_self"));
        }

        request.setAttribute(VARS_PATH, attrs);
    }

    @Override
    public void onList(QueryInfo queryInfo, HttpServletRequest request) {

    }

    @Override
    public void onView(String id, HttpServletRequest request) {
        Map<String, String> data = (Map<String, String>)request.getAttribute(VARS_PATH);

        request.setAttribute(VARS_PATH, data);
    }

    @Override
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) {

    }

    @Override
    public void onResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {

    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(responseData.getData() == null) {
            return;
        }

        if(ClassUtils.isBaseType(responseData.getData())) {
            return;
        }

        if(isInsert) {
            UserModel userModel = new UserModel();
            userModel.setUserId(Integer.parseInt(responseData.getData().toString()));
            userModel.where(String.format("[%s]=#{%s}", userIdField, userIdField)).update(String.format("[%s]=#{%s}", createByField, userIdField));
        }
        if(request.getMethod().equalsIgnoreCase("GET") || request.getMethod().equalsIgnoreCase("POST")){

            List<Map> list = new ArrayList<Map>();

            boolean isMapResult = true;
            if(responseData.getData() instanceof List) {
                list = JsonUtils.convert(responseData.getData(), List.class);
                isMapResult = false;
            }

            else {
                Map map = JsonUtils.convert(responseData.getData(), Map.class);
                list.add(map);
            }

            for(int i=0; i<list.size(); i++) {

                List<String> keyList = (List) list.get(i).keySet().stream()
                        .filter(a -> "ID".equalsIgnoreCase(a.toString()) ||
                                "PASSWORD".equalsIgnoreCase(a.toString()))
                        .map(b -> b.toString()).collect(Collectors.toList());

                if (keyList == null) {
                    return;
                }

                for (String key : keyList) {
                    if(key.toUpperCase().equals("PASSWORD")) {
                        list.get(i).remove(key);
                    }
                }
            }

            if(isMapResult) {
                responseData.setData(list.get(0));
            }

            else {
                responseData.setData(list);
            }
        }
    }

    @Override
    public void onError(Exception ex, HttpServletRequest request) {

    }

    @Override
    public void onUpload(byte[] data, HttpServletRequest request) {

    }

    @Override
    public void onDownload(DownloadData data, HttpServletRequest request) {

    }

    @Override
    public void onPreviewDoc(DownloadData data, HttpServletRequest request) {

    }

    @Override
    public void onPlayVideo(DownloadData data, HttpServletRequest request) {

    }

    @Override
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {
        throw new Exception("can not build user table!!!");
    }
}
