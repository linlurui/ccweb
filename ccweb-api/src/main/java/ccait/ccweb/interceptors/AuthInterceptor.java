/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.interceptors;

import ccait.ccweb.abstracts.AbstractPermissionInterceptor;
import ccait.ccweb.annotation.AccessCtrl;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.UserContext;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.*;
import ccait.ccweb.utils.EncryptionUtil;
import ccait.ccweb.utils.JwtUtils;
import ccait.ccweb.utils.PermissionUtils;
import com.auth0.jwt.JWT;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;

/**
 * @description 访问权限拦截器
 */
public class AuthInterceptor extends AbstractPermissionInterceptor implements HandlerInterceptor {

    @Value("${ccweb.security.admin.username:admin}")
    private String admin;

    @Value("${ccweb.ip.whiteList:}")
    private String whiteListText;

    @Value("${ccweb.ip.blackList:}")
    private String blackListText;

    @Value("${ccweb.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${ccweb.auth.user.jwt.enable:false}")
    private boolean jwtEnable;

    @Value("${ccweb.auth.user.aes.enable:false}")
    private boolean aesEnable;

    @Value("${ccweb.auth.user.wechat.enable:false}")
    private boolean wechatEnable;

    private static final Logger log = LoggerFactory.getLogger( AuthInterceptor.class );

    private static final Map<String, UserModel> userCacheMap = new ConcurrentHashMap<>();

    private boolean hasUploadFile;

    @PostConstruct
    private void construct() {
        whiteListText = ApplicationConfig.getInstance().get("${ccweb.ip.whiteList}", whiteListText);
        blackListText = ApplicationConfig.getInstance().get("${ccweb.ip.blackList}", blackListText);
        jwtEnable = ApplicationConfig.getInstance().get("${ccweb.auth.user.jwt.enable}", jwtEnable);
        aesEnable = ApplicationConfig.getInstance().get("${ccweb.auth.user.aes.enable}", aesEnable);
        wechatEnable = ApplicationConfig.getInstance().get("${ccweb.auth.user.wechat.enable}", wechatEnable);
        aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", aesPublicKey);
        admin = ApplicationConfig.getInstance().get("${ccweb.security.admin.username}", admin);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Map<String, String> attrs = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        PermissionUtils.InitLocalMap initLocalMap = new PermissionUtils.InitLocalMap(response, attrs).invoke();
        if (initLocalMap.is()) {
            return false;
        }

        String currentTable = initLocalMap.getCurrentTable();

        // session 中获取该用户的权限信息 并判断是否有权限
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if( user != null && user.getUsername().equals(admin) ) { //超级管理员
            log.info(String.format(LOG_PRE_SUFFIX + "超级管理员访问表[%s]！", initLocalMap.getCurrentTable()));
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + initLocalMap.getCurrentTable(), PrivilegeScope.ALL);
        }

        boolean result = check(request, response, handler, attrs, currentTable);

        return result;
    }

    private boolean check(HttpServletRequest request, HttpServletResponse response, Object handler, Map<String, String> attrs, String table) throws Exception {

        if("yes".equals(response.getHeader("finish"))) {
            return true;
        }

        if(!request.getRequestURI().endsWith("/login")) {
            loginByToken(request, response);
        }

        if(request instanceof CCWebRequestWrapper) {

            if (!vaildUploadFilesByInsert((CCWebRequestWrapper) request, table)) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                if(response.isCommitted()) {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "无效的上传文件格式!!!");
                }
                return false;
            }

            // 验证权限
            if (PermissionUtils.allowIp(request, whiteListText, blackListText) &&
                    this.hasPermission(handler, request.getMethod(), request, response, attrs, table)) {
                return true;
            }

            //  null == request.getHeader("x-requested-with") TODO 暂时用这个来判断是否为ajax请求
            // 如果没有权限 则抛401异常 springboot会处理，跳转到 /error/401 页面
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if(!response.isCommitted()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("has_not_privilege"));
            }
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, LangConfig.getInstance().get("has_not_privilege"));
        }
        else {
            return true;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // TODO
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // TODO
    }

    /**
     * 是否有权限
     *
     * @param handler
     * @return
     */
    private boolean hasPermission(Object handler, String method, HttpServletRequest request, HttpServletResponse response, Map<String, String> attrs, String table) throws Exception {

        boolean canAccess = true;
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

            if(StringUtils.isNotEmpty(table)) {
                canAccess = canAccessTable(method, (CCWebRequestWrapper) request, response, requiredPermission, attrs, table);
            }

            else {
                CCWebRequestWrapper requestWarpper = (CCWebRequestWrapper)request;
                if(requestWarpper.getParameters() == null) {
                    return true;
                }

                if(requestWarpper.getParameters() instanceof QueryInfo) {
                    QueryInfo queryInfo = JsonUtils.convert(requestWarpper.getParameters(), QueryInfo.class);
                    List<String> tableList = new ArrayList<String>();
                    if (queryInfo != null && queryInfo.getJoinTables() != null && queryInfo.getJoinTables().size() > 0) {
                        for (int i = 0; i < queryInfo.getJoinTables().size(); i++) {
                            if (StringUtils.isEmpty(queryInfo.getJoinTables().get(i).getTablename())) {
                                continue;
                            }
                            canAccess = canAccess && canAccessTable(method, (CCWebRequestWrapper) request, response, requiredPermission, attrs, queryInfo.getJoinTables().get(i).getTablename());
                        }
                    }
                }
                else {
                    UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
                    if(user == null) {
                        return false;
                    }

                    return true;
                }
            }
        }

        return canAccess;
    }

    private Boolean canAccessTable(String method, CCWebRequestWrapper request, HttpServletResponse response, AccessCtrl requiredPermission, Map<String, String> attrs, String table) throws Exception {

        if(requiredPermission == null) {
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, PrivilegeScope.ALL);
            return true;
        }

        // session 中获取该用户的权限信息 并判断是否有权限
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);

        if( user != null && user.getUsername().equals(admin) ) { //超级管理员
            log.info(String.format(LOG_PRE_SUFFIX + "超级管理员访问表[%s]，操作：%s！", table, method));
            ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, PrivilegeScope.ALL);
            return true;
        }

        if(Pattern.matches("^/(?i)(as)?(?i)api/[^/]+(/[^/]+)?/(?i)build$", request.getRequestURI())) {
            return false;
        }

        AclModel acl = new AclModel();
        acl.setTableName(table);
        List<AclModel> aclList = acl.where("[tableName]=#{tableName}").query();

        String message = null;
        if(aclList == null || aclList.size() < 1) {

            if(method.equals("GET") || (method.equals("POST") && !request.getRequestURI().endsWith("/delete") &&
                    !request.getRequestURI().endsWith("/upload") && !request.getRequestURI().endsWith("/update") &&
                    !request.getRequestURI().endsWith("/import"))) {
                log.info(String.format(LOG_PRE_SUFFIX + "表[%s]没有设置权限，允许查询！", table));

                if(!ApplicationContext.getThreadLocalMap().containsKey(CURRENT_MAX_PRIVILEGE_SCOPE) ||
                        PrivilegeScope.DENIED.equals(ApplicationContext.getThreadLocalMap().get(CURRENT_MAX_PRIVILEGE_SCOPE))) {
                    ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, PrivilegeScope.ALL);
                }

                return true;
            }

            else {
                if(!checkPrivilege(table, user, aclList, method, attrs, request.getParameters(), request, response)){
                    if(user == null) {
                        throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
                    }
                    message = String.format(LangConfig.getInstance().get("has_not_privilege_for_this_table"), user.getUsername(), table, method);
                    log.warn(LOG_PRE_SUFFIX + message);
                    throw new Exception(message);
                }
            }
        }

        List<Integer> groupIds = aclList.stream().map(a->a.getGroupId()).collect(Collectors.toList());

        GroupModel requiredPermissionGroup = new GroupModel();
        requiredPermissionGroup.setGroupName(requiredPermission.groupName());
        requiredPermissionGroup = requiredPermissionGroup.where("[groupName]=#{groupName}").first();

        if(requiredPermissionGroup != null && requiredPermissionGroup.getGroupId() != null &&
                !groupIds.contains(requiredPermissionGroup.getGroupId()) ) { //不属于指定组
            message = String.format(LangConfig.getInstance().get("this_group_can_not_edit_table"), requiredPermission.groupName(), table);
            log.warn(LOG_PRE_SUFFIX + message);
            throw new Exception(message);
        }

        if(user == null) {
            log.warn(LOG_PRE_SUFFIX + "用户未登录！！！");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if(!response.isCommitted()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            }
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, LangConfig.getInstance().get("has_not_privilege"));
        }

        boolean hasGroup = false;
        for (Integer groupId : groupIds) {
            Optional<UserGroupRoleModel> opt = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream()
                    .filter(a -> a.getGroup() == null || a.getGroup().getGroupId().equals(groupId)).findAny();

            if(opt != null && opt.isPresent()) {
                hasGroup = true;
                break;
            }
        }

        Optional<AclModel> optional = aclList.stream().filter(a->a.getGroupId() == null).findAny();
        boolean canAllAccess = (optional == null ? false : optional.isPresent());
        if( aclList.size() > 0 && !canAllAccess) {
            if(!hasGroup) {
                message = String.format(LangConfig.getInstance().get("user_has_not_group_for_acl_table"), user.getUsername(), table);
                log.warn(LOG_PRE_SUFFIX + message);
                throw new Exception(message);
            }
        }

        if(!checkPrivilege(table, user, aclList, method, attrs, request.getParameters(), request, response)){
            message = String.format(LangConfig.getInstance().get("has_not_privilege_for_this_table"), user.getUsername(), table, method);
            log.warn(LOG_PRE_SUFFIX + message);
            throw new Exception(message);
        }

        return true;
    }

    private boolean checkPrivilege(String table, UserModel user, List<AclModel> aclList, String method, Map<String, String> attrs,
                                   Object params, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if(user == null) {
            log.warn(LOG_PRE_SUFFIX + "用户未登录！！！");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            if(!response.isCommitted()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            }
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, LangConfig.getInstance().get("has_not_privilege"));
        }

        String privilegeWhere = null;
        switch (method.toUpperCase()) {
            case "GET":
                if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)download/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canDownload=1";
                }

                else if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)preview/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canPreview=1";
                }

                else if(Pattern.matches("^/(?i)(as)?(?i)api/(?i)play/[^/]+/[^/]+/[^/]+$", request.getRequestURI())) {
                    privilegeWhere = "canPlayVideo=1";
                }

                else {
                    privilegeWhere = "(canQuery=1 OR canList=1 OR canView=1 OR canDecrypt=1)";
                }
                break;
            case "POST":

                if(PermissionUtils.tablePattern.matcher(request.getRequestURI()).find()) {
                    break;
                }

                else if(PermissionUtils.updatePattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canUpdate=1";
                }

                else if(PermissionUtils.deletePattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canDelete=1";
                }

                else if(PermissionUtils.uploadPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canUpload=1";
                    break;
                }

                else if(PermissionUtils.importPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canImport=1";
                    break;
                }

                else if(PermissionUtils.exportPattern.matcher(request.getRequestURI()).find()) {
                    privilegeWhere = "canExport=1";
                }

                else {
                    QueryInfo queryInfo = JsonUtils.convert(params, QueryInfo.class);
                    if (queryInfo.getKeywords() == null || queryInfo.getKeywords().size() > 0 || queryInfo.getConditionList().size() > 0) {
                        privilegeWhere = "canQuery=1";
                    } else {
                        privilegeWhere = "canList=1";
                    }
                }
                break;
            case "PUT":
                String[] arr = request.getRequestURI().split("/");
                if(StringUtils.isEmpty(attrs.get("id")) && arr[arr.length - 1].toLowerCase() != "update") {
                    privilegeWhere = "canAdd=1";
                }

                else {
                    privilegeWhere = "canUpdate=1";
                }

                if(hasUploadFile) {
                    privilegeWhere += " AND canUpload=1";
                }
                break;
            case "DELETE":
                privilegeWhere = "canDelete=1";
                break;
        }

        if(StringUtils.isEmpty(privilegeWhere)) {
            return false;
        }

        PrivilegeModel privilege = new PrivilegeModel();
        List<String> roleIdList = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream().filter(a->a.getRoleId()!=null)
                .map(a->a.getRoleId().toString().replace("-","")).collect(Collectors.toList());

        roleIdList = roleIdList.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<Integer> aclIds = aclList.stream().filter(o-> table.equals(o.getTableName())).map(b->b.getAclId()).collect(Collectors.toList());
        aclIds = aclIds.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<PrivilegeModel> privilegeList = new ArrayList<PrivilegeModel>();
        if(roleIdList.size() > 0) {
            String roleWhere = String.format("[roleId] in ('%s')", String.join("','", roleIdList));
            if(aclList.size() > 0) {
                String aclWhere = String.format("(aclId in ('%s') OR aclId IS NULL OR aclId='')", String.join("','",
                        aclIds.stream().filter(o -> o != null).map(a -> a.toString().replace("-", ""))
                                .collect(Collectors.toList())));
                privilegeList = privilege.where(roleWhere).and(privilegeWhere)
                        .and(aclWhere).query();
            }
            else {
                privilegeList = privilege.where(roleWhere).and(privilegeWhere).query();
            }
        }

        else {
            if(aclList.size() > 0) {
                String aclWhere = String.format("(aclId in ('%s') OR aclId IS NULL OR aclId='')", String.join("','",
                        aclIds.stream().filter(o->o != null).map(a -> a.toString().replace("-", ""))
                                .collect(Collectors.toList())));

                privilegeList =  privilege.where(privilegeWhere)
                        .and(aclWhere).query();
            }
            else {
                privilegeList = privilege.where(privilegeWhere).query();
            }
        }

        boolean result = false;
        PrivilegeScope currentMaxScope = PrivilegeScope.DENIED;
        for (UserGroupRoleModel groupRole : UserContext.getUserGroupRoleModels(request, user.getUserId())) {
            Optional<PrivilegeModel> opt = privilegeList.stream().filter(a -> a.getRoleId().equals(groupRole.getRoleId()) &&
                    (a.getGroupId()==null || a.getGroupId().equals(groupRole.getGroupId()))).filter(a->a.getScope() != null)
                    .max(Comparator.comparing(b->b.getScope().getCode()));

            if(opt != null && opt.isPresent()) {

                if(opt.get().getScope().getCode() > currentMaxScope.getCode()) {
                    currentMaxScope = opt.get().getScope(); //求出最大权限
                }

                result = true;
            }
        }

        ApplicationContext.getThreadLocalMap().put(CURRENT_MAX_PRIVILEGE_SCOPE + table, currentMaxScope);

        return  result;
    }

    private void loginByToken(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final String token = request.getHeader(ApplicationConfig.getInstance().get("${ccweb.auth.header}", DEFAULT_AUTHORIZATION));
        if(StringUtils.isEmpty(token)) {
            return;
        }

        if(token.indexOf(".")>0) {
            if(jwtEnable) {
                try {
                    String username = JWT.decode(token).getClaim("username").asString();
                    UserModel user = new UserModel();
                    user.setUsername(username);
                    UserModel sessionUser = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
                    if(sessionUser != null && username.equals(sessionUser.getUsername())) {
                        Boolean verify = JwtUtils.isVerify(token, sessionUser);
                        if (!verify) {
                            removeCache(username);
                            throw new RuntimeException("非法访问！");
                        }
                        return;
                    }

                    if(userCacheMap.containsKey(username)) {
                        UserModel userCache = userCacheMap.get(username);
                        if(userCache != null && username.equals(userCache.getUsername())) {
                            Boolean verify = JwtUtils.isVerify(token, userCache);
                            if(!verify) {
                                removeCache(username);
                                throw new RuntimeException("非法访问！");
                            }
                            if(sessionUser == null) {
                                ApplicationContext.setSession(request, LOGIN_KEY, userCache);
                            }
                            return;
                        }
                    }

                    if (!user.where("username=#{username}").exist()) {
                        removeCache(username);
                        throw new RuntimeException("非法用户！");
                    }
                    user = user.where("username=#{username}").first();
                    Boolean verify = JwtUtils.isVerify(token, user);
                    if (!verify) {
                        removeCache(username);
                        throw new RuntimeException("非法访问！");
                    }

                    ApplicationContext.setSession(request, LOGIN_KEY, user);
                    userCacheMap.put(username, user);
                    return;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    if(!response.isCommitted()) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("has_not_privilege"));
                    }
                    throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, LangConfig.getInstance().get("login_please"));
                }
            }
            else {
                throw new RuntimeException("不支持的鉴权方式！");
            }
        }

        else if(aesEnable) {
            try {
                String decrypted = EncryptionUtil.decryptByAES(token, aesPublicKey);
                if (StringUtils.isEmpty(decrypted)) {
                    throw new RuntimeException("fail to get the token!!!");
                }

                String username = decrypted.substring(0, decrypted.length() - 32);
                String vaildCode = decrypted.substring(decrypted.length() - 32);

                UserModel user = new UserModel();
                user.setUsername(username);
                if (!user.where("username=#{username}").exist()) {
                    throw new RuntimeException("非法用户！");
                }
                user = user.where("username=#{username}").first();

                String userkey = request.getHeader(ApplicationConfig.getInstance().get("${ccweb.auth.userkey}", DEFAULT_USERKEY));
                String vaildCode2 = EncryptionUtil.md5(EncryptionUtil.encryptByAES(user.getUserId().toString(), userkey + aesPublicKey), "UTF-8");
                if(!vaildCode2.equals(vaildCode) || !userkey.equals(user.getKey())) {
                    throw new RuntimeException("fail to get the token!!!");
                }

                UserModel sessionUser = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
                if(sessionUser == null || user.getUserId() != sessionUser.getUserId()) {
                    UserContext.login(request, user);
                }

                return;
            }

            catch (Exception e) {
                log.error(e.getMessage(), e);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                if(!response.isCommitted()) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("has_not_privilege"));
                }
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, LangConfig.getInstance().get("login_please"));
            }
        }
    }

    private void removeCache(String username) {
        if (userCacheMap.containsKey(username)) {
            userCacheMap.remove(username);
        }
    }

    private boolean vaildUploadFilesByInsert(CCWebRequestWrapper request, String table) throws Exception {

        if(request.getParameters() == null) {
            return true;
        }

        if(!(request.getParameters() instanceof HashMap)) {
            return true;
        }

        HashMap<String, Object> data = (HashMap) request.getParameters();
        List<Map.Entry<String, Object>> files = data.entrySet().stream()
                .filter(a -> a.getValue() instanceof byte[]).collect(Collectors.toList());
        if(files == null || files.size() < 1) {
            return true;
        }

        hasUploadFile = true;
        ApplicationContext.getThreadLocalMap().put(HAS_UPLOAD_FILE, true);

        if(request.getRequestURI().toLowerCase().endsWith("/import") ||
                request.getRequestURI().toLowerCase().endsWith("/upload")) { //上传和导入接口不校验

            return true;
        }

        String currentDatasource = "default";
        if(ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }

        Map<String, Object> uploadConfigMap = ApplicationConfig.getInstance().getMap(String.format("ccweb.upload.%s.%s", currentDatasource, table));
        if(uploadConfigMap == null || uploadConfigMap.size() < 1) {
            return false;
        }

        request.setPostParameter(data);

        return true;
    }
}
