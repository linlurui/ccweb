package ccait.ccweb.context;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.model.AclModel;
import ccait.ccweb.model.PrivilegeModel;
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import ccait.ccweb.utils.JwtUtils;
import entity.query.Where;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;

public class UserContext {

    private static final Logger log = LoggerFactory.getLogger(UserContext.class);

    public static UserModel login(String username, String passwordEncode, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(passwordEncode)) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw new Exception(LangConfig.getInstance().get("username_and_password_can_not_be_empty"));
        }

        try {
            UserModel user = new UserModel();

            user.setUsername(username);
            user.setPassword(passwordEncode);

            Where<UserModel> where = user.where("[username]=#{username}").and("[password]=#{password}");

            user = where.first();

            return UserContext.login(request, user);
        }
        catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    public static UserModel login(HttpServletRequest request, UserModel user) throws Exception {
        if(user == null) {
            throw new Exception(LangConfig.getInstance().get("username_or_password_is_invalid"));
        }

        if(user.getStatus() != null && !user.getStatus().equals(0)) {
            throw new Exception(LangConfig.getInstance().get("user_status_has_been_frozen"));
        }

        List<UserGroupRoleModel> userGroupRoleModelList = UserContext.getUserGroupRoleModels(request, user.getUserId());
        userGroupRoleModelList.stream().forEach((item)->{
            item.getGroup();
            item.getRole();
        });
        user.setUserGroupRoleModels(userGroupRoleModelList);

        Boolean jwtEnable = Boolean.valueOf(ApplicationConfig.getInstance().get("${ccweb.auth.user.jwt.enable}", "false"));
        Boolean aesEnable = Boolean.valueOf(ApplicationConfig.getInstance().get("${ccweb.auth.user.aes.enable}", "false"));
        Long jwtMillis = Long.valueOf(ApplicationConfig.getInstance().get("${ccweb.auth.user.jwt.millis}", "43200000"));
        String aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", "ccait");

        if(ApplicationConfig.getInstance().getMap("ccweb.auth.user.jwt.millis").containsKey("millis")) {
            jwtMillis = Long.valueOf(ApplicationConfig.getInstance().getMap("ccweb.auth.user.jwt.millis").get("millis").toString());
        }

        if(jwtEnable) {
            String token = JwtUtils.createJWT(jwtMillis, user);
            user.setJwtToken(token);
        }

        if(aesEnable) {
            if(StringUtils.isEmpty(user.getKey())) {
                throw new Exception(LangConfig.getInstance().get("can_not_find_the_user_key"));
            }
            String vaildCode2 = EncryptionUtil.md5(EncryptionUtil.encryptByAES(user.getUserId().toString(), user.getKey() + aesPublicKey), "UTF-8");
            String token = EncryptionUtil.encryptByAES(user.getUsername() + vaildCode2, aesPublicKey);
            user.setAesToken(token);
        }

        ApplicationContext.setSession(request, LOGIN_KEY, user);
        ApplicationContext.getUserIdByAllGroups(request, user);

        user.setPassword("******");

        return user;
    }

    public static void setLoginUser(HttpServletRequest request, UserModel data) {
        ApplicationContext.setSession( request, LOGIN_KEY, data );
        ApplicationContext.setSession( request, CURRENT_USER_ID_GROUPS, null);
        ApplicationContext.setSession( request, CURRENT_USER_ID_SUB_GROUPS, null);
    }

    /***
     * user logout
     */
    public static void logout(HttpServletRequest request){
        setLoginUser(request, null);
    }

    public static List<UserGroupRoleModel> getUserGroupRoleModels(HttpServletRequest request, Integer userId) throws SQLException, IOException {

        List list = ApplicationContext.getSession(request, "UserGroupRoleModels", List.class);
        if(list != null && list.size() > 0) {
            List<UserGroupRoleModel> userGroupRoleModels = JsonUtils.toList(JsonUtils.toJson(list), UserGroupRoleModel.class);
            list = userGroupRoleModels.stream().filter(a-> a.getUserId() == userId).collect(Collectors.toList());
            if(list != null && list.size() > 0) {
                return JsonUtils.toList(JsonUtils.toJson(list), UserGroupRoleModel.class);
            }
        }

        UserGroupRoleModel model = new UserGroupRoleModel();
        model.setUserId(userId);

        String userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", "userId");
        List<UserGroupRoleModel> userGroupRoleModels = model.where(userIdField + "=#{userId}").orderby("createOn desc").query();

//    List<String> roleIdList = userGroupRoleModels.stream().filter(a-> a.getRoleId() != null)
//            .map(b-> b.getRoleId().toString().replace("-", ""))
//            .collect(Collectors.toList());

        ApplicationContext.setSession(request, "UserGroupRoleModels", userGroupRoleModels);

        return userGroupRoleModels;
    }

    public static Map<String, PrivilegeModel> getPrivilegeMap(HttpServletRequest request) throws IOException, SQLException {
        Map<String, PrivilegeModel> result = new HashMap<>();
        Map<String, PrivilegeModel> adminPrivileges = new HashMap<>();
        UserModel user = ApplicationContext.getSession( request, LOGIN_KEY, UserModel.class );
        if(user == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }
        List<AclModel> aclList = new AclModel().where("1=1").query();
        if( ApplicationConfig.getInstance().get("${ccweb.security.admin.username}") != null &&
                user.getUsername().equals(ApplicationConfig.getInstance().get("${ccweb.security.admin.username}")) ) { //超级管理员
            aclList.forEach(a-> {
                adminPrivileges.put(a.getTableName(), new PrivilegeModel() {{
                    setCanList(1);
                    setCanDecrypt(1);
                    setCanImport(1);
                    setCanUpload(1);
                    setCanExport(1);
                    setCanUpdate(1);
                    setCanPreview(1);
                    setCanPlayVideo(1);
                    setCanDownload(1);
                    setCanAdd(1);
                    setCanDelete(1);
                    setCanQuery(1);
                    setCanView(1);
                    setScope(PrivilegeScope.PARENT_AND_CHILD);
                }});
            });

            return adminPrivileges;
        }

        PrivilegeModel privilege = new PrivilegeModel();
        List<String> roleIdList = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream().filter(a->a.getRoleId()!=null)
                .map(a->a.getRoleId().toString().replace("-","")).collect(Collectors.toList());

        roleIdList = roleIdList.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<Integer> groupIds = aclList.stream().filter(o-> o.getGroupId() != null).map(b->b.getGroupId()).collect(Collectors.toList());
        groupIds = groupIds.stream().collect(
                Collectors.collectingAndThen(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(o -> o))), ArrayList::new));

        List<PrivilegeModel> privilegeList = new ArrayList<PrivilegeModel>();
        if(roleIdList.size() > 0) {
            String roleWhere = String.format("[roleId] in ('%s')", String.join("','", roleIdList));
            if(aclList.size() > 0) {
                String groupsString = String.format("(groupId in ('%s') OR groupId IS NULL OR groupId='')", String.join("','",
                        groupIds.stream().filter(o -> o != null).map(a -> a.toString().replace("-", ""))
                                .collect(Collectors.toList())));
                privilegeList = privilege.where(roleWhere)
                        .and(groupsString).query();
            }
            else {
                privilegeList = privilege.where(roleWhere).query();
            }
        }

        else {
            if(aclList.size() > 0) {
                String groupString = String.format("(groupId in ('%s') OR groupId IS NULL OR groupId='')", String.join("','",
                        groupIds.stream().filter(o->o != null).map(a -> a.toString().replace("-", ""))
                                .collect(Collectors.toList())));

                privilegeList =  privilege.where(groupString).query();
            }
            else {
                return adminPrivileges;
            }
        }

        privilegeList.stream().collect(Collectors.groupingBy(PrivilegeModel::getAclId));

        privilegeList.forEach(a-> {
            Optional<String> tableOpt = aclList.stream().filter(b -> b.getAclId() == a.getAclId()).map(c -> c.getTableName()).findFirst();
            if(!tableOpt.isPresent() || StringUtils.isEmpty(tableOpt.get())) {
                return;
            }

            if(result.containsKey(tableOpt.get())) {
                PrivilegeModel b = result.get(tableOpt.get());
                a.setCanList(a.getCanList() | b.getCanList());
                a.setCanDecrypt(a.getCanDecrypt() | b.getCanDecrypt());
                a.setCanImport(a.getCanImport() | b.getCanImport());
                a.setCanUpload(a.getCanUpload() | b.getCanUpload());
                a.setCanExport(a.getCanExport() | b.getCanExport());
                a.setCanUpdate(a.getCanUpdate() | b.getCanUpdate());
                a.setCanPreview(a.getCanPreview() | b.getCanPreview());
                a.setCanPlayVideo(a.getCanPlayVideo() | b.getCanPlayVideo());
                a.setCanDownload(a.getCanDownload() | b.getCanDownload());
                a.setCanAdd(a.getCanAdd() | b.getCanAdd());
                a.setCanDelete(a.getCanDelete() | b.getCanDelete());
                a.setCanQuery(a.getCanQuery() | b.getCanQuery());
                a.setCanView(a.getCanView() | b.getCanView());
                a.setScope(b.getScope().getCode()>a.getScope().getCode()? b.getScope(): a.getScope());
                a.setRoleId(0);
                a.setAclId(0);
                a.setPrivilegeId(0);
                a.setGroupId(0);
            }

            result.put(tableOpt.get(), a);
        });

        return result;
    }
}
