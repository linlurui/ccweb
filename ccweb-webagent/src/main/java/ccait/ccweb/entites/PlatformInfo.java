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

import java.util.Map;

public class PlatformInfo {

    public PlatformInfo() {
        this.charset = "UTF-8";
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDomain() {
        return domain;
    }

    public String getSign() {
        return sign;
    }

    public String getToken() {
        return token;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHeaders(Map headers) {
        this.headers = headers;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Boolean isEnsureTable() {
        return ensureTable;
    }

    public void setEnsureTable(Boolean ensureTable) {
        this.ensureTable = ensureTable;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public Boolean isSaveRequired() {
        return saveRequired;
    }

    public void setSaveRequired(Boolean saveRequired) {
        this.saveRequired = saveRequired;
    }

    public Map<String, String> getCancel() {
        return cancel;
    }

    public void setCancel(Map<String, String> cancel) {
        this.cancel = cancel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String id;
    private String key;
    private String domain;
    private String sign;
    private String token;
    private String name;
    private Map<String, Object> headers;
    private String charset;
    private boolean ensureTable;
    private String platform;
    private Map<String, Object> required;
    private boolean saveRequired;
    private String description;
    private Map<String, String> cancel;
}
