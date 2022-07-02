/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2020-11-09
 */

package ccait.ccweb.entites;

import java.util.List;
import java.util.Map;
import entity.tool.util.StringUtils;

public class AgentProfile {

    public AgentProfile(){}
    public AgentProfile(Map param) {
        if(param.containsKey("method") && param.get("method") != null) {
            this.method = param.get("method").toString();
        }

        if(param.containsKey("mode") && param.get("mode") != null) {
            this.mode = param.get("mode").toString();
        }

        if(param.containsKey("path") && param.get("path") != null) {
            this.path = param.get("path").toString();
        }

        if(param.containsKey("save") && param.get("save") != null) {
            this.save = param.get("save").toString();
        }

        if(param.containsKey("data") && param.get("data") != null) {
            this.data = param.get("data").toString();
        }

        if(param.containsKey("primaryKey") && param.get("primaryKey") != null) {
            this.primaryKey = param.get("primaryKey").toString();
        }

        if(param.containsKey("success") && param.get("success") != null) {
            this.success = (Map)param.get("success");
        }

        if(param.containsKey("inMap") && param.get("inMap") != null) {
            this.inMap = (Map)param.get("inMap");
        }

        if(param.containsKey("outMap") && param.get("outMap") != null) {
            this.outMap = (Map)param.get("outMap");
        }

        if(param.containsKey("required") && param.get("required") != null) {
            this.required = (Map)param.get("required");
        }

        if(param.containsKey("fixedColumns") && param.get("fixedColumns") != null) {
            this.fixedColumns = (Map)param.get("fixedColumns");
        }

        if(param.containsKey("unique") && param.get("unique") != null) {
            this.uniqueList = StringUtils.splitString2List(param.get("unique").toString(), ",");
        }

        if(param.containsKey("roleId") && param.get("roleId") != null) {
            this.roleId = Integer.parseInt(param.get("roleId").toString());
        }

        if(param.containsKey("callback") && param.get("callback") != null) {
            this.callback = (Map)param.get("callback");
        }

        if(param.containsKey("checkDomain") && param.get("checkDomain") != null) {
            this.checkDomain = Boolean.parseBoolean(param.get("checkDomain").toString());
        }
        else {
            this.checkDomain = true;
        }

        if(param.containsKey("response") && param.get("response") != null) {
            this.response = param.get("response").toString();
        }
    }

    /**
     * 提交参数的方法
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * 参数拼接方式
     * @return
     */
    public String getMode() {
        return mode;
    }

    /**
     * 路径
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * 需保存到表的数据，result＝第三方平台返的响应结果。
     * post＝提交的参数，both=两种数据都存，同名的会被result覆盖, dont=不保存
     * @return
     */
    public String getSave() {
        return save;
    }

    /**
     * 要存到数据库的json数据集位置，不填默认为返回的整个json结果集
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     * 检查返回成功的键值对
     * @return
     */
    public Map<String, String> getSuccess() {
        return success;
    }

    /**
     * 提交到第三方平台的参数映射
     * （入参映射）
     * @return
     */
    public Map<String, String> getInMap() {
        return inMap;
    }

    /**
     * 出参到数据库字段名的映射
     * （出参映射）
     * @return
     */
    public Map<String, String> getOutMap() {
        return outMap;
    }

    /**
     * UPDATE时的主键
     * @return
     */
    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setIsPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * 获取必填的提交参数
     * @return
     */
    public Map<String, Object> getRequired() {
        return required;
    }

    public void setRequired(Map<String, Object> required) {
        this.required = required;
    }


    /**
     * 获取保存到数据库的固定列，值可以是变量
     * @return
     */
    public Map<String, String> getFixedColumns() {
        return fixedColumns;
    }

    public void setFixedColumns(Map<String, String> fixedColumns) {
        this.fixedColumns = fixedColumns;
    }

    public List<String> getUniqueList() {
        return uniqueList;
    }

    public void setUniqueList(List<String> uniqueList) {
        this.uniqueList = uniqueList;
    }

    /**
     * 获取角色ID，当插入数据到用户表才有效
     * @return
     */
    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    /**
     * 获取回调配置
     * @return
     */
    public Map<String, String> getCallback() {
        return callback;
    }

    public void setCallback(Map<String, String> callback) {
        this.callback = callback;
    }

    public boolean isCheckDomain() {
        return checkDomain;
    }

    public void setCheckDomain(boolean checkDomain) {
        this.checkDomain = checkDomain;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    private String method;
    private String mode;
    private String path;
    private String save;
    private String data;
    private Map<String, String> success;
    private Map<String, String> inMap;
    private Map<String, String> outMap;
    private String primaryKey;
    private Map<String, Object> required;
    private Map<String, String> fixedColumns;
    private List<String> uniqueList;
    private Integer roleId;
    private Map<String, String> callback;
    private boolean checkDomain;
    private String response;
}
