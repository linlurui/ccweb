package ccait.ccweb.controllers;

import ccait.ccweb.RestAgent;
import ccait.ccweb.abstracts.AbstractWebController;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.ConditionInfo;
import ccait.ccweb.entites.PlatformInfo;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.enums.Algorithm;
import ccait.ccweb.express.KeyValueParts;
import ccait.ccweb.express.VarInfo;
import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.NetworkUtils;
import ccait.ccweb.utils.StaticVars;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static ccait.ccweb.utils.StaticVars.LOG_PRE_SUFFIX;


@RestController
@RequestMapping( value = {"api/{datasource}"} )
public class WebAgentController extends AbstractWebController {

    private Map<String, Object> agentProfile;
    private boolean hasAgentNode;

    @PostConstruct
    private void init() {
        hasAgentNode = ApplicationConfig.getInstance().getMap("").containsKey("entity") && ApplicationConfig.getInstance().getMap("entity").containsKey("agent");
        agentProfile = ApplicationConfig.getInstance().getMap("ccweb.agent");
    }

    /***
     * test/agent
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "test/agent", method = {RequestMethod.POST}  )
    public ResponseData<RestAgent> test(@RequestBody RestAgent restAgent) throws Exception {

        UserModel user = getLoginUser();
        if(user == null || !admin.equals(user.getUsername())) {
            throw new Exception(LangConfig.getInstance().get("you_are_not_administrator"));
        }

        restAgent.replaceUserId(restAgent.getPlatformInfo().getRequired(), user);
        restAgent.replaceUserId(restAgent.getAgentProfile().getRequired(), user);

        Map postData = restAgent.getPostData();
        restAgent.replaceUserId(postData, user);

        Map<String, String> signFieldMap = new HashMap<>();
        postData = restAgent.ensurePostData(postData, signFieldMap);
        restAgent.setPostData(postData);

        Map headers = restAgent.getHeaders();
        restAgent.replaceUserId(headers, user);
        signFieldMap = new HashMap<>();
        headers = restAgent.ensureHeaders(headers, signFieldMap);
        restAgent.setHeaders(headers);

        return success(restAgent);
    }

    /***
     * test/sign
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "test/sign", method = {RequestMethod.POST}  )
    public ResponseData<String> sign(@RequestBody RestAgent restAgent) throws Exception {

        UserModel user = getLoginUser();
        if(user == null || !admin.equals(user.getUsername())) {
            throw new Exception(LangConfig.getInstance().get("you_are_not_administrator"));
        }

        Map<String, String> signFieldMap = new HashMap<>();

        KeyValueParts keyValueParts = new KeyValueParts();
        restAgent.getPostData().entrySet().stream().forEach(a->keyValueParts.put(a.getKey(), a.getValue()));

        //填充平台级别必填字段
        restAgent.fillRequriedFieldsToPostData(keyValueParts, signFieldMap, restAgent.getPlatformInfo().getRequired());

        //填充接口级别必填字段
        restAgent.fillRequriedFieldsToPostData(keyValueParts, signFieldMap, restAgent.getAgentProfile().getRequired());

        String sign = restAgent.sign(restAgent.getPostData(), signFieldMap);

        return success(sign);
    }

    /***
     * callback
     * @return
     */
    @ResponseBody
    @RequestMapping( value = "callback/{datasource}/{table}/{id}", method = {RequestMethod.POST}  )
    public Object callback(@PathVariable String table, @PathVariable String id, @RequestBody Map data) {

        String lastMessage = "Unknown Error";
        
        try{
            if(!hasAgentNode) {
                return error(lastMessage);
            }

            if(data==null || data.size()==0) {
                return error("params can not be empty!!!");
            }

            for (Map.Entry<String, Object> platformProfile : agentProfile.entrySet()) {
                Map<String, Map> profile = (Map) platformProfile.getValue();

                Optional<Map> opt = profile.entrySet().stream()
                        .filter(a -> "request".equals(a.getKey()))
                        .map(b->b.getValue())
                        .filter(a-> a.containsKey(table))
                        .map(b-> (Map)b.get(table))
                        .filter(a-> a.containsKey("callback"))
                        .map(b-> (Map)b.get("callback"))
                        .findFirst();

                if(!opt.isPresent()) {
                    lastMessage = String.format("fail to callback config for %s!!!", table);
                    continue;
                }

                PlatformInfo platformInfo = RestAgent.getPlatformInfo(platformProfile);
                RestAgent rest = new RestAgent(platformInfo, opt.get());
                if(StringUtils.isEmpty(rest.getAgentProfile().getPrimaryKey())) {
                    lastMessage = "primary key field setting can not be empty!!!";
                    continue;
                }

                if(rest.getAgentProfile().isCheckDomain()) { //检查平台域名
                    if(!profile.containsKey("domain") || profile.get("domain")==null) {
                        lastMessage = "no support platform any!!!";
                        continue;
                    }

                    String clientDomain = NetworkUtils.getClientDomain(request);
                    String profileDomain = ((Map) platformProfile.getValue()).get("domain").toString().replaceAll("https?://", "");
                    getLogger().info(LOG_PRE_SUFFIX + "Agent Callback Domain ======> " + clientDomain);
                    if(!clientDomain.equals(profileDomain)) {
                        lastMessage =  "no support this platform!!!";
                        continue;
                    }
                }

                final Map<String, String> signFieldMap = new HashMap<String, String>();
                platformInfo.getRequired().entrySet().stream()
                        .filter(a -> VarInfo.parse(a.getValue().toString(), "platform", "sign").isMatches())
                        .forEach(a-> signFieldMap.put(a.getKey(), a.getValue().toString()));

                if(rest.getAgentProfile().getRequired()==null) {
                    return error("config required can not be empty!!!");
                }

                rest.getAgentProfile().getRequired().entrySet().stream()
                        .filter(a -> VarInfo.parse(a.getValue().toString(), "platform", "sign").isMatches())
                        .forEach(a-> signFieldMap.put(a.getKey(), a.getValue().toString()));

                if(signFieldMap.size() != 1) {
                    return error("lost sign field!!!");
                }

                if(StringUtils.isNotEmpty(rest.getAgentProfile().getData())) {
                    String dataFieldname = rest.getAgentProfile().getData();
                    data = JsonUtils.convert(data.get(dataFieldname), Map.class);

                    if(data==null) {
                        return error(String.format("can not find %s!!!", dataFieldname));
                    }
                }

                Map postData = data;
                if(platformInfo.getRequired().entrySet().stream()
                        .filter(a->!postData.containsKey(a.getKey()))
                        .isParallel()) {
                    return error("lost required field!!!");
                }

                if(rest.getAgentProfile().getRequired().entrySet().stream()
                        .filter(a->!postData.containsKey(a.getKey()))
                        .isParallel()) {
                    return error("lost required field!!!");
                }

                String signFieldKey = "";
                boolean hasSignField = false;
                for(String key : signFieldMap.keySet()) {
                    if(postData.containsKey(key) && postData.get(key)!=null && StringUtils.isNotEmpty(postData.get(key).toString())) {
                        hasSignField = true;
                        signFieldKey = key;
                    }
                }

                if(!hasSignField) {
                    return error("can not find sign code!!!");
                }

                String signCode = postData.get(signFieldKey).toString();
                if(!signCode.equals(rest.sign(postData, signFieldMap))) {
                    return error("invalid sign code!!!");
                }

                if(platformInfo.isEnsureTable()) {
                    if(!ApplicationContext.existTable(table)) {
                        ApplicationContext.ensureTable(postData, rest.getAgentProfile().getUniqueList(), table);
                    }
                    else {
                        ApplicationContext.ensureColumns(postData, rest.getAgentProfile().getUniqueList());
                    }
                }

                Map dataMap = getDataByPrimaryKey(id, rest);
                if(dataMap == null) {
                    insert(table, postData);
                }

                else {
                    update(table, dataMap.get("id").toString(), postData);
                }

                if(StringUtils.isNotEmpty(rest.getAgentProfile().getResponse())) {
                    return rest.getAgentProfile().getResponse();
                }
                return success();
            }
        }
        catch (Exception e) {
            getLogger().error(LOG_PRE_SUFFIX + "ERROR=====> ", e);
            return error(e.getMessage());
        }

        return error(lastMessage);
    }

    public Map getDataByPrimaryKey(String id, RestAgent rest) throws Exception {
        QueryInfo exp = new QueryInfo();
        ConditionInfo condition = new ConditionInfo();
        condition.setName(rest.getAgentProfile().getPrimaryKey());
        condition.setValue(id);
        condition.setAlgorithm(Algorithm.EQ);
        exp.setConditionList(new ArrayList<>());
        exp.getConditionList().add(condition);
        PageInfo pageInfo = new PageInfo();
        pageInfo.setPageSize(1);
        pageInfo.setPageIndex(1);
        exp.setPageInfo(pageInfo);
        List<Map> dataList = query(EntityContext.getCurrentTable(), exp);
        if(dataList.size()==0) {
            return null;
        }

        return dataList.get(0);
    }

    public void end() throws IOException {
        response.flushBuffer();
        PrintWriter writer = response.getWriter();
        writer.close();
        ApplicationContext.getThreadLocalMap().put(StaticVars.RESPONSE_END, true);
    }

    public void write(List<ResponseData> responseList) throws IOException {
        if(responseList == null) {
            return;
        }

        response.getWriter().write(JsonUtils.toJson(responseList));
    }
}
