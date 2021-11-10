package ccait.ccweb.abstracts;

import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.PermissionUtils;
import ccait.ccweb.utils.StaticVars;
import entity.query.ColumnInfo;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static ccait.ccweb.utils.StaticVars.*;
import static ccait.ccweb.utils.StaticVars.CURRENT_MAX_PRIVILEGE_SCOPE;

public abstract class AbstractPermissionInterceptor {

    @Value("${ccweb.security.admin.username:admin}")
    private String admin;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        boolean result = isChecked();
        if(!result) {
            setChecked(true);
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法上的注解
            AccessCtrl requiredPermission = handlerMethod.getMethod().getAnnotation(AccessCtrl.class);
            // 如果方法上的注解为空 则获取类的注解
            if (requiredPermission == null) {
                requiredPermission = handlerMethod.getMethod().getDeclaringClass().getAnnotation(AccessCtrl.class);
            }

            if(requiredPermission == null) {
                return true;
            }
        }

        Map<String, String> attrs = (Map<String, String>) request.getAttribute(StaticVars.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        PermissionUtils.InitLocalMap initLocalMap = new PermissionUtils.InitLocalMap(response, attrs).invoke();
        if (initLocalMap.is()) {
            return false;
        }

        String currentTable = initLocalMap.getCurrentTable();

        if(StringUtils.isEmpty(currentTable)) {
            return true;
        }

        // session 中获取该用户的权限信息 并判断是否有权限
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if( user != null && user.getUsername().equals(admin) ) { //超级管理员
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + initLocalMap.getCurrentTable(), PrivilegeScope.ALL);
        }

        //执行触发器
        runTrigger(currentTable, request.getMethod(), attrs, ((CCWebRequestWrapper) request).getParameters(), request);

        if(Boolean.valueOf(true).equals(ApplicationContext.getThreadLocalMap().get(StaticVars.RESPONSE_END))) {
            return false;
        }

        return true;
    }

    private void runTrigger(String table, String method, Map<String, String > attrs, Object object, HttpServletRequest request) throws Exception {

        String postString = JsonUtils.toJson(object);
        switch (method.toUpperCase()) {
            case "GET":
                TriggerContext.exec(table, EventType.View, attrs.get("id"), request);
                break;
            case "POST":
                if(PermissionUtils.tablePattern.matcher(request.getRequestURI()).find()) {
                    List<ColumnInfo> columns = JsonUtils.toList(postString, ColumnInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, columns, request);
                    break;
                }

                else if(PermissionUtils.viewPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = JsonUtils.parse(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.BuildTable, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.uploadPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = JsonUtils.parse(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Upload, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.importPattern.matcher(request.getRequestURI()).find()) {
                    TriggerContext.exec(table, EventType.Import, postString.getBytes("ISO-8859-1"), request);
                    break;
                }

                else if(PermissionUtils.exportPattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = JsonUtils.parse(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Export, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.updatePattern.matcher(request.getRequestURI()).find()) {
                    QueryInfo queryInfo = JsonUtils.parse(postString, QueryInfo.class);
                    TriggerContext.exec(table, EventType.Update, queryInfo, request);
                    break;
                }

                else if(PermissionUtils.deletePattern.matcher(request.getRequestURI()).find()) {
                    List idList = JsonUtils.parse(postString, List.class);
                    for(Object id : idList) {
                        TriggerContext.exec(table, EventType.Delete, id.toString(), request);
                    }
                    break;
                }

                QueryInfo queryInfo = JsonUtils.parse(postString, QueryInfo.class);
                if(!Pattern.compile("\"(conditionList|sortList|groupList|keywords|joinTables|selectList|data)\"\\s*:",
                        Pattern.MULTILINE).matcher(postString).find()) {

                    queryInfo.setData(JsonUtils.parse(postString, Map.class));
                }
                if((queryInfo.getKeywords() != null && queryInfo.getKeywords().size() > 0) ||
                        (queryInfo.getConditionList() != null && queryInfo.getConditionList().size() > 0)) {
                    TriggerContext.exec(table, EventType.Query, queryInfo, request);
                }

                else {
                    TriggerContext.exec(table, EventType.List, queryInfo, request);
                }
                break;
            case "PUT":
                if(StringUtils.isEmpty(attrs.get("id"))) {
                    if(Pattern.matches("^\\s*\\[[^\\[\\]]+\\]\\s*$", postString)) {
                        List<Map<String, Object>> params = JsonUtils.parse(postString, List.class);
                        TriggerContext.exec(table, EventType.Insert, params, request);
                    }

                    else {
                        Map data = JsonUtils.parse(postString, Map.class);
                        TriggerContext.exec(table, EventType.Insert, data, request);
                    }
                }

                else {
                    Map data = JsonUtils.parse(postString, Map.class);
                    TriggerContext.exec(table, EventType.Update, data, request);
                }

                if(Boolean.valueOf(true).equals(ApplicationContext.getThreadLocalMap().get(HAS_UPLOAD_FILE))) {
                    TriggerContext.exec(table, EventType.Upload, postString.getBytes("ISO-8859-1"), request);
                }
                break;
            case "DELETE":
                if(StringUtils.isNotEmpty(attrs.get("id"))) {
                    TriggerContext.exec(table, EventType.Delete, attrs.get("id"), request);
                }

                else {
                    TriggerContext.exec(table, EventType.Delete, JsonUtils.parse(postString, List.class), request);
                }
                break;
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    private boolean checked;
}
