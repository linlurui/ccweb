package ccait.ccweb.trigger;


import ccait.ccweb.annotation.Trigger;
import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.entites.QueryInfo;
import ccait.ccweb.model.DownloadData;
import ccait.ccweb.model.ResponseData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Scope("prototype")
@Trigger(tablename = "${entity.table.group}")
public final class GroupTableTrigger implements ITrigger {

    private static final Logger log = LogManager.getLogger( GroupTableTrigger.class );

    @Value("${entity.table.reservedField.groupId:groupId}")
    private String groupIdField;

    @Override
    public void onInsert(List<Map<String, Object>> list, HttpServletRequest request) {
        for(Map<String, Object> data : list) {
            data.put(groupIdField, UUID.randomUUID().toString().replace("-", ""));
        }
        CCWebRequestWrapper wrapper = (CCWebRequestWrapper) request;
        wrapper.setPostParameter(list);
    }

    @Override
    public void onUpdate(QueryInfo queryInfo, HttpServletRequest request) {

        Map<String, Object> data = queryInfo.getData();
        if(data.containsKey(groupIdField)) {
            data.remove(groupIdField);
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
    public void onResponse(HttpServletResponse response, HttpServletRequest request) {
    }

    @Override
    public void onSuccess(ResponseData responseData, HttpServletRequest request) {
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
}
