package ccait.ccweb.context;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import ccait.ccweb.utils.JwtUtils;
import entity.query.Where;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import static ccait.ccweb.utils.StaticVars.*;

public class UserContext {

    private static final Logger log = LoggerFactory.getLogger(UserContext.class);

    public static UserModel login(String username, String passwordEncode, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(passwordEncode)) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            throw new Exception(LangConfig.getInstance().get("username_and_password_can_not_be_empty"));
        }

        UserModel user = new UserModel();

        user.setUsername(username);
        user.setPassword(passwordEncode);

        Where<UserModel> where = user.where("[username]=#{username}").and("[password]=#{password}");

        user = where.first();

        return UserContext.login(request, user);
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
        Long jwtMillis = Long.valueOf(ApplicationConfig.getInstance().get("${ccweb.auth.user.jwt.millis}", "600000"));
        String aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", "ccait");

        if(jwtEnable) {
            String token = JwtUtils.createJWT(jwtMillis, user);
            user.setToken(token);
        }

        else if(aesEnable) {
            String vaildCode2 = EncryptionUtil.md5(EncryptionUtil.encryptByAES(user.getUserId().toString(), user.getUsername() + aesPublicKey), "UTF-8");
            String token = EncryptionUtil.encryptByAES(user.getUsername() + vaildCode2, aesPublicKey);
            user.setToken(token);
        }

        user.setPassword("******");

        ApplicationContext.setSession(request, LOGIN_KEY, user);
        ApplicationContext.getUserIdByAllGroups(request, user);

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
            return JsonUtils.toList(JsonUtils.toJson(list), UserGroupRoleModel.class);
        }

        UserGroupRoleModel model = new UserGroupRoleModel();
        model.setUserId(userId);

        List<UserGroupRoleModel> userGroupRoleModels = model.where("[userId]=#{userId}").orderby("createOn desc").query();

//    List<String> roleIdList = userGroupRoleModels.stream().filter(a-> a.getRoleId() != null)
//            .map(b-> b.getRoleId().toString().replace("-", ""))
//            .collect(Collectors.toList());

        ApplicationContext.setSession(request, "UserGroupRoleModels", userGroupRoleModels);

        return userGroupRoleModels;
    }
}
