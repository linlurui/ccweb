/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2020-11-09
 */

package ccait.ccweb.trigger;

import ccait.ccweb.RestAgent;
import ccait.ccweb.annotation.*;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.PlatformInfo;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.controllers.WebAgentController;
import ccait.ccweb.entites.SaveDataInfo;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserGroupRoleModel;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.StaticVars;
import entity.query.Datetime;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import ccait.ccweb.config.LangConfig;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static ccait.ccweb.utils.StaticVars.LOGIN_KEY;
import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;

@Component
@Scope("prototype")
@Order(Ordered.HIGHEST_PRECEDENCE+500)
@Trigger
public class AgentTrigger {

    private static final Logger log = LoggerFactory.getLogger( AgentTrigger.class );
    private Map<String, Object> agentProfile;

    @Value("${ccweb.table.reservedField.userPath:userPath}")
    private String userPathField;

    @Value("${ccweb.table.reservedField.createOn:createOn}")
    private String createOnField;

    @Value("${ccweb.table.reservedField.modifyOn:modifyOn}")
    private String modifyOnField;

    @Value("${ccweb.table.reservedField.createBy:createBy}")
    private String createByField;

    @Value("${ccweb.table.reservedField.owner:owner}")
    private String ownerField;

    @Value("${ccweb.table.reservedField.modifyBy:modifyBy}")
    private String modifyByField;

    @Value("${ccweb.table.user:user}")
    private String userTable;

    @Value("${ccweb.table.reservedField.userId:userId}")
    private String userIdField;

    private boolean hasAgentNode = false;

    @Autowired
    private WebAgentController webAgentController;

    @PostConstruct
    private void init() {
        hasAgentNode = ApplicationConfig.getInstance().getMap("").containsKey("entity") && ApplicationConfig.getInstance().getMap("entity").containsKey("agent");
        agentProfile = ApplicationConfig.getInstance().getMap("ccweb.agent");
        createOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createOn}", createOnField);
        modifyOnField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyOn}", modifyOnField);
        modifyByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.modifyBy}", modifyByField);
        createByField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.createBy}", createByField);
        userTable = ApplicationConfig.getInstance().get("${ccweb.table.user}", userTable);
        userIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.userId}", userIdField);
        ownerField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.owner}", ownerField);
        log.info("init trigger  =================================> " + this.getClass().getSimpleName());
    }

    @OnSave
    public void onSave(SaveDataInfo saveDataInfo, HttpServletRequest request) throws Exception {
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(saveDataInfo);
    }

    @OnInsert
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {

        if(isCancel(request) || !hasAgentNode) {
            return;
        }

        String table = EntityContext.getCurrentTable();
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        List<ResponseData> responseList = new ArrayList<>();

        Map returnData = null;
        int continueCount = 0;
        for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
            Map<String, Map> profile = (Map) platformProfile.getValue();

            Optional<Map> opt = profile.entrySet().stream()
                    .filter(a -> "request".equals(a.getKey()))
                    .map(b->b.getValue())
                    .filter(a-> a.containsKey(table))
                    .map(b-> (Map)b.get(table))
                    .filter(a-> a.containsKey("insert"))
                    .map(b-> (Map)b.get("insert"))
                    .findFirst();

            if(!opt.isPresent()) {
                continueCount++;
                error(responseList, new Exception(String.format("fail to insert config for %s!!!", table)));
                continue;
            }

            PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
            RestAgent rest = new RestAgent(platformInfo, opt.get());
            rest.replaceUserId(platformInfo.getRequired(), user);
            rest.replaceUserId(rest.getAgentProfile().getRequired(), user);

            for (Map item : list) {
                try {
                    rest.replaceUserId(item, user);
                    Map result = rest.invoke(item);
                    if(result == null) {
                        success(responseList, null);
                        continue;
                    }

                    result.put(createByField, user.getUserId());
                    result.put(createOnField, Datetime.now());
                    result.put(modifyByField, user.getUserId());
                    result.put(modifyOnField, Datetime.now());

                    if(platformInfo.isEnsureTable()) {
                        if(!ApplicationContext.existTable(table)) {
                            ApplicationContext.ensureTable(result, rest.getAgentProfile().getUniqueList(), table);
                        }
                        else {
                            ApplicationContext.ensureColumns(result, rest.getAgentProfile().getUniqueList());
                        }
                    }
                    returnData = JsonUtils.convert(result, Map.class);

                    Object id = webAgentController.insert(table, result);

                    if(userTable.equals(table)) {
                        UserGroupRoleModel userGroupRole = new UserGroupRoleModel();
                        Optional<UserGroupRoleModel> optionalUserGroupRole = user.getUserGroupRoleModels().stream()
                                .filter(a -> a.getUserId().equals(user.getUserId())).findFirst();

                        if(optionalUserGroupRole.isPresent()) {
                            userGroupRole.setPath(String.format("%s/%s", optionalUserGroupRole.get().getPath(), id.toString()));
                        }

                        else {
                            userGroupRole.setPath(String.format("%s/%s", user.getUserId(), id.toString()));
                        }

                        userGroupRole.setCreateBy(user.getUserId());
                        userGroupRole.setCreateOn(Datetime.now());
                        userGroupRole.setModifyBy(user.getUserId());
                        userGroupRole.setModifyOn(Datetime.now());
                        userGroupRole.setUserId(Integer.parseInt(id.toString()));
                        userGroupRole.setUserGroupRoleId(UUID.randomUUID().toString().replace("-", ""));
                        if(rest.getAgentProfile().getRoleId() != null) {
                            userGroupRole.setRoleId(rest.getAgentProfile().getRoleId());
                        }
                        userGroupRole.insert();
                    }

                    success(responseList, returnData);
                }

                catch(Exception e) {
                    log.error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
                    error(responseList, e);
                }
            }
        }

        if(continueCount == agentProfile.size()) {
            return;
        }

        end(responseList);
    }

    @OnUpdate
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        if(isCancel(request) || !hasAgentNode) {
            return;
        }

        if(queryInfo.getConditionList()!=null && queryInfo.getConditionList().size()>0) {
            return;
        }

        List<ResponseData> responseList = new ArrayList<>();
        String table = EntityContext.getCurrentTable();
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        Map attr = (Map)request.getAttribute(StaticVars.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if(!attr.containsKey("id")) {
            return;
        }

        String id = attr.get("id").toString();

        int continueCount = 0;
        for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
            Map<String, Map> profile = (Map) platformProfile.getValue();

            Optional<Map> opt = profile.entrySet().stream()
                    .filter(a -> "request".equals(a.getKey()))
                    .map(b->b.getValue())
                    .filter(a-> a.containsKey(table))
                    .map(b-> (Map)b.get(table))
                    .filter(a-> a.containsKey("update"))
                    .map(b-> (Map)b.get("update"))
                    .findFirst();

            if(!opt.isPresent()) {
                continueCount++;
                error(responseList, new Exception(String.format("fail to update config for %s!!!", table)));
                continue;
            }

            PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
            RestAgent rest = new RestAgent(platformInfo, opt.get());
            rest.replaceUserId(platformInfo.getRequired(), user);
            rest.replaceUserId(rest.getAgentProfile().getRequired(), user);

            Map<String, Object> postData = queryInfo.getData();

            if(StringUtils.isEmpty(rest.getAgentProfile().getPrimaryKey())) {
                new Exception(LangConfig.getInstance().get("can_not_find_primary_key"));
            }

            try {
                Map data = webAgentController.getDataByPrimaryKey(id, rest);
                if(data!=null && data.containsKey("id")) {
                    setRequiredValues(id, rest, data);
                }
                else {
                    throw new Exception(LangConfig.getInstance().get("can_not_find_data_for_update"));
                }

                rest.replaceUserId(postData, user);
                Map result = rest.invoke(postData);
                if(result == null) {
                    success(responseList, null);
                    continue;
                }

                result.put(modifyByField, user.getUserId());
                result.put(modifyOnField, Datetime.now());

                Map returnData = JsonUtils.convert(result, Map.class);
                if(platformInfo.isEnsureTable()) {
                    ApplicationContext.ensureColumns(result);
                }
                webAgentController.update(table, data.get("id").toString(), result);
                success(responseList, returnData);
            }

            catch(Exception e) {
                log.error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
                error(responseList, e);
            }
        }

        if(continueCount == agentProfile.size()) {
            return;
        }

        end(responseList);
    }

    @OnDelete
    public void onDelete(String id, HttpServletRequest request) throws Exception {

        if(isCancel(request) || !hasAgentNode) {
            return;
        }

        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);
        List<ResponseData> responseList = new ArrayList<>();
        String table = EntityContext.getCurrentTable();

        int continueCount = 0;
        for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
            Map<String, Map> profile = (Map) platformProfile.getValue();

            Optional<Map> opt = profile.entrySet().stream()
                    .filter(a -> "request".equals(a.getKey()))
                    .map(b->b.getValue())
                    .filter(a-> a.containsKey(table))
                    .map(b-> (Map)b.get(table))
                    .filter(a-> a.containsKey("delete"))
                    .map(b-> (Map)b.get("delete"))
                    .findFirst();

            if(!opt.isPresent()) {
                continueCount++;
                error(responseList, new Exception(String.format("fail to delete config for %s!!!", table)));
                continue;
            }

            PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
            RestAgent rest = new RestAgent(platformInfo, opt.get());
            rest.replaceUserId(platformInfo.getRequired(), user);
            rest.replaceUserId(rest.getAgentProfile().getRequired(), user);
            Map<String, Object> postData = new HashMap<>();

            if(StringUtils.isEmpty(rest.getAgentProfile().getPrimaryKey())) {
                new Exception(LangConfig.getInstance().get("can_not_find_primary_key"));
            }

            try {
                Map data = webAgentController.getDataByPrimaryKey(id, rest);
                if(data!=null && data.containsKey("id")) {
                    setRequiredValues(id, rest, data);
                }

                rest.replaceUserId(postData, user);
                Map result = rest.invoke(postData);
                if(userTable.equals(table)) {
                    String userId = data.get(userIdField).toString();
                    webAgentController.delete(table, userId);
                    UserGroupRoleModel userGroupRole = new UserGroupRoleModel();
                    userGroupRole.setUserId(Integer.parseInt(userId));
                    userGroupRole.where(String.format("%s=#{userId}", userIdField)).delete();
                }
                else {
                    webAgentController.delete(table, data.get("id").toString());
                }
                success(responseList, result);
            }

            catch(Exception e) {
                log.error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
                error(responseList, e);
            }
        }

        if(continueCount == agentProfile.size()) {
            return;
        }

        end(responseList);
    }

    @OnView
    public void onView(String id, HttpServletRequest request)throws Exception {

        if(isCancel(request) || !hasAgentNode) {
            return;
        }

        List<ResponseData> responseList = new ArrayList<>();
        String table = EntityContext.getCurrentTable();
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);

        int continueCount = 0;
        for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
            Map<String, Map> profile = (Map) platformProfile.getValue();

            Optional<Map> opt = profile.entrySet().stream()
                    .filter(a -> "request".equals(a.getKey()))
                    .map(b->b.getValue())
                    .filter(a-> a.containsKey(table))
                    .map(b-> (Map)b.get(table))
                    .filter(a-> a.containsKey("get"))
                    .map(b-> (Map)b.get("get"))
                    .findFirst();

            if(!opt.isPresent()) {
                continueCount++;
                error(responseList, new Exception(String.format("fail to get config for %s!!!", table)));
                continue;
            }

            PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
            RestAgent rest = new RestAgent(platformInfo, opt.get());
            rest.replaceUserId(platformInfo.getRequired(), user);
            rest.replaceUserId(rest.getAgentProfile().getRequired(), user);
            if(StringUtils.isEmpty(rest.getAgentProfile().getPrimaryKey())) {
                new Exception(LangConfig.getInstance().get("can_not_find_primary_key"));
            }

            try {

                Map<String, Object> postData = new HashMap<>();
                List<String> list = StringUtils.splitString2List(request.getQueryString(), "&");
                for(String item : list) {
                    List<String> paramPart = StringUtils.splitString2List(item, "=");
                    if(paramPart.size() != 2) {
                        continue;
                    }
                    postData.put(paramPart.get(0), paramPart.get(1));
                }
                postData.put(rest.getAgentProfile().getPrimaryKey(), id);
                rest.replaceUserId(postData, user);

                Map result = rest.invoke(postData);
                success(responseList, result);
            }

            catch(Exception e) {
                log.error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
                error(responseList, e);
            }
        }

        if(continueCount == agentProfile.size()) {
            return;
        }

        end(responseList);
    }

    @OnList
    @OnQuery
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        if(isCancel(request) || !hasAgentNode) {
            return;
        }
        UserModel user = ApplicationContext.getSession(request, LOGIN_KEY, UserModel.class);

        List<ResponseData> responseList = new ArrayList<>();
        String table = EntityContext.getCurrentTable();
        int continueCount = 0;
        for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
            Map<String, Map> profile = (Map) platformProfile.getValue();

            Optional<Map> opt = profile.entrySet().stream()
                    .filter(a -> "request".equals(a.getKey()))
                    .map(b->b.getValue())
                    .filter(a-> a.containsKey(table))
                    .map(b-> (Map)b.get(table))
                    .filter(a-> a.containsKey("query"))
                    .map(b-> (Map)b.get("query"))
                    .findFirst();

            if(!opt.isPresent()) {
                continueCount++;
                error(responseList, new Exception(String.format("fail to query config for %s!!!", table)));
                continue;
            }

            PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
            RestAgent rest = new RestAgent(platformInfo, opt.get());
            rest.replaceUserId(platformInfo.getRequired(), user);
            rest.replaceUserId(rest.getAgentProfile().getRequired(), user);
            Map<String, Object> postData = queryInfo.getData();
            if(postData == null) {
                continue;
            }

            try {
                rest.replaceUserId(postData, user);
                Map result = rest.invoke(postData);
                success(responseList, result);
            }

            catch(Exception e) {
                log.error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
                error(responseList, e);
            }
        }

        if(continueCount == agentProfile.size()) {
            return;
        }

        end(responseList);
    }

    private void setRequiredValues(String id, RestAgent rest, Map data) {
        for(Map.Entry<String, Object> item : rest.getAgentProfile().getRequired().entrySet()) {
            if("${id}".equals(item.getValue())) {
                item.setValue(id);
            }

            else {
                Matcher m = Pattern.compile("^\\$\\{(\\w[\\w\\d]*)\\}$").matcher(item.getValue().toString());
                if (m.matches()) {
                    item.setValue(m.group(1));
                }
            }
        }
    }

    private void success(List<ResponseData> responseList, Map data) {

        ResponseData responseData = new ResponseData();
        responseData.setMessage("success");
        responseData.setData(data);
        responseData.setStatus(0);
        responseList.add(responseData);
    }

    private void error(List<ResponseData> responseList, Exception e) {

        ResponseData responseData = new ResponseData();
        log.error("RestAgent Error!!!", e);
        responseData.setMessage(e.getMessage()==null ? e.toString() : e.getMessage());
        responseData.setStatus(500);
        responseList.add(responseData);
    }

    private void end(List<ResponseData> responseList) throws IOException {
        if (responseList.size() > 0) {
            webAgentController.write(responseList);
            webAgentController.end();
        }
    }

    private boolean isCancel(HttpServletRequest request) {
        return StringUtils.splitString2List(request.getQueryString(), "&").contains("agent=false");
    }
}
