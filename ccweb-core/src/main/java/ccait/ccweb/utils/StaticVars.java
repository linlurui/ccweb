/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.utils;

public final class StaticVars {
    public static final String VARS_PATH = "org.springframework.web.servlet.HandlerMapping.uriTemplateVariables";
    public static final String CURRENT_DATASOURCE = "_CURRENT_DATASOURCE_";
    public static final String LOG_PRE_SUFFIX = "【ccweb】";
    public static final String LOG_PRE_SUFFIX_BY_SOCKET = "【ccweb-socket】";
    public static final String LOGIN_KEY = "_USER_LOGIN_";
    public static final String CURRENT_TABLE = "_CURRENT_TABLE_";
    public static final String CURRENT_MAX_PRIVILEGE_SCOPE = "_CURRENT_MAX_PRIVILEGE_SCOPE_";
    public static final String NO_PRIVILEGE_MESSAGE = "没有足够的权限执行该操作(No Privilege)";
    public static final String CURRENT_USER_ID_GROUPS = "CURRENT_USER_ID_GROUPS";
    public static final String CURRENT_USER_ID_SUB_GROUPS = "CURRENT_USER_ID_SUB_GROUPS";
}
