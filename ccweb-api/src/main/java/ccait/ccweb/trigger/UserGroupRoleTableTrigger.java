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
import ccait.ccweb.context.UserContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.DownloadData;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.ClassUtils;
import entity.query.ColumnInfo;
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
import java.util.*;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;

@Component
@Scope("request")
@Trigger(tablename = "${ccweb.table.userGroupRole}")
@Order(Ordered.HIGHEST_PRECEDENCE+666)
public final class UserGroupRoleTableTrigger implements ITrigger {

    private static final Logger log = LoggerFactory.getLogger( UserGroupRoleTableTrigger.class );

    @Value("${ccweb.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${ccweb.table.reservedField.userGroupRoleId:userGroupRoleId}")
    private String userGroupRoleIdField;

    @Value("${ccweb.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Value("${ccweb.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${ccweb.security.admin.username:admin}")
    protected String admin;

    @PostConstruct
    private void init() {
        userGroupRoleIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userGroupRoleId}", userGroupRoleIdField);
        aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", aesPublicKey);
        groupIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.groupId}", groupIdField);
        userPathField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userPath}", userPathField);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);

    }

    @Override
    public void onSave(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        this.onUpdate(queryInfo, request);
    }

    @Override
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {
        for(Map<String, Object> data : list) {

            if(!data.containsKey(userIdField) || data.get(userIdField) == null) {
                throw new Exception(LangConfig.getInstance().get("set_value_by_userId_please"));
            }

            UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
            if(user == null) {
                throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
            }

            data.put(userGroupRoleIdField, UUID.randomUUID().toString().replace("-", ""));

            if(admin.equals(user.getUsername())) {
                String path = String.format("%s/%s", user.getUserId(), data.get(userIdField));
                data.put(userPathField, path);
                continue;
            }

            if(!data.containsKey(groupIdField) || data.get(groupIdField) == null) {
                continue;
            }

            Integer groupId = Integer.parseInt(data.get(groupIdField).toString());
            Optional<String> optional = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream()
                    .filter(a -> a.getGroupId() == groupId).map(b -> b.getPath()).findFirst();
            if(!optional.isPresent()) {
                throw new Exception(LangConfig.getInstance().get("set_value_by_groupId_please"));
            }

            String path = String.format("%s/%s", optional.get(), data.get(userIdField));

            data.put(userPathField, path);
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(list);
    }

    @Override
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) {

        Map<String, Object> data = queryInfo.getData();
        if(data.containsKey(userGroupRoleIdField)) {
            data.remove(userGroupRoleIdField);
        }
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        String[] arr = request.getRequestURI().split("/");
        if("update".equals(arr[arr.length - 1].toLowerCase()) || "save".equals(arr[arr.length - 1].toLowerCase())) {
            wrapper.setPostParameter(queryInfo);
        }

        else {
            wrapper.setPostParameter(data);
        }
    }

    @Override
    public void onDelete(String id, HttpServletRequest request) {

    }

    @Override
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws IOException {

    }

    @Override
    public void onView(String id, HttpServletRequest request) {}

    @Override
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws IOException {

    }

    @Override
    public void onResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {

    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(request.getMethod().equalsIgnoreCase("GET") || request.getMethod().equalsIgnoreCase("POST")){

            if(responseData.getData() == null) {
                return;
            }

            if(ClassUtils.isBaseType(responseData.getData())) {
                return;
            }

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
        throw new Exception("can not build userGroupRole table!!!");
    }
}
