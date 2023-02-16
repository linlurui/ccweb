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


import ccait.ccweb.config.LangConfig;
import ccait.ccweb.model.SheetHeaderModel;
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.TableInfo;
import entity.query.annotation.*;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.query.enums.AlterMode;
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
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
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

    private static final Map<String, Map<String, List<ColumnInfo>>> tableColumnsMap = new HashMap<String, Map<String, List<ColumnInfo>>>();

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
            threadLocal.set(new ConcurrentHashMap<>());
        }

        return threadLocal.get();
    }

    private final static ThreadLocal<Map<String, Object>> threadLocal = new ThreadLocal<>();
    static {
        threadLocal.set(new ConcurrentHashMap<>());
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

    @Value("${ccweb.table.reservedField.owner:owner}")
    private String ownerField;

    @Value("${ccweb.table.reservedField.aclId:aclId}")
    private String aclIdField;

    @Value("${ccweb.table.reservedField.privilegeId:privilegeId}")
    private String privilegeIdField;

    @Value("${ccweb.table.reservedField.userGroupRoleId:userGroupRoleId}")
    private String userGroupRoleIdField;

    @Value(value = "${ccweb.suffix:Entity}")
    private String suffix;

    private static final Map<String, List<String>> allTablesMap = new HashMap<>();

    private static String createOnFieldStatic;
    private static String createByFieldStatic;
    private static String modifyByFieldStatic;
    private static String ownerFieldStatic;
    private static String modifyOnFieldStatic;
    private static String userTableStatic;
    private static String userIdFieldStatic;
    private static String privilegeTableStatic;
    private static String aclTableStatic;
    private static String groupTableStatic;
    private static String userGroupRoleTableStatic;
    private static String roleTableSataic;
    private static String userGroupRoleIdFieldStatic;
    private static String aclIdFieldStatic;
    private static String privilegeIdFieldStatic;
    private static String roleIdFieldStatic;
    private static String groupIdFieldStatic;
    public static String adminFieldStatic;
    public static String userPathFieldStatic;

    @PostConstruct
    private void init() {
        adminFieldStatic = admin;
        createByFieldStatic = createByField;
        createOnFieldStatic = createOnField;
        modifyByFieldStatic = modifyByField;
        ownerFieldStatic = ownerField;
        modifyOnFieldStatic = modifyOnField;
        userTableStatic = userTablename;
        privilegeTableStatic = privilegeTablename;
        aclTableStatic = aclTablename;
        groupTableStatic = groupTablename;
        userGroupRoleTableStatic = userGroupRoleTablename;
        roleTableSataic = roleTablename;
        userIdFieldStatic  = userIdField;
        userGroupRoleIdFieldStatic = userGroupRoleIdField;
        aclIdFieldStatic = aclIdField;
        privilegeIdFieldStatic = privilegeIdField;
        roleIdFieldStatic = roleIdField;
        groupIdFieldStatic = groupIdField;
        userPathFieldStatic = userPathField;
        try {
            org.springframework.context.ApplicationContext app = ApplicationContext.getInstance();
            initTableColumnsCache();
            if (app == null) {
                throw new RuntimeException("程序启动顺序不正确, ApplicationContext必须优先启动！");
            }
            String[] names = app.getBeanNamesForAnnotation(Tablename.class);
            if (names != null) {
                for (String name : names) {

                    String key = name;
                    if (StringUtils.isNotEmpty(suffix) && name.length() > suffix.length() &&
                            name.substring(name.length() - suffix.length()).equals(suffix)) {
                        key = name.substring(0, name.length() - suffix.length());
                    }
                }
            }

            log.info(LOG_PRE_SUFFIX + "实体类上下文 ： [CCEntityContext]", "初始化完成");
        } catch (Exception e) {
            log.error(LOG_PRE_SUFFIX + "实体类上下文 ： [CCEntityContext]", "初始化失败！！！", e);
        }
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
        ownerField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField);
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
                List<TableInfo> tableInfos = Queryable.getTables(ds.getId());
                if(tableInfos != null) {
                    allTables = tableInfos.stream().map(a-> a.getTableName()).collect(Collectors.toList());
                }

                if(!allTablesMap.containsKey(ds.getId())) {
                    allTablesMap.put(ds.getId(), allTables);
                }

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
            user.setOwner(Integer.valueOf(0));
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

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
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
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDelete");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpdate");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canView");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDownload");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canPreview");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canPlayVideo");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canUpload");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canExport");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canImport");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canDecrypt");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canList");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("canQuery");
        col.setDefaultValue("0");
        col.setCanNotNull(true);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName("scope");
        col.setDefaultValue("0");
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

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
        col.setCanNotNull(false);
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

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
        col.setCanNotNull(false);
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
        col.setColumnName("type");
        col.setMaxLength(32);
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

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
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
        col.setColumnName("parentId");
        col.setCanNotNull(false);
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
        col.setCanNotNull(false);
        col.setType(Integer.class);
        columns.add(col);

        col = new ColumnInfo();
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
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
        col.setColumnName(ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField));
        col.setCanNotNull(false);
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

        if(user == null || adminFieldStatic.equals(user.getUsername())) {
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

        if(user == null || adminFieldStatic.equals(user.getUsername())) {
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

        if(user == null || adminFieldStatic.equals(user.getUsername())) {
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

    public static String getCurrentTable() {
        Map map = ApplicationContext.getThreadLocalMap();
        if(!map.containsKey(CURRENT_TABLE)) {
            return "";
        }

        return map.get(CURRENT_TABLE).toString();
    }

    public static String getCurrentDatasourceId() {
        String currentDatasource = "default";
        if (ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE) != null) {
            currentDatasource = ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE).toString();
        }
        return currentDatasource;
    }


    public static boolean existTable(String table) throws Exception {
        return existTable(getCurrentDatasourceId(), table);
    }

    public static boolean existTable(String datasource, String table) throws Exception {
        if(!allTablesMap.containsKey(datasource)) {
            List<String> allTables = Queryable.getTables(datasource).stream().map(a-> a.getTableName()).collect(Collectors.toList());
            allTablesMap.put(datasource, allTables);
        }

        if(!allTablesMap.containsKey(datasource)) {
            log.error(String.format(LangConfig.getInstance().get("can_not_find_datasource_mapping"), datasource));
        }
        if(allTablesMap.get(datasource)
                .stream().filter(a->a.toLowerCase().equals(table.toLowerCase()))
                .findAny().isPresent()) {
            return true;
        }

        boolean result = false;
        try {
            result = Queryable.exist(datasource, table);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    public static void ensureTable(Map<String, Object> data) throws Exception {
        ensureTable(data, null, getCurrentDatasourceId(), getCurrentTable());
    }

    public static void ensureTable(Map<String, Object> data, String table) throws Exception {
        ensureTable(data, null, getCurrentDatasourceId(), table);
    }

    public static void ensureTable(Map<String, Object> data, final List<String> uniqueList) throws Exception {
        ensureTable(data, uniqueList, getCurrentTable());
    }

    public static void ensureTable(Map<String, Object> data, final List<String> uniqueList, String table) throws Exception {
        ensureTable(data, uniqueList, getCurrentDatasourceId(), table);
    }

    public static List<ColumnInfo> ensureTable(Map<String, Object> data, String datasource, String table) throws Exception {
        return ensureTable(data, null, datasource, table);
    }

    public static List<ColumnInfo> ensureTable(Map<String, Object> data, final List<String> uniqueList, String datasource, String table) throws Exception {

        if(existTable(datasource, table)) {
            return getColumns(datasource, table);
        }

        if(uniqueList != null) {
            for(int i=0; i<uniqueList.size(); i++) {
                uniqueList.set(i, uniqueList.get(i).toLowerCase());
            }
        }

        List<ColumnInfo> columns = data.entrySet().stream().map(a-> new ColumnInfo(){{
            setColumnName(a.getKey());
            if(createOnFieldStatic.equals(a.getKey()) || modifyOnFieldStatic.equals(a.getKey())) {
                setType(Date.class);
            }

            else if(createByFieldStatic.equals(a.getKey()) || modifyByFieldStatic.equals(a.getKey()) || ownerFieldStatic.equals(a.getKey())) {
                setType(Integer.class);
            }

            else {
                setType(String.class);
            }

            if(uniqueList!=null && uniqueList.contains(a.getKey().toLowerCase())) {
                setUnique(true);
            }
        }}).collect(Collectors.toList());
        String pk = "id";
        if(userTableStatic.equals(table)) {
            pk = userIdFieldStatic;
        }

        else if(userGroupRoleTableStatic.equals(table)) {
            pk = userGroupRoleIdFieldStatic;
        }

        else if(aclTableStatic.equals(table)) {
            pk = aclIdFieldStatic;
        }

        else if(privilegeTableStatic.equals(table)) {
            pk = privilegeIdFieldStatic;
        }

        else if(roleTableSataic.equals(table)) {
            pk = roleIdFieldStatic;
        }

        else if(groupTableStatic.equals(table)) {
            pk = groupIdFieldStatic;
        }

        if(!data.containsKey(pk)) {
            ColumnInfo idField = new ColumnInfo();
            idField.setColumnName(pk);
            idField.setType(Integer.class);
            idField.setPk(true);
            idField.setIsAutoIncrement(true);
            columns.add(0, idField);
        }

        return ensureTable(datasource, table, columns);
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

        String datasource = getCurrentDatasourceId();
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

        if(existTable(datasource, table)) {
            return getColumns(datasource, table);
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

        if(existTable(table)) {
            return getColumns(datasource, table);
        }

        Queryable.createTable(datasource, table, cloumns);
        allTablesMap.get(datasource).add(table);
        return getColumns(datasource, table);
    }

    public static void ensureColumns(Map<String, Object> data) throws Exception {
        if(existTable(getCurrentTable())) {
            ensureColumns(getCurrentDatasourceId(), getCurrentTable(), data, null);
        }
    }

    public static void ensureColumns(Map<String, Object> data, final List<String> uniqueList) throws Exception {
        if(existTable(getCurrentTable())) {
            ensureColumns(getCurrentDatasourceId(), getCurrentTable(), data, uniqueList);
        }
    }

    public static List<ColumnInfo> ensureColumns(String datasource, String table, Map<String, Object> data) throws Exception {
        return ensureColumns(datasource, table, data, null);
    }

    public static List<ColumnInfo> ensureColumns(String datasource, String table, Map<String, Object> data, final List<String> uniqueList) throws Exception {

        if(uniqueList != null) {
            for(int i=0; i<uniqueList.size(); i++) {
                uniqueList.set(i, uniqueList.get(i).toLowerCase());
            }
        }
        List<ColumnInfo> dataColumns = convertToColumnInfos(data);

        return ensureColumns(datasource, table, uniqueList, dataColumns);
    }

    public static List<ColumnInfo> ensureColumns(String datasource, String table, List<String> uniqueList, List<ColumnInfo> dataColumns) throws Exception {
        List<ColumnInfo> columnInfos = getColumns(datasource, table);
        List<String> fieldList = columnInfos.stream().map(a->a.getColumnName()).collect(Collectors.toList());
        List<ColumnInfo> newFieldList = dataColumns.stream().filter(a -> !fieldList.contains(a.getColumnName()))
                .collect(Collectors.toList());

        for(ColumnInfo field : newFieldList) {
            ColumnInfo column = new ColumnInfo() {{
                setColumnName(field.getColumnName());
                setAlterMode(AlterMode.ADD);
                if(createOnFieldStatic.equals(field.getColumnName()) || modifyOnFieldStatic.equals(field.getColumnName())) {
                    setType(Date.class);
                }

                else if(createByFieldStatic.equals(field.getColumnName()) || modifyByFieldStatic.equals(field.getColumnName()) || ownerFieldStatic.equals(field.getColumnName())) {
                    setType(Integer.class);
                }

                else if(field.getDataType() != null){
                    setDataType(field.getDataType());
                }

                else if(field.getType() != null){
                    setType(field.getType());
                }

                else {
                    setType(String.class);
                }

                if(uniqueList!=null && uniqueList.contains(field.getColumnName().toLowerCase())) {
                    setUnique(true);
                }
            }};

            columnInfos.add(column);
        }

        Queryable.alterTable(datasource, table, columnInfos);

        return columnInfos;
    }

    public static <T> List<ColumnInfo> convertToColumnInfos(T data) {
        List<ColumnInfo> result = new ArrayList<>();
        if(data instanceof Map) {
            Map<String, Object> map = (Map) data;
            result = map.entrySet().stream()
                    .map(a -> new ColumnInfo() {{
                        setColumnName(a.getKey());
                    }}).collect(Collectors.toList());
        }

        else if(data instanceof List) {
            List list = (List) data;
            if(list.size() < 1) {
                return result;
            }

            if(list.get(0) instanceof SheetHeaderModel) {
                final List<SheetHeaderModel> dataList = list;
                result = dataList.stream().map(a -> new ColumnInfo() {{
                    setColumnName(a.getField());
                    setDataType(a.getType());
                }}).collect(Collectors.toList());
            }

            else if(list.get(0) instanceof String) {
                final List<String> dataList = list;
                result = dataList.stream().map(a -> new ColumnInfo() {{
                    setColumnName(a);
                }}).collect(Collectors.toList());
            }
        }

        return result;
    }


    public static List<ColumnInfo> getColumns(String table) throws SQLException {
        return getColumns(getCurrentDatasourceId(), table);
    }

    public static List<ColumnInfo> getColumns(String datasource, String table) {

        if(!tableColumnsMap.containsKey(datasource) ||
                !tableColumnsMap.get(datasource).containsKey(table)) {

            List<ColumnInfo> columns = Queryable.getColumns(datasource, table);
            String primaryKey = Queryable.getPrimaryKey(datasource, table);
            for(ColumnInfo column : columns) {
                if(column.getColumnName().equals(primaryKey)) {
                    column.setIsPrimaryKey(Boolean.TRUE);
                }
            }
            tableColumnsMap.put(datasource, new HashMap<String, List<ColumnInfo>>());
            tableColumnsMap.get(datasource).put(table, columns);
        }

        return tableColumnsMap.get(datasource).get(table);
    }

    public static ColumnInfo getPrimaryKey(String datasourceId, String table) {
        if(!tableColumnsMap.containsKey(datasourceId)) {
            return null;
        }

        if(!tableColumnsMap.get(datasourceId).containsKey(table)) {
            return null;
        }

        Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                .filter(a->a.isPrimaryKey()==true).findAny();

        if(optional.isPresent()) {
            ColumnInfo info = optional.get();
            if(info == null) {
                return null;
            }

            return info;
        }

        return null;
    }

    public static boolean hasColumn(String datasourceId, String table, String columnName) {
        if(!tableColumnsMap.containsKey(datasourceId)) {
            return false;
        }

        if(!tableColumnsMap.get(datasourceId).containsKey(table)) {
            return false;
        }

        Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                .filter(a->a.getColumnName().equals(columnName)).findAny();

        if(optional.isPresent()) {
            ColumnInfo info = optional.get();
            if(info == null) {
                return false;
            }

            return true;
        }

        return false;
    }

    private static void addColumnById(String datasourceId, String table, String id, List<ColumnInfo> columns) {

        String fieldname = "id";
        if(tableColumnsMap.containsKey(datasourceId) && tableColumnsMap.get(datasourceId).containsKey(table)) {
            Optional<ColumnInfo> optional = tableColumnsMap.get(datasourceId).get(table).stream()
                    .filter(a -> a.isPrimaryKey() == true).findAny();

            if(optional.isPresent()) {
                ColumnInfo column = optional.get();
                fieldname = column.getColumnName();
            }
        }

        if(Pattern.matches("^[0-9]{1,32}$", id)) {
            columns.add(new ColumnInfo(fieldname, "integer", true));
        }

        else if(Pattern.matches("^\\d+$", id)) {
            columns.add(new ColumnInfo(fieldname, "long", true));
        }

        else {
            columns.add(new ColumnInfo(fieldname, "text", true));
        }
    }

    public void initTableColumnsCache() {
        try {
            String configPath = System.getProperty("user.dir") + "/src/main/resources/"
                    + ApplicationConfig.getInstance().get("entity.datasource.configFile", "db-config.xml");

            Collection<DataSource> dsList = DataSourceFactory.getInstance().getAllDataSource(configPath);
            if(dsList == null || dsList.size() < 1) {
                //tomcat路径
                String property = System.getProperty("catalina.home");
                String path =property + File.separator + "conf" + File.separator + "db-config.xml";
                dsList = DataSourceFactory.getInstance().getAllDataSource(path);
            }

            for(DataSource ds : dsList) {
                try {
                    if(ds.getConnection() == null) {
                        continue;
                    }

                    List<TableInfo> tablenames = Queryable.getTables(ds.getId());
                    for (TableInfo tb : tablenames) {

                        if (StringUtils.isEmpty(tb.getTableName())) {
                            continue;
                        }

                        List<ColumnInfo> columns = Queryable.getColumns(ds.getId(), tb.getTableName());
                        String primaryKey = Queryable.getPrimaryKey(ds.getId(), tb.getTableName());
                        for(ColumnInfo column : columns) {
                            if(column.getColumnName().equals(primaryKey)) {
                                column.setIsPrimaryKey(Boolean.TRUE);
                            }
                        }
                        if (!tableColumnsMap.containsKey(ds.getId())) {
                            tableColumnsMap.put(ds.getId(), new HashMap<String, List<ColumnInfo>>());
                        }
                        tableColumnsMap.get(ds.getId()).put(tb.getTableName(), columns);
                    }
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                log.info("table==>columns==>");
                log.info(JsonUtils.toJson(tableColumnsMap));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
