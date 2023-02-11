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


import ccait.ccweb.annotation.OnSave;
import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.config.LangConfig;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.entites.SaveDataInfo;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.DownloadData;
import ccait.ccweb.model.ResponseData;
import entity.query.ColumnInfo;
import entity.query.core.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Component
@Scope("prototype")
@Trigger(tablename = "${ccweb.table.group}")
@Order(Ordered.HIGHEST_PRECEDENCE+666)
public final class GroupTableTrigger implements ITrigger {

    private static final Logger log = LoggerFactory.getLogger( GroupTableTrigger.class );

    @Value("${ccweb.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @PostConstruct
    private void init() {
        groupIdField = ApplicationConfig.getInstance().get("${ccweb.table.reservedField.groupId}", groupIdField);
        log.info("init trigger  =================================> " + this.getClass().getSimpleName());
    }

    @OnSave
    public void onSave(SaveDataInfo saveDataInfo, HttpServletRequest request) throws Exception {
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(saveDataInfo);
    }

    @Override
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception {
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        for(Map<String, Object> data : list) {
            if(data.containsKey(groupIdField) && data.containsKey("parentId")) {
                if(data.get(groupIdField)!=null && data.get("parentId")!=null) {
                    if(data.get(groupIdField).toString().equals(data.get("parentId").toString())) {
                        throw new Exception(LangConfig.getInstance().get("parent_can_not_be_equals_self"));
                    }
                }
                if(data.get("parentId")==null) {
                    data.put("parentId", 0);
                }
            }
        }
        wrapper.setPostParameter(list);
    }

    @Override
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception {

        Map<String, Object> data = queryInfo.getData();
        if(data.containsKey(groupIdField) && data.containsKey("parentId")) {
            if(data.get(groupIdField)!=null && data.get("parentId")!=null) {
                if(data.get(groupIdField).toString().equals(data.get("parentId").toString())) {
                    throw new Exception(LangConfig.getInstance().get("parent_can_not_be_equals_self"));
                }
            }
            if(data.get("parentId")==null) {
                data.put("parentId", 0);
            }
        }
        if(data.containsKey(groupIdField)) {
            data.remove(groupIdField);
        }
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        String[] arr = request.getRequestURI().split("/");
        if("update".equals(arr[arr.length - 1].toLowerCase()) || "save".equals(arr[arr.length - 1].toLowerCase())) {
            wrapper.setPostParameter(queryInfo);
        }

        else {
            wrapper.setPostParameter(data);
        }
    }

    @Override
    public void onDelete(String id, HttpServletRequest request) {
    }

    @Override
    public void onList(QueryInfo queryInfo, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onView(String id, HttpServletRequest request) {}

    @Override
    public void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {
    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception {
    }

    @Override
    public void onError(Exception ex, HttpServletRequest request) {
    }

    @Override
    public void onUpload(byte[] data, HttpServletRequest request) {
    }

    @Override
    public void onDownload(DownloadData data, HttpServletRequest request) {
    }

    @Override
    public void onPreviewDoc(DownloadData data, HttpServletRequest request) {
    }

    @Override
    public void onPlayVideo(DownloadData data, HttpServletRequest request) {

    }

    @Override
    public void onBuild(List<ColumnInfo> columns, HttpServletRequest request) throws Exception {
        throw new Exception("can not build group table!!!");
    }
}
