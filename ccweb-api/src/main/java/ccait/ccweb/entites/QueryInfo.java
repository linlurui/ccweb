/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.entites;


import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.enums.Algorithm;
import ccait.ccweb.enums.EncryptMode;
import ccait.ccweb.enums.PrivilegeScope;
import ccait.ccweb.model.GroupModel;
import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import com.alibaba.fastjson.JSONArray;
import entity.query.*;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.query.enums.Function;
import entity.tool.util.DBUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.dynamic.DynamicClassBuilder.ensureColumnName;
import static ccait.ccweb.dynamic.DynamicClassBuilder.smallHump;
import static ccait.ccweb.utils.StaticVars.*;
import static entity.tool.util.StringUtils.cast;
import static entity.tool.util.StringUtils.join;

@Component
public class QueryInfo implements Serializable {

    private static final Logger log = LogManager.getLogger(QueryInfo.class);
    private static QueryInfo context;
    @PostConstruct
    public void init() {
        context = this;
        context.request = this.request;
        // 初使化时将已静态化的request实例化

        aesPublicKey = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.publicKey}", aesPublicKey);
        encoding = ApplicationConfig.getInstance().get("${entity.encoding}", encoding);
        groupIdField = ApplicationConfig.getInstance().get("${entity.table.reservedField.groupId}", groupIdField);
        createByField = ApplicationConfig.getInstance().get("${entity.table.reservedField.createBy}", createByField);
    }

    @Autowired
    protected HttpServletRequest request;

    @Value("${entity.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${entity.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Value("${entity.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${entity.encoding:UTF-8}")
    private String encoding;

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<ConditionInfo> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<ConditionInfo> conditionList) {
        this.conditionList = conditionList;
    }

    public List<SortInfo> getSortList() {
        return sortList;
    }

    public void setSortList(List<SortInfo> sortList) {
        this.sortList = sortList;
    }

    public List<String> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
    }

    public List<FieldInfo> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<FieldInfo> keywords) {
        this.keywords = keywords;
    }

    private PageInfo pageInfo;
    private List<ConditionInfo> conditionList;
    private List<SortInfo> sortList;
    private List<String> groupList;
    private List<FieldInfo> keywords;
    private List<TableInfo> joinTables;
    private List<SelectInfo> selectList;
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


    /***
     * 获取查询条件
     * @param entity
     * @return
     */
    public Where getWhereQuerable(String tablename, Object entity, PrivilegeScope privilegeScope) throws Exception {
        return getWhereQuerable(tablename, null, entity, privilegeScope);
    }

    /***
     * 获取查询条件
     * @param entity
     * @return
     */
    public Where getWhereQuerable(String tablename, String alias, Object entity, PrivilegeScope privilegeScope) throws Exception {

        List<Field> fields = EntityContext.getFields(entity);

        Where where = (Where) ReflectionUtils.invoke(entity.getClass(), entity, "where", "1=1");

        where = ensureWhereQuerable(where, fields, privilegeScope, entity, tablename, alias);

        return where;
    }


    /***
     * 获取查询条件
     * @param tableList
     * @return
     */
    public Where getWhereQuerableByJoin(List<TableInfo> tableList, On on) throws Exception {

        if(tableList == null) {
            throw new IllegalAccessException("condition can not be empty!!!");
        }

        Where result = on.where("1=1");
        for(TableInfo table : tableList) {
            table.setFields(EntityContext.getFields(table.getEntity()));

            Where where = (Where) ReflectionUtils.invoke(table.getEntity().getClass(), table.getEntity(), "where", "1=1");

            where = ensureWhereQuerable(where, table.getFields(), table.getPrivilegeScope(), table.getEntity(),
                    table.getTablename(), table.getAlias());

            String strWhere = where.getExpression().replaceAll("[\\s]*1=1[\\s,]*(OR|AND|or|and)", "");

            if(StringUtils.isEmpty(strWhere)) {
                continue;
            }

            if(result.toString().indexOf(strWhere) > -1) {
                continue;
            }

            result.and(strWhere);
        }

        return result;
    }

    public QueryableAction getSelectQuerable(GroupBy groupBy) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return groupBy;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {
            if(StringUtils.isEmpty(info.getField())) {
                continue;
            }

            String field = info.getField();
            if(!Pattern.matches("^\\w[A-Za-z0-9_]*$", field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            field = ensureColumn(field);

            if(info.getFunction() == null || info.getFunction() == Function.NONE) {
                if(info.getAlias() == null) {
                    list.add(field);
                }
                else {
                    list.add(String.format("%s AS %s", ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }

            else {
                if(info.getAlias() == null) {
                    list.add(String.format("%s(%s)", info.getFunction().getValue(), ensureColumnName(field)));
                }
                else {
                    list.add(String.format("%s(%s) AS %s", info.getFunction().getValue(), ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }
        }

        if(list.size() < 1) {
            return groupBy;
        }

        groupBy.select().clean();

        return groupBy.select(StringUtils.join(", ", list.toArray()));
    }

    public QueryableAction getSelectQuerable(OrderBy orderBy) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return orderBy;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {
            if(StringUtils.isEmpty(info.getField())) {
                continue;
            }

            String field = info.getField();
            if(!Pattern.matches("^\\w[A-Za-z0-9_]*$", field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            field = ensureColumn(field);

            if(info.getFunction() == null || info.getFunction() == Function.NONE) {
                if(info.getAlias() == null) {
                    list.add(field);
                }
                else {
                    list.add(String.format("%s AS %s", ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }

            else {
                if(info.getAlias() == null) {
                    list.add(String.format("%s(%s)", info.getFunction().getValue(), ensureColumnName(field)));
                }
                else {
                    list.add(String.format("%s(%s) AS %s", info.getFunction().getValue(), ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }
        }

        if(list.size() < 1) {
            return orderBy;
        }

        orderBy.select().clean();

        return orderBy.select(StringUtils.join(", ", list.toArray()));
    }

    public QueryableAction getSelectQuerable(Where where, boolean isMutilTable) {

        if(this.getSelectList() == null || this.getSelectList().size() < 1) {
            return where;
        }

        List<String> list = new ArrayList<String>();
        for(SelectInfo info : this.getSelectList()) {

            if(!isMutilTable) {
                if (StringUtils.isEmpty(info.getField())) {
                    continue;
                }
            }

            String field = info.getField();
            if(isMutilTable) {
                field = info.getField();
            }

            if(StringUtils.isEmpty(field)) {
                continue;
            }

            String regSqlInject = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|"
                    + "(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

            if(Pattern.matches(regSqlInject, field)) {
                continue;
            }

            if(!Pattern.matches("^\\w[A-Za-z0-9_]*(\\.\\w[A-Za-z0-9_]*)*$", field)) {
                continue;
            }

            field = ensureColumn(field);

            if(info.getFunction() == null || info.getFunction() == Function.NONE) {
                if(info.getAlias() == null) {
                    list.add(field);
                }
                else {
                    list.add(String.format("%s AS %s", ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }

            else {
                if(info.getAlias() == null) {
                    list.add(String.format("%s(%s)", info.getFunction().getValue(), ensureColumnName(field)));
                }
                else {
                    list.add(String.format("%s(%s) AS %s", info.getFunction().getValue(), ensureColumnName(field), DBUtils.getSqlInjText(ensureColumnName(info.getAlias()))));
                }
            }
        }

        if(list.size() < 1) {
            return where;
        }

        where.select().clean();

        return where.select(StringUtils.join(", ", list.toArray()));
    }

    private String ensureValue(ConditionInfo info, String fieldName, String value) {

        String fieldText = "#{" + fieldName + "}";
        if(info.getValue().getClass().equals(JSONArray.class)) {
            value = value.substring(1).substring(0, value.length() - 2);
            List<String> list = StringUtils.splitString2List(value, ",");
            for(int i=0; i<list.size(); i++) {
                list.set(i, DBUtils.getStringValue(list.get(i).replaceAll("['\"]", "").trim()));
            }

            if(list.size() == 1) {
                value = StringUtils.join(",", list);
            }
            else if(list.size() > 1) {
                if(Algorithm.IN.equals(info.getAlgorithm()) || Algorithm.NOTIN.equals(info.getAlgorithm())) {
                    value = StringUtils.join(",", list);
                }
            }
        }

        else {
            value = DBUtils.getStringValue(value);
        }

        Pattern pattern = Pattern.compile("'([^']*)'");

        Matcher m2 = pattern.matcher(value);
        if (m2.find()) {
            value = String.format("%s",
                    value.replace(String.format("#{%s}", fieldName), m2.group(1)));
        }

        return value;
    }

    private Where ensureWhereQuerable(Where where, List<Field> fields, PrivilegeScope privilegeScope, Object entity,
                                      String tablename, String alias) throws Exception {
        if(this.getConditionList() != null) {
            for(ConditionInfo info : this.getConditionList()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(smallHump(ensureColumnName(info.getName())))).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                Field fld = opt.get();

                if(info.getValue() == null) {
                    if(Algorithm.EQ.equals(info.getAlgorithm())) {
                        where = where.and(DBUtils.getSqlInjText(ensureColumnName(info.getName())) + " IS NULL ");
                    }

                    else if(Algorithm.NOT.equals(info.getAlgorithm())) {
                        where = where.and(DBUtils.getSqlInjText(ensureColumnName(info.getName())) + " IS NOT NULL ");
                    }
                    continue;
                }

                if(info.getValue().toString().trim().equals("")) {
                    if(Algorithm.EQ.equals(info.getAlgorithm())) {
                        where = where.and(DBUtils.getSqlInjText(ensureColumnName(info.getName())) + "=''");
                    }

                    else if(Algorithm.NOT.equals(info.getAlgorithm())) {
                        where = where.and(DBUtils.getSqlInjText(ensureColumnName(info.getName())) + "!=''");
                    }
                    continue;
                }

                String value = info.getValue().toString();

                where = setWhereStament(where, info, ensureValue(info, fld.getName(), value));
            }
        }

        if(this.getKeywords() != null && this.getKeywords().size() > 0) {

            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;
            for(FieldInfo info : this.getKeywords()) {
                Optional<Field> opt = fields.stream().filter(a->a.getName().equals(smallHump(ensureColumnName(info.getName())))).findFirst();
                if(!opt.isPresent()) {
                    continue;
                }

                if(info.getValue() == null || info.getValue().toString().trim().equals("")) {
                    continue;
                }

                if(!isFirst) {
                    sb.append(" OR ");
                }

                if(info.getValue().getClass().equals(String.class)) {

                    sb.append(String.format("%s LIKE '%s'", ensureColumn(info.getName()), "%"+ DBUtils.getSqlInjText(info.getValue()) +"%"));
                }

                else {
                    sb.append(String.format("%s='%s'", ensureColumn(info.getName()), info.getValue()));
                }
                isFirst = false;
            }

            if(sb.length() > 0) {
                where.and(String.format("(%s)", sb.toString()));
            }
        }

        if(privilegeScope == null) {
            return where;
        }

        String datasourceId = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);

        //控制查询权限
        DataSource dataSource = DataSourceFactory.getInstance().getDataSource(datasourceId);

        UserModel user = (UserModel)context.request.getSession().getAttribute(context.request.getSession().getId() + LOGIN_KEY);

        String createByField = String.format("[%s]", context.createByField);

        if(StringUtils.isNotEmpty(alias)) {
            createByField = String.format("[%s].[%s]", alias, context.createByField);
        }

        List<Integer> useridList = (List<Integer>) ApplicationContext.getUserIdByAllGroups(request, user);
        switch(privilegeScope) {
            case DENIED:
                if(user == null) {
                    throw new HttpException(LangConfig.getInstance().get("session_maybe_to_timeout"));
                }
                throw new Exception(LangConfig.getInstance().get("data_access_denied"));
            case SELF:
                if(!EntityContext.hasColumn(dataSource.getId(), tablename, context.createByField)) {
                    where = where.and(String.format("%s='%s'", createByField, user.getUserId()));
                    break;
                }

                break;
            case CHILD:
                if(!EntityContext.hasColumn(dataSource.getId(), tablename, context.createByField)) {
                    useridList = (List<Integer>)ApplicationContext.getUserIdBySubGroups(request, user);
                    where = getWhereByPrivilegeScope(where, user, createByField, useridList);
                }
                break;
            case PARENT_AND_CHILD:
                if(!EntityContext.hasColumn(dataSource.getId(), tablename, context.createByField)) {
                    where = getWhereByPrivilegeScope(where, user, createByField, useridList);
                }
                break;
            case GROUP:
                if(!EntityContext.hasColumn(dataSource.getId(), tablename, context.createByField)) {
                    useridList = (List<Integer>)ApplicationContext.getUserIdByCurrentGroups(request, user);
                    where = getWhereByPrivilegeScope(where, user, createByField, useridList);
                }
                break;
            case NO_GROUP:
                //查询没有分组数据
                if(EntityContext.hasColumn(dataSource.getId(), tablename, context.createByField)) { //被访问的表有创建人ID时才需要检查分组权限
                    where = where.and(String.format("[%s] is null", createByField));
                }
                break;
        }

        return where;
    }

    private Where getWhereByPrivilegeScope(Where where, UserModel user, String createByField, List<Integer> useridList) {
        where = where.and(String.format("%s IN (%s)", createByField, join(", ", useridList)));
        where = where.or(String.format("%s=-1", createByField));
        where = where.or(String.format("%s='%s'", createByField, user.getUserId()));
        return where;
    }

    private Where setWhereStament(Where where, ConditionInfo info, String value) throws Exception {
        try {
            if(info.getAlgorithm() == null) {
                return where;
            }
            switch (info.getAlgorithm()) {
                case LIKE:
                    where.and(ensureColumn(info.getName()) + " LIKE '%" + value.replace("'", "") + "%'");
                    break;
                case START:
                    where.and(ensureColumn(info.getName()) + " LIKE '%" + value.replace("'", "") + "'");
                    break;
                case END:
                    where.and(ensureColumn(info.getName()) + " LIKE '" + value.replace("'", "") + "'");
                    break;
                case IN:
                    where.and(ensureColumn(info.getName()) + " IN (" + value + ")");
                    break;
                case NOTIN:
                    where.and(ensureColumn(info.getName()) + " NOT IN (" + value + ")");
                    break;
                default:
                    where.and(String.format("%s%s%s", ensureColumn(info.getName()), info.getAlgorithm().getValue(), value));
            }
        }
        catch (Exception e) {
            if(info.getAlgorithm() == null) {
                throw new Exception(LangConfig.getInstance().get("algorithm_can_not_be_empty"));
            }

            throw e;
        }

        return where;
    }

    private String ensureColumn(String name) {
        return DBUtils.getSqlInjText(name).trim().replaceAll("\\s", "")
                .replaceAll("([_\\w\\d]+)(\\.?)", "[$1]$2");
    }

    public Where getWhereQueryableById(Object entity, String table, String id) throws Exception {
        PrimaryKeyInfo pk = EntityContext.getPrimaryKeyInfo(entity);
        if(pk == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_primary_key"));
        }

        id = decryptById(id, table, pk.getField().getName());
        ReflectionUtils.invoke(entity.getClass(), entity, pk.getSetter(), cast(pk.getField().getType(), id));

        Where where = (Where) ReflectionUtils.invoke(entity.getClass(), entity, "where",
                String.format("%s=#{%s}", ensureColumn(pk.getColumnName()), pk.getField().getName()));

        return where;
    }

    protected String decryptById(String id, String table, String fieldName) {

        if(StringUtils.isEmpty(id)) {
            return id;
        }

        String base64Fields = ApplicationConfig.getInstance().get("${entity.security.encrypt.BASE64.fields}", "");
        List<String> base64FieldList = StringUtils.splitString2List(base64Fields, ",");
        String aesFields = ApplicationConfig.getInstance().get("${entity.security.encrypt.AES.fields}", "");
        List<String> aesFieldList = StringUtils.splitString2List(aesFields, ",");

        if(base64FieldList != null && base64FieldList.size() > 0) {
            if(base64FieldList.stream()
                    .allMatch(a -> a.equalsIgnoreCase(fieldName) || a.equalsIgnoreCase(String.join(".", table, fieldName)))) {
                return decrypt(id, EncryptMode.BASE64, encoding);
            }
        }

        aesFieldList = StringUtils.splitString2List(aesFields, ",");
        if(aesFieldList != null && aesFieldList.size() > 0) {
            if(aesFieldList.stream()
                    .allMatch(a -> a.equalsIgnoreCase(fieldName) || a.equalsIgnoreCase(String.join(".", table, fieldName)))) {
                return decrypt(id, EncryptMode.AES, aesPublicKey);
            }
        }

        return id;
    }

    public static String decrypt(String value, EncryptMode encryptMode, String... encryptArgs) {
        try {
            switch (encryptMode) {
                case BASE64:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.base64Decode(value, encryptArgs[0]);
                    break;
                case AES:
                    if(encryptArgs == null || encryptArgs.length != 1) {
                        throw new NoSuchAlgorithmException("encryptArgs has be wrong!!!");
                    }
                    value = EncryptionUtil.decryptByAES(value, encryptArgs[0]);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e, e);
        } catch (NoSuchAlgorithmException e) {
            log.error(e, e);
        } catch (Exception e) {
            log.error(e, e);
        }

        return value;
    }

    public GroupBy getGroupByQuerable(Where where) {
        if(this.getGroupList() == null || this.getGroupList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(String group : this.getGroupList()) {

            if(StringUtils.isEmpty(group.trim())) {
                continue;
            }
            list.add(ensureColumn(group));
        }

        if(list.size() < 1) {
            return null;
        }

        return where.groupby(list.toArray(new String[]{}));
    }

    public OrderBy getOrderByQuerable(Where where) {

        if(this.getSortList() == null || this.getSortList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(SortInfo sort : this.getSortList()) {

            if(StringUtils.isEmpty(sort.getName().trim())) {
                continue;
            }
            list.add(String.format("%s %s", ensureColumn(sort.getName()), sort.isDesc() ? "DESC" : "ASC"));
        }

        if(list.size() < 1) {
            return null;
        }

        return where.orderby(list.toArray(new String[]{}));
    }

    public OrderBy getOrderByQuerable(GroupBy groupBy) {

        if(groupBy == null) {
            return null;
        }

        if(this.getSortList() == null || this.getSortList().size() < 1) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for(SortInfo sort : this.getSortList()) {

            if(StringUtils.isEmpty(sort.getName().trim())) {
                continue;
            }
            list.add(String.format("%s %s", ensureColumn(sort.getName()), sort.isDesc() ? "DESC" : "ASC"));
        }

        if(list.size() < 1) {
            return null;
        }

        return groupBy.orderby(list.toArray(new String[]{}));
    }

    public int getSkip() {

        int result = 0;
        if(this.getPageInfo() == null) {
            return result;
        }

        if(this.getPageInfo().getPageIndex() > 0 && getPageInfo().getPageSize() > 0) {
            return this.getPageInfo().getPageIndex() * this.getPageInfo().getPageSize() - this.getPageInfo().getPageSize();
        }

        return result;
    }

    public Connection getConnection(Object entity) {
        return (Connection) ReflectionUtils.invoke(entity.getClass(), entity, "connection");
    }

    public List<TableInfo> getJoinTables() {
        return joinTables;
    }

    public void setJoinTables(List<TableInfo> joinTables) {
        this.joinTables = joinTables;
    }

    public List<SelectInfo> getSelectList() {
        return selectList;
    }

    public void setSelectList(List<SelectInfo> selectList) {
        this.selectList = selectList;
    }
}
