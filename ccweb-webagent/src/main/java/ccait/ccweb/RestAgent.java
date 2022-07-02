/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2020-11-09
 */

package ccait.ccweb;

import ccait.ccweb.config.LangConfig;
import ccait.ccweb.entites.AgentProfile;
import ccait.ccweb.entites.PlatformInfo;
import ccait.ccweb.express.*;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import ccait.ccweb.utils.NetworkUtils;
import entity.query.Datetime;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.RequestUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RestAgent {

    private static final Logger log = LoggerFactory.getLogger( RestAgent.class );

    AgentProfile agentProfile;
    PlatformInfo platformInfo;
    Map<String, Object> postData;
    Map<String, Object> headers;

    public RestAgent() {}

    public RestAgent(PlatformInfo platformInfo, AgentProfile agentProfile) {
        this.platformInfo = platformInfo;
        this.agentProfile = agentProfile;
    }

    public RestAgent(PlatformInfo platformInfo, Map agentProfileMap) {
        this.platformInfo = platformInfo;
        this.agentProfile = new AgentProfile(agentProfileMap);
    }

    public AgentProfile getAgentProfile() {
        return agentProfile;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public Map<String, Object> getPostData() {
        return postData;
    }

    public void setPostData(Map<String, Object> postData) {
        this.postData = postData;
    }

    public void setAgentProfile(AgentProfile agentProfile) {
        this.agentProfile = agentProfile;
    }

    public void setPlatformInfo(PlatformInfo platformInfo) {
        this.platformInfo = platformInfo;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public static PlatformInfo getPlatformInfo(Map.Entry platformProfile) {
        PlatformInfo platformInfo = new PlatformInfo();
        platformInfo.setPlatform(platformProfile.getKey().toString());
        Map map = (Map) platformProfile.getValue();
        if(map == null || map.size() < 1) {
            return platformInfo;
        }

        if(map.containsKey("description") && map.get("description")!=null) {
            platformInfo.setDescription(map.get("description").toString());
        }

        if(map.containsKey("key") && map.get("key")!=null) {
            platformInfo.setKey(map.get("key").toString());
        }

        if(map.containsKey("domain") && map.get("domain")!=null) {
            platformInfo.setDomain(map.get("domain").toString());
        }

        if(map.containsKey("name") && map.get("name")!=null) {
            platformInfo.setName(map.get("name").toString());
        }

        if(map.containsKey("sign") && map.get("sign")!=null) {
            platformInfo.setSign(map.get("sign").toString());
        }

        if(map.containsKey("token") && map.get("token")!=null) {
            platformInfo.setToken(map.get("token").toString());
        }

        if(map.containsKey("id") && map.get("id")!=null) {
            platformInfo.setId(map.get("id").toString());
        }

        if(map.containsKey("headers") && map.get("headers")!=null &&
                map.get("headers") instanceof Map) {
            platformInfo.setHeaders((Map)map.get("headers"));
        }

        if(map.containsKey("charset") && map.get("charset")!=null) {
            platformInfo.setCharset(map.get("charset").toString());
        }

        if(map.containsKey("ensureTable") && map.get("ensureTable")!=null) {
            platformInfo.setEnsureTable("true".equals(map.get("ensureTable").toString()));
        }

        if(map.containsKey("required") && map.get("required") != null) {
            platformInfo.setRequired((Map)map.get("required"));
        }

        if(map.containsKey("saveRequired") && map.get("saveRequired")!=null) {
            platformInfo.setSaveRequired("true".equals(map.get("saveRequired").toString()));
        }

        if(map.containsKey("cancel") && map.get("cancel")!=null &&
                map.get("cancel") instanceof Map) {
            platformInfo.setCancel((Map)map.get("cancel"));
        }

        return platformInfo;
    }

    public static boolean isReserved(String table) {
        if("domain".equals(table)) {
            return true;
        }

        if("sign".equals(table)) {
            return true;
        }

        if("name".equals(table)) {
            return true;
        }

        if("key".equals(table)) {
            return true;
        }

        return false;
    }

    public Map invoke(Map postData) throws Exception {
        String serviceUrl = platformInfo.getDomain() + agentProfile.getPath();
        Map<String, String> signFieldMap = new HashMap<>();
        String postString = getJsonFromPostData(postData, signFieldMap);
        log.info("Agent Request:----->" + serviceUrl);
        log.info("Agent PostString:-----> " + postString);
        signFieldMap = new HashMap<>();
        Map<String, Object> headParams = ensureHeaders(platformInfo.getHeaders(), signFieldMap);
        String restMethod = agentProfile.getMethod();
        String responseString = RequestUtils.request(serviceUrl, postString, headParams, restMethod);
        if(StringUtils.isEmpty(responseString)) {
            return null;
        }
        Map result = getMapFromDataString(postData, responseString);

        return result;
    }

    private Map getMapFromDataString(Map<String, Object> postData, String dataString) throws IOException {

        boolean isSuccess = false;
        Map result = JsonUtils.parse(dataString, Map.class);
        if(agentProfile.getSuccess()!=null && agentProfile.getSuccess().size()>0){
            for(Map.Entry<String, String> item : agentProfile.getSuccess().entrySet()) {

                if(item.getValue() == null) {
                    continue;
                }

                if(!result.containsKey(item.getKey())) {
                    continue;
                }

                if(result.get(item.getKey()) == null) {
                    continue;
                }

                if(result.get(item.getKey()).equals(item.getValue())) {
                    if(StringUtils.isNotEmpty(agentProfile.getData())) {
                        result = JsonUtils.convert(result.get(agentProfile.getData()), Map.class);
                    }
                    isSuccess = true;
                    break;
                }
            }
        }

        if(!isSuccess) {
            throw new IOException(String.format(LangConfig.getInstance().get("fail_to_request_message"), platformInfo.getPlatform(), dataString));
        }

        //ensure fixedColumns
        if(agentProfile.getFixedColumns()!=null && agentProfile.getFixedColumns().size()>0) {
            for(Map.Entry<String, String> item : agentProfile.getFixedColumns().entrySet()) {
                String value = replaceVars(item.getValue());
                List<VarInfo> varInfoList = VarInfo.parseList(value, "response");
                if(varInfoList.size()>0) {
                    value = getVarInfosValue(varInfoList, result, value);
                }

                else {
                    varInfoList = VarInfo.parseList(value, "post");
                    value = getVarInfosValue(varInfoList, postData, value);
                }

                item.setValue(value);
            }
        }

        if("dont".equals(agentProfile.getSave())) {
            return null;
        }

        else if("both".equals(agentProfile.getSave())) {
            postData.putAll(result);
            result = postData;
        }

        else if("post".equals(agentProfile.getSave())) {
            result = postData;
        }

        if(agentProfile.getOutMap()!=null && agentProfile.getOutMap().size()>0) {
            //mapping post data
            for(Map.Entry<String, String> item : agentProfile.getOutMap().entrySet()) {
                if(result.containsKey(item.getKey())) {
                    Object value = result.get(item.getKey());
                    result.remove(item.getKey());
                    result.put(item.getValue(), value);
                }
            }
        }

        if(!platformInfo.isSaveRequired()) {
            for(String key : platformInfo.getRequired().keySet()) {
                result.remove(key);
            }
        }

        return result;
    }

    private String getVarInfosValue(List<VarInfo> varInfos, Map<String, Object> dataMap, String defaultValue) {
        String value = defaultValue;
        for(VarInfo varInfo : varInfos) {
            if (varInfo.isMatches() && dataMap.containsKey(varInfo.getName()) && dataMap.get(varInfo.getName()) != null) {
                value = varInfo.replace(dataMap.get(varInfo.getName()).toString());
            }
        }

        return value;
    }

    private String getJsonFromPostData(Map<String, Object> postData, Map<String, String> signFieldMap) throws Exception {

        postData = ensurePostData(postData, signFieldMap);

        String postString;
        if("url".equals(agentProfile.getMode())) {
            postString = getSortParamsBuff(postData, true).toString();
        }

        else {
            postString = JsonUtils.toJson(postData);
        }

        return postString;
    }

    /**
     * 替换变量，执行表达式等
     * @param postData
     * @return
     * @throws Exception
     */
    public Map<String, Object> ensureHeaders(Map<String, Object> postData, Map<String, String> signFieldMap) throws Exception {

        if(postData == null) {
            return null;
        }

        KeyValueParts keyValueParts = new KeyValueParts();
        postData.entrySet().stream().forEach(a->keyValueParts.put(a.getKey(), a.getValue()));

        //填充平台级别必填字段
        fillRequriedFieldsToPostData(keyValueParts, signFieldMap, platformInfo.getHeaders());

        //替换掉post变量
        replaceFromPostData(keyValueParts);
        //解析字典表达式
        handleDictExpr(keyValueParts);
        //执行表达式并填充
        Expr.fillData(keyValueParts);

        postData = keyValueParts.toMap();

        //签名
        sign(postData, signFieldMap);
        for(Map.Entry<String, String> item : signFieldMap.entrySet()) {
            postData.put(item.getKey(), item.getValue());
        }

        return postData;
    }

    /**
     * 替换变量，执行表达式等
     * @param postData
     * @return
     * @throws Exception
     */
    public Map<String, Object> ensurePostData(Map<String, Object> postData, Map<String, String> signFieldMap) throws Exception {

        if(postData == null) {
            return null;
        }

        if(agentProfile.getInMap()!=null && agentProfile.getInMap().size()>0) {
            //mapping post data
            for(Map.Entry<String, String> item : agentProfile.getInMap().entrySet()) {
                if(postData.containsKey(item.getKey())) {
                    Object value = postData.get(item.getKey());
                    postData.remove(item.getKey());
                    postData.put(item.getValue(), value);
                }
            }
        }

        KeyValueParts keyValueParts = new KeyValueParts();
        postData.entrySet().stream().forEach(a->keyValueParts.put(a.getKey(), a.getValue()));
        
        //填充平台级别必填字段
        fillRequriedFieldsToPostData(keyValueParts, signFieldMap, platformInfo.getRequired());

        //填充接口级别必填字段
        fillRequriedFieldsToPostData(keyValueParts, signFieldMap, agentProfile.getRequired());

        //替换掉post变量
        replaceFromPostData(keyValueParts);
        //解析字典表达式
        handleDictExpr(keyValueParts);
        //执行表达式并填充
        Expr.fillData(keyValueParts);

        postData = keyValueParts.toMap();

        //签名
        sign(postData, signFieldMap);
        for(Map.Entry<String, String> item : signFieldMap.entrySet()) {
            postData.put(item.getKey(), item.getValue());
        }

        return postData;
    }

    private void handleDictExpr(KeyValueParts postDataList) {
        for(KeyValueParts.Part item  : postDataList) {
            String value = DictInfo.parseList(item.getValue().toString()).replace();
            item.setValue(value);
        }
    }

    public void replaceUserId(Map<String, Object> data, UserModel user) {

        if(data==null || data.size() < 1) {
            return;
        }

        if(user == null || user.getUserId()==null) {
            return;
        }

        for(String key : data.keySet()) {
            String value =data.get(key).toString().replaceAll("\\$\\{\\s*USERID\\s*\\}", user.getUserId().toString());
            data.put(key, value);
        }
    }

    private void replaceFromPostData(KeyValueParts postDataList) {
        for(KeyValueParts.Part item  : postDataList) {
            String text = item.getValue().toString();
            VarInfoList varInfos = VarInfo.parseList(text, "post");
            for(VarInfo varInfo : varInfos) {
                if (varInfo == null || !varInfo.isMatches()) {
                    continue;
                }

                if (!postDataList.containsKey(varInfo.getName()) || postDataList.get(varInfo.getName()) == null) {
                    continue;
                }

                text = varInfo.replace(text, postDataList.get(varInfo.getName()));
            }
            item.setValue(text);
        }
    }

    public Map<String, String> fillRequriedFieldsToPostData(KeyValueParts postDataList, Map<String, String> signFieldMap, Map<String, Object> required) {
        if (required != null) {
            for (Map.Entry<String, Object> item : required.entrySet()) {
                String value = item.getValue().toString();
                VarInfo varInfo = VarInfo.parse(value, "platform", "sign");

                if(varInfo.isMatches()) {
                    signFieldMap.put(item.getKey(), item.getValue().toString());
                    continue;
                }

                postDataList.put(item.getKey(), replaceVars(item.getValue().toString()));
            }
        }

        return signFieldMap;
    }

    private StringBuffer getSortParamsBuff(Map<String, Object> postData, boolean urlEncode) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        TreeMap<String, Object> ps = new TreeMap<>(postData);
        Set<String> keys = ps.keySet();
        boolean first = true;
        for (String key : keys) {
            if(!first) {
                sb.append("&");
            }

            sb.append(key);
            sb.append("=");
            if(ps.get(key) != null) {
                if(urlEncode) {
                    sb.append(URLEncoder.encode(ps.get(key).toString().trim(), platformInfo.getCharset()));
                }

                else {
                    sb.append(ps.get(key).toString().trim());
                }
            }
            first = false;
        }

        return sb;
    }

    public String sign(Map<String, Object> postData, Map<String, String> signFieldMap) throws Exception {
        if(StringUtils.isEmpty(platformInfo.getSign())) {
            return "";
        }

        String result = "";
        for(Map.Entry<String, String> item : signFieldMap.entrySet()) {
            postData.remove(item.getKey());
        }

        String postString = "";
        if("url".equals(agentProfile.getMode())) {
            postString = getSortParamsBuff(postData, false).toString();
        }

        else {
            postString += JsonUtils.toJson(postData);
        }

        Pattern pattern = Pattern.compile("\\s*(string|md5|aes|mac)\\(([^\\(\\)]+)\\)\\s*");
        Matcher m = pattern.matcher(platformInfo.getSign());
        if(m.matches()) {

            String signString = VarInfo.parseList(m.group(2), "", "postString").replace(postString);

            signString = replaceVars(signString);

            if("string".equals(m.group(1))) {
                result = signString;
            }

            else if("md5".equals(m.group(1))) {
                result = EncryptionUtil.md5(signString, platformInfo.getCharset());
            }

            else if("aes".equals(m.group(1))) {
                if(StringUtils.isEmpty(platformInfo.getKey())) {
                    throw new NoSuchAlgorithmException(LangConfig.getInstance().get("key_can_not_be_find"));
                }
                result = EncryptionUtil.encryptByAES(signString, platformInfo.getKey(), platformInfo.getCharset());
            }

            else if("mac".equals(m.group(1))) {
                if(StringUtils.isEmpty(platformInfo.getKey())) {
                    throw new NoSuchAlgorithmException(LangConfig.getInstance().get("key_can_not_be_find"));
                }
                result = EncryptionUtil.mac(signString.getBytes(platformInfo.getCharset()), platformInfo.getKey());
            }

            else if("base64".equals(m.group(1))) {
                result = EncryptionUtil.base64Encode(signString, platformInfo.getCharset());
            }

            for(String key : signFieldMap.keySet()) {
                signFieldMap.put(key, VarInfo.parseList(signFieldMap.get(key), "platform", "sign").replace(result));
            }

            return result;
        }

        throw new IOException(LangConfig.getInstance().get("wrong_to_expression") + ": " + platformInfo.getSign());
    }

    private String replaceVars(String value) {

        value = VarInfo.parseList(value, "platform", "key").replace(platformInfo.getKey());
        value = VarInfo.parseList(value, "platform", "token").replace(platformInfo.getToken());
        value = VarInfo.parseList(value, "platform", "id").replace(platformInfo.getId());
        value = VarInfo.parseList(value, "", "platform").replace(platformInfo.getPlatform());
        value = VarInfo.parseList(value, "platform", "name").replace(platformInfo.getName());
        value = VarInfo.parseList(value, "platform", "description").replace(platformInfo.getDescription());
        value = VarInfo.parseList(value, "platform", "domain").replace(platformInfo.getDomain());
        value = VarInfo.parseList(value, "platform", "charset").replace(platformInfo.getCharset());
        value = VarInfo.parseList(value, "platform", "ensureTable").replace(platformInfo.isEnsureTable().toString());
        value = VarInfo.parseList(value, "platform", "saveRequired").replace(platformInfo.isSaveRequired().toString());

        value = value.replaceAll("\\$\\{timestamp\\}", Long.valueOf(System.currentTimeMillis()).toString());
        value = value.replaceAll("\\$\\{shotTimestamp\\}", Long.valueOf(System.currentTimeMillis() / 1000).toString());

        value = value.replaceAll("\\$\\{yyyy\\-MM\\-dd HH:mm:ss.SSS\\}", Datetime.format(Datetime.now(), "yyyy-MM-dd HH:mm:ss.SSS"));
        value = value.replaceAll("\\$\\{yyyy/MM/dd HH:mm:ss.SSS\\}", Datetime.format(Datetime.now(), "yyyy/MM/dd HH:mm:ss.SSS"));
        value = value.replaceAll("\\$\\{yyyy年MM月dd日 HH时mm分ss秒SSS\\}", Datetime.format(Datetime.now(), "yyyy年MM月dd日 HH时mm分ss秒SSS"));
        value = value.replaceAll("\\$\\{dd\\-MM\\-yyyy HH:mm:ss.SSS\\}", Datetime.format(Datetime.now(), "yyyy-MM-dd HH:mm:ss.SSS"));
        value = value.replaceAll("\\$\\{dd/MM/yyyy HH:mm:ss.SSS\\}", Datetime.format(Datetime.now(), "yyyy/MM/dd HH:mm:ss.SSS"));

        value = value.replaceAll("\\$\\{yyyy\\-MM\\-dd HH:mm:ss\\}", Datetime.format(Datetime.now(), "yyyy-MM-dd HH:mm:ss"));
        value = value.replaceAll("\\$\\{yyyy/MM/dd HH:mm:ss\\}", Datetime.format(Datetime.now(), "yyyy/MM/dd HH:mm:ss.SSS"));
        value = value.replaceAll("\\$\\{yyyy年MM月dd日 HH时mm分ss秒\\}", Datetime.format(Datetime.now(), "yyyy年MM月dd日 HH时mm分ss秒"));
        value = value.replaceAll("\\$\\{dd\\-MM\\-yyyy HH:mm:ss\\}", Datetime.format(Datetime.now(), "yyyy-MM-dd HH:mm:ss"));
        value = value.replaceAll("\\$\\{dd/MM/yyyy HH:mm:ss\\}", Datetime.format(Datetime.now(), "yyyy/MM/dd HH:mm:ss"));

        value = VarInfo.parseList(value, "", "UUID").replace(UUID.randomUUID().toString());
        value = VarInfo.parseList(value, "server", "ip").replace(NetworkUtils.INTERNET_IP);
        value = VarInfo.parseList(value, "server", "port").replace(ApplicationConfig.getInstance().get("${server.port}", "80"));

        return value;
    }
}
