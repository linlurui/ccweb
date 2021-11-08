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

import ccait.ccweb.annotation.*;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.ConditionInfo;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.enums.DefaultValueMode;
import ccait.ccweb.enums.EncryptMode;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.ClassUtils;
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.sql.Blob;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ccait.ccweb.controllers.BaseController.encrypt;
import static ccait.ccweb.entites.QueryInfo.decrypt;
import static ccait.ccweb.utils.StaticVars.CURRENT_DATASOURCE;
import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;


@Component
@Scope("prototype")
@Trigger
@Order(Ordered.HIGHEST_PRECEDENCE+555)
public final class DefaultTrigger {

    private static final Logger log = LoggerFactory.getLogger( DefaultTrigger.class );

    @Value("${ccweb.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${ccweb.table.reservedField.createOn:createOn}")
    private String createOnField;

    @Value("${ccweb.table.reservedField.modifyOn:modifyOn}")
    private String modifyOnField;

    @Value("${ccweb.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${ccweb.table.reservedField.modifyBy:modifyBy}")
    private String modifyByField;

    @Value("${ccweb.security.encrypt.AES.publicKey:ccait}")
    private String aesPublicKey;

    @Value("${ccweb.encoding:UTF-8}")
    private String encoding;

    @Value("${ccweb.security.admin.username:admin}")
    protected String admin;

    @Autowired
    private QueryInfo queryInfo;

    private String datasourceId;

    @PostConstruct
    private void construct() {
        datasourceId = (String) ApplicationContext.getThreadLocalMap().get(CURRENT_DATASOURCE);
        aesPublicKey = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.publicKey}", aesPublicKey);
        encoding = ApplicationConfig.getInstance().get("${ccweb.encoding}", encoding);
        createOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createOn}", createOnField);
        modifyOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyOn}", modifyOnField);
        modifyByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyBy}", modifyByField);
        userPathField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userPath}", userPathField);
        createByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField);
    }

    @OnInsert
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {

        boolean hasCreateBy = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), createByField);
        boolean hasCreateOn = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), createOnField);
        boolean hasModifyByField = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), modifyByField);
        boolean hasModifyOnField = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), modifyOnField);

        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);

        if(user == null) {
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        List<ColumnInfo> columnInfos = Queryable.getColumns(ApplicationContext.getCurrentDatasourceId(), EntityContext.getCurrentTable());
        Optional<ColumnInfo> pk = columnInfos.stream().filter(a-> a.getIsPrimaryKey()).findFirst();

        for(Map item : list) {

            setDefaultValues(item);

            vaildPostData(item);

            if(hasCreateBy) {
                item.put(createByField, user.getUserId());
            }

            if(hasCreateOn) {
                item.put(createOnField, Datetime.now());
            }

            if(hasModifyByField) {
                item.put(modifyByField, user.getUserId());
            }

            if(hasModifyOnField) {
                item.put(modifyOnField, Datetime.now());
            }

            if(pk.isPresent() && item.containsKey(pk.get().getColumnName())) {
                if(pk.get().getIsAutoIncrement() || item.get(pk.get().getColumnName())==null ||
                        "".equals(item.get(pk.get().getColumnName()))) {
                    item.remove(pk.get().getColumnName());
                }
            }
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(list);
    }

    private void setDefaultValues(Map<String, Object> postData) {
        Map<String, Object> defaultValueMap = ApplicationConfig.getInstance().getMap("ccweb.defaultValue");
        if(defaultValueMap == null) {
            return;
        }
        defaultValueMap.entrySet().stream().forEach(a-> {
            String key = a.getKey();
            if(a.getKey().indexOf(".")>0) {
                if(a.getKey().startsWith(EntityContext.getCurrentTable() + ".")) {
                    return;
                }

                key = StringUtils.splitString2List(a.getKey(), "\\.").get(1);
            }
            else if(!postData.containsKey(key)) {
                return;
            }

            Object value = a.getValue();
            switch (DefaultValueMode.valueOf(value.toString())) {
                case UUID_RANDOM:
                    value = UUID.randomUUID().toString().replace("-", "");
                    break;
                case DATE_NOW:
                    value = Datetime.now();
                    break;
            }

            if(value != null && StringUtils.isNotEmpty(value.toString())) {
                postData.put(key, value);
            }
        });
    }

    @OnUpdate
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        Map<String, Object> data = queryInfo.getData();

        vaildPostData(data);

        if(data.containsKey(createOnField)) {
            data.remove(createOnField);
        }

        if(data.containsKey(userPathField)) {
            data.remove(userPathField);
        }

        if(data.containsKey(createByField)) {
            data.remove(createByField);
        }

        boolean hasModifyByField = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), modifyByField);
        boolean hasModifyOnField = EntityContext.hasColumn(datasourceId, EntityContext.getCurrentTable(), modifyOnField);

        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        if(user != null) {
            if(hasModifyByField) {
                data.put(modifyByField, user.getUserId());
            }
        }
        else{
            throw new HttpResponseException(HttpStatus.UNAUTHORIZED.value(), LangConfig.getInstance().get("login_please"));
        }

        if(hasModifyOnField) {
            data.put(modifyOnField, Datetime.now());
        }

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        String[] arr = request.getRequestURI().split("/");
        if("update".equals(arr[arr.length - 1].toLowerCase())) {
            wrapper.setPostParameter(queryInfo);
        }

        else {
            wrapper.setPostParameter(data);
        }
    }

    @OnBuildTable
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {

        Object entity = EntityContext.getEntity(EntityContext.getCurrentTable(), queryInfo);
        if(entity == null) {
            throw new Exception(LangConfig.getInstance().get("can_not_find_entity"));
        }

        Queryable query = (Queryable)entity;

        if(Queryable.exist(query.dataSource().getId(), EntityContext.getCurrentTable())) {
            return;
        }

        ColumnInfo col = null;

        if(StringUtils.isNotEmpty(createOnField) && !columns.stream()
                .filter(a->createOnField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(createOnField);
            col.setDataType("DATETIME");
            columns.add(col);
        }

        if(StringUtils.isNotEmpty(createByField) && !columns.stream()
                .filter(a->createByField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(createByField);
            col.setCanNotNull(true);
            col.setDataType("INT");
            columns.add(col);
        }

        if(StringUtils.isNotEmpty(modifyByField) && !columns.stream()
                .filter(a->modifyByField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(modifyByField);
            col.setCanNotNull(true);
            col.setDataType("INT");
            columns.add(col);
        }

        if(StringUtils.isNotEmpty(modifyOnField) && !columns.stream()
                .filter(a->modifyOnField.toLowerCase().equals(a.getColumnName().toLowerCase()))
                .findAny().isPresent()) {

            col = new ColumnInfo();
            col.setColumnName(modifyOnField);
            col.setCanNotNull(true);
            col.setDataType("DATETIME");
            columns.add(col);
        }

        query = null;
        entity = null;

        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(columns);
    }

    private void vaildPostData(Map<String, Object> data) throws Exception {
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("ccweb.validation");
        if(map != null) {
            for(String key : data.keySet()){
                Optional opt = map.keySet().stream()
                        .filter(a -> a.equals(key) ||
                                String.format("%s.%s", EntityContext.getCurrentTable(), key).equals(a))
                        .findAny();

                if(!opt.isPresent()){
                    continue;
                }

                Map vaildation = (Map) opt.get();
                if(vaildation.containsKey("match")) {
                    if(!Pattern.matches(vaildation.get("match").toString(), data.get(key).toString())){
                        if(vaildation.containsKey("message")) {
                            throw new Exception(vaildation.get("message").toString());
                        }
                        throw new Exception(key + LangConfig.getInstance().get("field_has_invalid_parameter_value"));
                    }
                }
            }
        }
    }

    @OnList
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        vaildCondition(queryInfo);
        if(queryInfo.getConditionList() != null) {
            queryInfo.getConditionList().forEach(a -> {
                if(ApplicationConfig.getInstance().get(String.format("${ccweb.table.display.%s.%s}", EntityContext.getCurrentTable(), a.getName())).equals("encrypt")) {
                    String decryptValue = decrypt(a.getValue().toString(), EncryptMode.AES, aesPublicKey);
                    a.setValue(decryptValue);
                }
            });

            CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
            wrapper.setPostParameter(JsonUtils.convert(queryInfo, Map.class));
        }
    }

    @OnQuery
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception {
        vaildCondition(queryInfo);
        if(queryInfo.getConditionList() != null) {
            queryInfo.getConditionList().forEach(a -> {
                if(ApplicationConfig.getInstance().get(String.format("${ccweb.table.display.%s.%s}", EntityContext.getCurrentTable(), a.getName())).equals("encrypt")) {
                    String decryptValue = decrypt(a.getValue().toString(), EncryptMode.AES, aesPublicKey);
                    a.setValue(decryptValue);
                }
            });

            CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
            wrapper.setPostParameter(JsonUtils.convert(queryInfo, Map.class));
        }
    }

    private void vaildCondition(QueryInfo queryInfo) throws Exception {
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("ccweb.validation");
        if(map != null && queryInfo.getConditionList() != null) {
            for(ConditionInfo condition : queryInfo.getConditionList()){
                Optional opt = map.keySet().stream()
                        .filter(a -> a.equals(condition.getName()) ||
                                String.format("%s.%s", EntityContext.getCurrentTable(), condition.getName()).equals(a))
                        .findAny();

                if(opt.isPresent() && !Pattern.matches(opt.get().toString(), condition.getValue().toString())){
                    throw new Exception(condition.getName() + LangConfig.getInstance().get("field_has_invalid_parameter_value"));
                }
            }
        }
    }

    @OnSuccess
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {

        if(responseData.getData() == null) {
            return;
        }

        if(ClassUtils.isBaseType(responseData.getData())) {
            return;
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


            String base64Fields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.BASE64.fields}", "");
            List<String> base64FieldList = StringUtils.splitString2List(base64Fields, ",");
            String aesFields = ApplicationConfig.getInstance().get("${ccweb.security.encrypt.AES.fields}", "");
            List<String> aesFieldList = StringUtils.splitString2List(aesFields, ",");

            for(int i=0; i<list.size(); i++) {
                if(list.get(i)==null) {
                    continue;
                }
                List<String> keyList = (List<String>) list.get(i).keySet().stream().map(a->a!=null ? a.toString() : "").collect(Collectors.toList());
                for(Object key : keyList) {
                    if(ApplicationConfig.getInstance().get(String.format("${ccweb.table.display.%s.%s}", EntityContext.getCurrentTable(), key.toString())).equals("hidden")) {
                        list.get(i).remove(key);
                    }
                    else if(ApplicationConfig.getInstance().get(String.format("${ccweb.table.display.%s.%s}", EntityContext.getCurrentTable(), key.toString())).equals("encrypt")) {
                        String encryptValue = encrypt(list.get(i).get(key).toString(), EncryptMode.AES, aesPublicKey);
                        list.get(i).put(key, encryptValue);
                    }
                    else if(list.get(i).get(key) instanceof Blob) {
                        list.get(i).remove(key);
                    }

                    else if(list.get(i).get(key) instanceof String) {
                        int index = list.get(i).get(key).toString().indexOf("|::|");
                        if(index > 0 && Pattern.matches("[^/]+/[^/:]+::[^/:]+::[^/:\\|]+", list.get(i).get(key).toString().substring(0, index))) {
                            list.get(i).remove(key);
                        }
                    }

                    else if(base64FieldList != null && base64FieldList.size() > 0 && base64FieldList.stream()
                            .allMatch(a -> a.equalsIgnoreCase(key.toString()) || a.equalsIgnoreCase(String.join(".", EntityContext.getCurrentTable(), key.toString())))) {
                        String value = encrypt(list.get(i).get(key).toString(), EncryptMode.BASE64, encoding);
                        list.get(i).put(key, value);
                    }

                    else if(aesFieldList != null && aesFieldList.size() > 0 && aesFieldList.stream()
                            .allMatch(a -> a.equalsIgnoreCase(key.toString()) || a.equalsIgnoreCase(String.join(".", EntityContext.getCurrentTable(), key.toString())))) {
                        String value = encrypt(list.get(i).get(key).toString(), EncryptMode.AES, aesPublicKey);
                        list.get(i).put(key, value);
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

        else if(request.getMethod().equalsIgnoreCase("GET")) {
            if(responseData.getData() instanceof Map) {
                Map<String, Object> dataMap = ((Map<String, Object>) responseData.getData());
                Set<String> keys = dataMap.keySet();
                for(String key : keys) {
                    if(dataMap.get(key) instanceof Blob) {
                        dataMap.remove(key);
                    }

                    else if(dataMap.get(key) instanceof String) {
                        int index = dataMap.get(key).toString().indexOf("|::|");
                        if(index > 0 && Pattern.matches("[^/]+/[^/:]+::[^/:]+::[^/:\\|]+", dataMap.get(key).toString().substring(0, index))) {
                            dataMap.remove(key);
                        }
                    }
                }
            }
        }
    }
}
