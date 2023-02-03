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


import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SaveDataInfo implements Serializable {
    private QueryInfo queryInfo;

    private List<Map<String, Object>> postData;

    private List<Object> removeIdList;

    public List<Object> getRemoveIdList() {
        return removeIdList;
    }

    public void setRemoveIdList(List<Object> removeIdList) {
        this.removeIdList = removeIdList;
    }

    public QueryInfo getQueryInfo() {
        return queryInfo;
    }

    public void setQueryInfo(QueryInfo queryInfo) {
        this.queryInfo = queryInfo;
    }

    public List<Map<String, Object>> getPostData() {
        return postData;
    }

    public void setPostData(List<Map<String, Object>> postData) {
        this.postData = postData;
    }
}
