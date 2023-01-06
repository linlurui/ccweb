package ccait.ccweb.model;


import ccait.ccweb.utils.FileUtils;
import ccait.ccweb.utils.OSUtils;
import ccait.ccweb.utils.UploadUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.UUID;

public abstract class DownloadData {

    private static final Logger log = LoggerFactory.getLogger( DownloadData.class );

    protected final String datasourceId;
    protected final String table;
    protected Integer index;
    protected final String field;
    protected final String id;
    protected String[] arrMessage;
    protected byte[] buffer;
    protected MediaType mediaType;
    protected String path;
    protected String tempFilePath;
    protected int page;

    public DownloadData(String datasourceId, String table, String field, String id) {
        this.table = table;
        this.field = field;
        this.id = id;
        this.datasourceId = datasourceId;
        this.preview = false;
        this.page = 0;
        this.index = 0;
    }

    public DownloadData(String datasourceId, String table, String field, String id, Integer index) {
        this.table = table;
        this.field = field;
        this.id = id;
        this.datasourceId = datasourceId;
        this.preview = false;
        this.page = 0;
        this.index = index;
    }

    public DownloadData(String datasourceId, String table, String field, String id, int page) {
        this.table = table;
        this.field = field;
        this.id = id;
        this.datasourceId = datasourceId;
        this.preview = true;
        this.page = page;
        this.index = 0;
    }

    public DownloadData(String datasourceId, String table, String field, String id, Integer index, int page) {
        this.table = table;
        this.field = field;
        this.id = id;
        this.datasourceId = datasourceId;
        this.preview = true;
        this.page = page;
        this.index = index;
    }

    public String getPath() {
        return path;
    }

    public boolean isPreview() {
        return preview;
    }

    protected boolean preview;

    public int getPage() {
        return page;
    }

    public String getFilename() {
        return arrMessage[2];
    }

    public String getExtension() {
        return arrMessage[1];
    }

    public String getMimeType() {
        return arrMessage[0];
    }

    public void setMimeType(String mimeType) {
        arrMessage[0] = mimeType;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public abstract DownloadData invoke() throws Exception;

    public void saveTempFile(byte[] buffer) {
        if(this.preview && mediaType.getType().equalsIgnoreCase("video")){
            String filename = UUID.randomUUID().toString().replace("-", "");
            String dir = String.format("%s/temp", System.getProperty("user.dir"));
            this.tempFilePath = dir + "/" + filename;
            FileUtils.save(buffer, dir, filename);
        }
    }

    public String getFullPath(String content, Map<String, Object> uploadConfigMap) {
        arrMessage = new String[3];
        String[] tmp = content.split("/");
        String[] fileArr = tmp[tmp.length -1].split("\\.");
        arrMessage[0] = UploadUtils.getMIMEType(fileArr[1]);
        arrMessage[1] = fileArr[1];
        arrMessage[2] = fileArr[0];
        String[] arr = arrMessage[0].split("/");
        mediaType = new MediaType(arr[0], arr[1]);

        String root = uploadConfigMap.get("path").toString();
        log.info("upload config path===>" + root);
        if(root.lastIndexOf("/") == root.length() - 1 ||
                root.lastIndexOf("\\") == root.length() - 1) {
            root = root.substring(0, root.length() - 2);
        }
        if("/".equals(content.substring(0,1))) {
            content = content.substring(1);
        }

        log.info("download root===>" + root);
        String filePath = String.format("%s/%s", root, content);
        log.info("download file path===>" + filePath);

        if(OSUtils.isWindows()) {
            filePath = System.getProperty("user.dir") + filePath;
            filePath = filePath.replaceAll("/", "\\\\");
        }

        return filePath;
    }

    public void cleanTempFile() {

        if(StringUtils.isEmpty(this.tempFilePath)) {
            return;
        }
        FileUtils.delete(this.tempFilePath);
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
