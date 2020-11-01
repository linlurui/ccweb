/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.enums;

/***
 * 权限值必须从小到大排列
 */
public enum PrivilegeScope {
    DENIED(-1, "DENIED"),
    SELF(0, "SELF"),
    NO_GROUP(1, "NO_GROUP"),
    GROUP(2, "GROUP"),
    CHILD(3, "CHILD"),
    PARENT_AND_CHILD(4, "PARENT_AND_CHILD"),
    ALL(5, "ALL"),
    ;

    private int code;
    private String value;

    PrivilegeScope(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
