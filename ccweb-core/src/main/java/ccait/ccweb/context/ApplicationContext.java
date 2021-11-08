/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.context;


import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.annotation.*;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.tool.util.JsonUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.join;


@Service
public class ApplicationContext implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger( ApplicationContext.class );

    private static org.springframework.context.ApplicationContext instance;

    public static org.springframework.context.ApplicationContext getInstance() {
        return instance;
    }

    private List<String> allTables = new ArrayList<String>();

    public static final String TABLE_USER = "${ccweb.table.user}";
    public static final String TABLE_GROUP = "${ccweb.table.group}";
    public static final String TABLE_ROLE = "${ccweb.table.role}";
    public static final String TABLE_USER_GROUP_ROLE = "${ccweb.table.userGroupRole}";
    public static final String TABLE_ACL = "${ccweb.table.acl}";
    public static final String TABLE_PRIVILEGE = "${ccweb.table.privilege}";

    public static boolean isEnableRedisSession() {
        return enableRedisSession;
    }

    public static void setEnableRedisSession(boolean enableRedisSession) {
        ApplicationContext.enableRedisSession = enableRedisSession;
    }

    private static boolean enableRedisSession;

    public static Map<String, Object> getThreadLocalMap() {

        if(threadLocal.get() == null) {
            threadLocal.set(new HashMap<String, Object>());
        }

        return threadLocal.get();
    }

    private final static InheritableThreadLocal<Map<String, Object>> threadLocal = new InheritableThreadLocal<Map<String, Object>>();
    static {
        threadLocal.set(new HashMap<String, Object>());
    }

    @Value(TABLE_USER)
    private String userTablename;

    @Value(TABLE_GROUP)
    private String groupTablename;

    @Value(TABLE_USER_GROUP_ROLE)
    private String userGroupRoleTablename;

    @Value(TABLE_ROLE)
    private String roleTablename;

    @Value(TABLE_ACL)
    private String aclTablename;

    @Value(TABLE_PRIVILEGE)
    private String privilegeTablename;

    @Value("${entity.datasource.configFile:db-config.xml}")
    private String configFile;

    @Value("${ccweb.security.encrypt.MD5.fields:}")
    private String md5Fields;

    @Value("${ccweb.security.encrypt.MD5.publicKey:ccait}")
    private String md5PublicKey;

    @Value("${ccweb.security.encrypt.BASE64.fields:}")
    private String base64Fields;

    @Value("${ccweb.security.encrypt.MAC.fields:}")
    private String macFields;

    @Value("${ccweb.security.encrypt.SHA.fields:}")
    private String shaFields;

    @Value("${ccweb.security.encrypt.MAC.publicKey:ccait}")
    private String macPublicKey;

    @Value("${ccweb.security.encrypt.AES.fields:}")
    private String aesFields;

    @Value("${ccweb.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${ccweb.security.admin.username:admin}")
    private String admin;

    @Value("${ccweb.security.admin.password:}")
    private String password;

    @Value("${ccweb.encoding:UTF-8}")
    private String encoding;


    @Value("${ccweb.table.reservedField.createOn:createOn}")
    private String createOnField;

    @Value("${ccweb.table.reservedField.modifyOn:modifyOn}")
    private String modifyOnField;

    @Value("${ccweb.table.reservedField.modifyBy:modifyBy}")
    private String modifyByField;

    @Value("${ccweb.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${ccweb.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    @Value("${ccweb.table.reservedField.roleId:roleId}")
    private String roleIdField;

    @Value("${ccweb.table.reservedField.createBy:createBy}")
    private String createByField;

    public static String adminName;

    @PostConstruct
    private void init() {
        admin = ApplicationConfig.getInstance().get("${ccweb.security.admin.username}", admin);
        adminName = admin;
    }

    public static void dispose() {
        threadLocal.remove();
    }

    /**
     * 通过名称获取bean
     */
    public static Object getBean(String name) {
        return instance.getBean(name);
    }


    /**
     * 通过类型获取bean
     */
    public static Object getBean(Class<?> clazz) {
        return instance.getBean(clazz);
    }

    /**
     * 判断某个bean是不是存在
     */
    public static boolean hasBean(String name) {
        return instance.containsBean(name);
    }

    private void ensureInjectValues() {
        configFile = ApplicationConfig.getInstance().get("${entity.datasource.configFile}", configFile);
        md5Fields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MD5.fields}", md5Fields);
        md5PublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MD5.publicKey}", md5PublicKey);
        base64Fields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.BASE64.fields}", base64Fields);
        macFields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MAC.fields}", macFields);
        shaFields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.SHA.fields}", shaFields);
        macPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MAC.publicKey}", macPublicKey);
        aesFields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.fields}", aesFields);
        aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", aesPublicKey);
        admin = ApplicationConfig.getInstance().get("${ccweb.security.admin.username}", admin);
        password = ApplicationConfig.getInstance().get("${ccweb.security.admin.password}", password);
        encoding = ApplicationConfig.getInstance().get("${ccweb.encoding}", encoding);
        createOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createOn}", createOnField);
        modifyOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyOn}", modifyOnField);
        modifyByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyBy}", modifyByField);
        groupIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.groupId}", groupIdField);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);
        roleIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.roleId}", roleIdField);
        createByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField);
    }

    /**
     * 实现该接口用来初始化应用程序上下文
     * 该接口会在执行完毕@PostConstruct的方法后被执行
     * 接着，会进行Mapper地址扫描并加载，就是RequestMapping中指定的那个路径
     *
     * @param applicationContext 应用程序上下文
     * @throws BeansException beans异常
     */
    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext) throws BeansException {
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 ： [{}]", "开始初始化");

        ensureInjectValues();
        this.instance = applicationContext;


        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getId ： [{}]", applicationContext.getId());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getApplicationName ： [{}]", applicationContext.getApplicationName());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getAutowireCapableBeanFactory ： [{}]", applicationContext.getAutowireCapableBeanFactory());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getDisplayName ： [{}]", applicationContext.getDisplayName());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getParent ： [{}]", applicationContext.getParent());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getStartupDate ： [{}]", applicationContext.getStartupDate());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 getEnvironment ： [{}]",applicationContext.getEnvironment());
        log.info(LOG_PRE_SUFFIX + "应用程序上下文 ： [ApplicationContext]", "初始化完成");

        initDatasource(applicationContext);
    }

    public void initDatasource(org.springframework.context.ApplicationContext applicationContext) {
        try {
            DataSource ds = getDefaultDataSource(configFile);

            if(StringUtils.isEmpty(userTablename)) {
                throw new Exception("请设置用户表名称(Setting ${ccweb.table.user} in application.yml pls)");
            }

            if(!"n1ql_jdbc".equals(ds.getDriverClassName())) {

                allTables = Queryable.getTables(ds.getId());

                List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
                ColumnInfo col = null;

                createUserTable(ds, columns);
                createGroupTable(ds, columns);
                createRoleTable(ds, columns);
                createAclTable(ds, columns);
                createPrivilegeTable(ds, columns);
                createUserGroupRoleTable(ds, columns);
            }

            String pwd = getEncryptPassword();
            UserModel user = new UserModel();
            user.setCreateBy(Integer.valueOf(0));
            user.setStatus(0);
            user.setCreateOn(Datetime.getTime());
            user.setUsername(admin);
            user.setPassword(pwd);
            user.setType("admin");
            user.setKey(EncryptionUtil.md5(user.getUsername(), md5PublicKey, "UTF-8"));
            UserModel admin = user.where("[username]=#{username}").first();
            if(null == admin) {
                user.insert();
            }

            else if(!pwd.equals(admin.getPassword())) {
                admin.where("[username]=#{username}").update(String.format("[password]='%s'", pwd));
            }

        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + "数据源初始化：[ApplicationContext]" + e.getMessage(), e);
            if (applicationContext instanceof ConfigurableApplicationContext) {
                ((ConfigurableApplicationContext) applicationContext).close();
            }
        }
    }

    public static DataSource getDefaultDataSource(String configFile) throws Exception {
        Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource();
        if(dsList == null || dsList.size() < 1) {
            throw new Exception(String.format("无法读取数据源配置文件(Can not read dataSource file %s)", configFile));
        }

        Optional<DataSource> opt = null;
        if(dsList.size() == 1) {
            opt = dsList.stream().findFirst();
        }

        else {
            opt = dsList.stream().filter(a->a.isDefault()).findFirst();
        }

        if(!opt.isPresent()) {
            throw new Exception(String.format("没有找到默认数据源(Can not find default dataSource in %s)", configFile));
        }

        return opt.get();
    }

    private void createUserGroupRoleTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(userGroupRoleTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", userGroupRoleTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("userGroupRoleId");
        col.setIsPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("userId");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.groupId}", groupIdField));
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.roleId}", roleIdField));
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(userPathField);
        col.setCanNotNull(true);
        col.setMaxLength(2048);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), userGroupRoleTablename, columns);
            log.info(String.format(LOG_PRE_SUFFIX + "用户群组角色表[%s]创建成功！", userGroupRoleTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "用户群组角色表[%s]没有创建！", userGroupRoleTablename));
        }
    }

    private void createPrivilegeTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(privilegeTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", privilegeTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("privilegeId");
        col.setIsPrimaryKey(true);
        col.setCanNotNull(true);
        col.setIsAutoIncrement(true);
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setCanNotNull(false);
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.roleId}", roleIdField));
        col.setCanNotNull(false);
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("aclId");
        col.setCanNotNull(false);
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canAdd");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDelete");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpdate");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canView");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDownload");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canPreview");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canPlayVideo");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpload");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canExport");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canImport");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDecrypt");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canList");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canQuery");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("scope");
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), privilegeTablename, columns);
            log.info(String.format(LOG_PRE_SUFFIX + "权限表[%s]创建成功！", privilegeTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "权限表[%s]没有创建！", privilegeTablename));
        }
    }

    private void createAclTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(aclTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", aclTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName("aclId");
        col.setIsPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setIsAutoIncrement(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setCanNotNull(false);
        col.setMaxLength(32);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("tableName");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);
        col = new ColumnInfo();

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), aclTablename, columns);
            log.info(String.format(LOG_PRE_SUFFIX + "访问控制表[%s]创建成功！", aclTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "访问控制表[%s]没有创建！", aclTablename));
        }
    }

    private void createRoleTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(roleTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", roleTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.roleId}", roleIdField));
        col.setIsPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(Integer.class);
        col.setIsAutoIncrement(true);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("roleName");
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("description");
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), roleTablename, columns);
            log.info(String.format(LOG_PRE_SUFFIX + "角色表[%s]创建成功！", roleTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "角色表[%s]没有创建！", roleTablename));
        }
    }

    private void createGroupTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        if(allTables.stream().filter(a->a.toLowerCase().equals(groupTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", groupTablename));
            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(groupIdField);
        col.setIsPrimaryKey(true);
        col.setCanNotNull(true);
        col.setMaxLength(32);
        col.setIsAutoIncrement(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("groupName");
        col.setCanNotNull(true);
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("type");
        col.setCanNotNull(true);
        col.setMaxLength(64);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("description");
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), groupTablename, columns);
            log.info(String.format(LOG_PRE_SUFFIX + "群组表[%s]创建成功！", groupTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "群组表[%s]没有创建！", groupTablename));
        }
    }

    public void createUserTable(DataSource ds, List<ColumnInfo> columns) throws Exception {

        UserModel user = new UserModel();
        user.setUsername(admin);
        String pwd = getEncryptPassword();

        user.setPassword(pwd);
        user.setCreateOn(new Date());
        user.setStatus(0);
        user.setCreateBy(Integer.valueOf(0));
        user.setType("admin");
        user.setKey(EncryptionUtil.md5(user.getUsername(), md5PublicKey, "UTF-8"));

        if(allTables.stream().filter(a->a.toLowerCase().equals(userTablename.toLowerCase())).findAny().isPresent()) {
            log.info(String.format(LOG_PRE_SUFFIX + "数据源初始化：数据表[%s]已存在！", userTablename));

            if(StringUtils.isNotEmpty(password)) {
                user.where("[username]=#{username}").update("[password]=#{password}"); //确保超级管理员密码
            }

            return;
        }

        ColumnInfo col;
        columns.clear();

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField));
        col.setIsAutoIncrement(true);
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(Integer.class);
        col.setIsPrimaryKey(true);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("username");
        col.setCanNotNull(true);
        col.setMaxLength(16);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("password");
        col.setCanNotNull(true);
        col.setMaxLength(255);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("type");
        col.setCanNotNull(true);
        col.setDefaultValue("user");
        col.setMaxLength(16);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("key");
        col.setCanNotNull(true);
        col.setDefaultValue("");
        col.setMaxLength(64);
        col.setType(String.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyOnField);
        col.setCanNotNull(false);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(modifyByField);
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(createOnField);
        col.setType(Date.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField));
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("status");
        col.setType(Integer.class);
        columns.add(col);

        try {
            Queryable.createTable(ds.getId(), userTablename, columns);

            user.insert();

            log.info(String.format(LOG_PRE_SUFFIX + "用户表[%s]创建成功！", userTablename));
        }
        catch (Exception e) {
            log.info(String.format(LOG_PRE_SUFFIX + "用户表[%s]没有创建！", userTablename));
        }
    }

    public String getEncryptPassword() throws Exception {
        String pwd = password;

        if(StringUtils.isNotEmpty(md5Fields)) {
            List<String> fieldList = StringUtils.splitString2List(md5Fields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.md5(pwd, md5PublicKey, encoding);
            }
        }

        if(StringUtils.isNotEmpty(macFields)) {
            List<String> fieldList = StringUtils.splitString2List(macFields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.mac(pwd.getBytes(encoding), macPublicKey);
            }
        }

        if(StringUtils.isNotEmpty(shaFields)) {
            List<String> fieldList = StringUtils.splitString2List(shaFields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.sha(pwd);
            }
        }

        if(StringUtils.isNotEmpty(base64Fields)) {
            List<String> fieldList = StringUtils.splitString2List(base64Fields, ",");
            if(fieldList.contains("password") || fieldList.contains("user.password")) {
                pwd = EncryptionUtil.base64Encode(pwd, encoding);
            }
        }

        if(StringUtils.isNotEmpty(aesFields)) {
            List<String> fieldList = StringUtils.splitString2List(aesFields, ",");
            if(fieldList.contains("password")) {
                pwd = EncryptionUtil.encryptByAES(pwd, encoding);
            }
        }
        return pwd;
    }

    public static <T> void setSession(HttpServletRequest request, String key, T data) {
        request.getSession().setAttribute( key, JsonUtils.toJson(data));
    }

    public static <T> T getSession(HttpServletRequest request, String key, Class<T> clazz) throws IOException {
        Object data = request.getSession().getAttribute(key);
        if(data == null) {
            return null;
        }

        if(data.getClass().equals(clazz)) {
            return (T) data;
        }

        if(data instanceof String) {
            return JsonUtils.parse(data.toString(), clazz);
        }

        return JsonUtils.convert(data, clazz);
    }

    public static void removeGroupsUserIdValue(HttpServletRequest request, String table, UserModel user, Object idValue) throws SQLException, IOException {

        if(idValue == null) {
            return;
        }

        if("usergrouprole".equalsIgnoreCase(table)) {
            ApplicationContext.getUserIdByCurrentGroups(request, user).remove(idValue);
            ApplicationContext.getUserIdBySubGroups(request, user).remove(idValue);
        }
    }

    public static void setGroupsUserIdValue(HttpServletRequest request, String table, Map<String, Object> data, UserModel user, Object idValue) throws SQLException, IOException {

        if(idValue == null) {
            return;
        }

        if("usergrouprole".equalsIgnoreCase(table) && user != null && data.get("groupId") != null) {
            if(UserContext.getUserGroupRoleModels(request, user.getUserId()).stream()
                    .filter(a->a.getGroupId().equals(data.get("groupId"))).isParallel()) {
                if(!ApplicationContext.getUserIdByCurrentGroups(request, user).contains(idValue)) {
                    ApplicationContext.getUserIdByCurrentGroups(request, user).add((Integer) idValue);
                }
            }
        }
    }

    public static Set<Integer> getUserIdByAllGroups(HttpServletRequest request, UserModel user) throws SQLException, IOException {

        if(user == null || adminName.equals(user.getUsername())) {
            return new HashSet<>();
        }

        Set<Integer> useridSet = ApplicationContext.getSession(request, CURRENT_USER_ID_GROUPS, Set.class);
        if(useridSet == null || useridSet.size() < 1) {
            useridSet =  getUserIdByCurrentGroups(request, user);
        }

        Set<Integer> useridSetBySub = ApplicationContext.getSession(request, CURRENT_USER_ID_SUB_GROUPS, Set.class);
        if(useridSetBySub == null || useridSetBySub.size() < 1) {
            useridSetBySub =  getUserIdBySubGroups(request, user);
        }

        useridSet.addAll(useridSetBySub);

        return useridSet;
    }

    public static Set<Integer> getUserIdBySubGroups(HttpServletRequest request, UserModel user) throws SQLException, IOException {

        if(user == null || adminName.equals(user.getUsername())) {
            return new HashSet<>();
        }

        Set<Integer> useridList = ApplicationContext.getSession(request, CURRENT_USER_ID_SUB_GROUPS, Set.class);
        if(useridList != null && useridList.size() > 0) {
            return useridList;
        }

        UserGroupRoleModel userGroupRoleModel = new UserGroupRoleModel();

        //再查找子用户组
        List<String> userPathConditionList = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream()
                .filter(a->a.getGroupId() != null)
                .map(a->String.format("[userPath] LIKE '%s/%s'", a.getPath(), "%"))
                .collect(Collectors.toList());

        List<UserGroupRoleModel> userIdListBySubGroups = userGroupRoleModel.where(join(" OR ", userPathConditionList)).query();
        useridList = userIdListBySubGroups.stream()
                .map(a-> a.getUserId()).collect(Collectors.toSet());

        ApplicationContext.setSession(request, CURRENT_USER_ID_SUB_GROUPS, useridList);

        return useridList;
    }

    public static Set<Integer> getUserIdByCurrentGroups(HttpServletRequest request, UserModel user) throws SQLException, IOException {

        if(user == null || adminName.equals(user.getUsername())) {
            return new HashSet<>();
        }

        Set<Integer> useridList = ApplicationContext.getSession(request, CURRENT_USER_ID_GROUPS, Set.class);
        if(useridList != null && useridList.size() > 0) {
            return useridList;
        }

        List<String> groupIdList = UserContext.getUserGroupRoleModels(request, user.getUserId()).stream()
                .filter(a->a.getGroupId() != null)
                .map(a->a.getGroupId().toString().replace("-", ""))
                .collect(Collectors.toList());

        UserGroupRoleModel userGroupRoleModel = new UserGroupRoleModel();
        List<UserGroupRoleModel> userIdListByGroups = new ArrayList<>();
        if(groupIdList.size() > 0) {
            userIdListByGroups = userGroupRoleModel.where(String.format("groupId in ('%s')", join("', '", groupIdList))).query();
        }

        useridList = userIdListByGroups.stream()
                .map(a-> a.getUserId()).collect(Collectors.toSet());

        ApplicationContext.setSession(request, CURRENT_USER_ID_GROUPS, useridList);

        return useridList;
    }

    public static String getCurrentDatasourceId() {
        String currentDatasource = "default";
        if (ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }
        return currentDatasource;
    }

    public static List<ColumnInfo> ensureTable(Class<?> type) throws Exception {

        String table = null;
        Tablename annTb = type.getAnnotation(Tablename.class);
        if(annTb!=null && StringUtils.isNotEmpty(annTb.value())) {
            table = annTb.value();
        }

        return ensureTable(type, table);
    }

    public static List<ColumnInfo> ensureTable(Class<?> type, String table) throws Exception {

        String datasource = ApplicationContext.getCurrentDatasourceId();
        entity.query.annotation.DataSource annDs = type.getAnnotation(entity.query.annotation.DataSource.class);
        if(annDs!=null && StringUtils.isNotEmpty(annDs.value())) {
            datasource = annDs.value();
        }

        if(StringUtils.isEmpty(table)) {
            Tablename annTb = type.getAnnotation(Tablename.class);
            if(annTb!=null && StringUtils.isNotEmpty(annTb.value())) {
                table = annTb.value();
            }
        }

        if(StringUtils.isEmpty(table)) {
            for (Annotation item : type.getAnnotations()) {
                if(StringUtils.isNotEmpty(table)) {
                    break;
                }
                List<Annotation> list = Arrays.stream(item.annotationType().getAnnotations()).collect(Collectors.toList());
                for(int i=0; i<list.size(); i++) {
                    if (list.get(i) instanceof Tablename) {
                        String value = ReflectionUtils.invoke(item.getClass(), item, "table").toString();
                        String ds = ReflectionUtils.invoke(item.getClass(), item, "dataSource").toString();
                        if(StringUtils.isNotEmpty(ds)) {
                            datasource = ds;
                        }
                        if(StringUtils.isNotEmpty(value)) {
                            table = value;
                            break;
                        }
                    }
                }
            }
        }

        if(StringUtils.isEmpty(table)) {
            table = type.getName();
        }

        if(Queryable.exist(datasource, table)) {
            return Queryable.getColumns(datasource, table);
        }

        List<ColumnInfo> columns = new ArrayList<>();
        Field[] flds = type.getDeclaredFields();
        for(Field fld : flds) {

            if(Modifier.isStatic(fld.getModifiers())) {
                continue;
            }

            if(fld.getAnnotation(Exclude.class)!=null) {
                continue;
            }

            String name = fld.getName();
            Fieldname fieldname = fld.getAnnotation(Fieldname.class);
            if(fieldname!=null && StringUtils.isNotEmpty(fieldname.value())) {
                name = fieldname.value();
            }
            ColumnInfo column = new ColumnInfo();
            column.setColumnName(name);
            if(fld.getType().isEnum()) {
                column.setType(Integer.class);
            }
            else {
                column.setType(fld.getType());
            }
            PrimaryKey pk = fld.getAnnotation(PrimaryKey.class);
            if(pk != null) {
                column.setIsPrimaryKey(true);
            }

            AutoIncrement au = fld.getAnnotation(AutoIncrement.class);
            if(au != null) {
                column.setIsAutoIncrement(true);
            }

            columns.add(column);
        }

        return ensureTable(datasource, table, columns);
    }

    public static List<ColumnInfo> ensureTable(String datasource, String table, List<ColumnInfo> cloumns) throws Exception {

        if(Queryable.exist(datasource, table)) {
            return Queryable.getColumns(datasource, table);
        }

        Queryable.createTable(datasource, table, cloumns);
        return Queryable.getColumns(datasource, table);
    }
}
