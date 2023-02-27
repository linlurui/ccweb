/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.filter;

import ccait.ccweb.model.UploadFileInfo;
import ccait.ccweb.utils.UploadUtils;
import entity.query.core.ApplicationConfig;
import entity.tool.util.FastJsonUtils;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CCWebRequestWrapper extends HttpServletRequestWrapper implements MultipartHttpServletRequest {

    private byte[] requestBody;
    private static Charset charSet;
    private String postString;
    private HttpServletRequest req;
    private Boolean multipleUpload;
    private Map<String, UploadFileInfo> uploadFileMap;
    private static final String ISO_8859_1 = "ISO-8859-1";
    private static final Logger log = LoggerFactory.getLogger(CCWebRequestWrapper.class);

    public CCWebRequestWrapper(HttpServletRequest request, Map newParams)
    {
        super(request);
        this.params = newParams;
        this.postString = FastJsonUtils.toJson(newParams);
        uploadFileMap = new HashMap<String, UploadFileInfo>();
        multipleUpload = ApplicationConfig.getInstance().get("${ccweb.upload.multiple}", true);
    }

    public CCWebRequestWrapper(HttpServletRequest request) {
        super(request);
        req = request;
        uploadFileMap = new HashMap<String, UploadFileInfo>();
        multipleUpload = ApplicationConfig.getInstance().get("${ccweb.upload.multiple}", true);

        //缓存请求body
        try {
            requestBody = readBody(request);
            if(requestBody == null) {
                requestBody = new byte[0];
            }

            log.info("requestBody length === > " + requestBody.length);
            Map<String, Object> map = new HashMap<String, Object>();

            String regexp = "(\\-{6}\\-*[\\d\\w]{6}[\\d\\w]*|--[\\-\\d\\w]{36}|--\\w+\\+[\\d\\w]{16})";
            String spliter = UploadUtils.match(requestBody, 0, regexp, requestBody.length, ISO_8859_1);
            if(StringUtils.isNotEmpty(spliter)) {
                List<ByteBuffer> files = multipleUpload ? UploadUtils.split(requestBody, spliter, ISO_8859_1) :
                        UploadUtils.trim(requestBody, spliter, ISO_8859_1);

                for (ByteBuffer buffer : files) {

                    try {
                        byte[] bytes = UploadUtils.getBytes(buffer);
                        regexp = "Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"((;\\s*filename=\"([^;]+)\")\\s*((;\\s*filename\\*=[^;\\s]+)\\s*(Content-Type:\\s*([^/]+/[^\\s]+)\\s*)|\\s*(Content-Type:\\s*([^/]+/[^\\s]+)\\s*)))?";
                        String summary = UploadUtils.match(bytes, 0, regexp, bytes.length, ISO_8859_1);
                        if (StringUtils.isEmpty(summary)) {
                            break;
                        }
                        if (summary.indexOf("Content-Type") == -1) {
                            summary += UploadUtils.match(bytes, summary.length(), "(;?\\s*Content-Type:\\s*([^/]+/[^\\s]+)\\s*)", 1024, ISO_8859_1);
                        }
                        if (summary.indexOf("Content-Length") == -1) {
                            summary += UploadUtils.match(bytes, summary.length(), "[\\r\\n]*Content-Length:\\s?\\d+[\\r\\n]{2}", 1024, ISO_8859_1);
                        }
                        bytes = UploadUtils.getBytes(ByteBuffer.wrap(bytes, summary.length(), bytes.length - summary.length()));
                        buffer = UploadUtils.trimEnter(bytes); //去掉两边回车
                        Matcher m = Pattern.compile(regexp).matcher(summary);
                        while (m.find()) {
                            try {
                                String key = new String(m.group(1).getBytes(ISO_8859_1), "UTF-8");
                                Object value = null;

                                if (m.group(10) != null && Pattern.matches("[^/]+/.+", m.group(10))) {
                                    saveToUploadFileInfo(buffer, m, key, 10);
                                } else if (m.group(8) != null && Pattern.matches("[^/]+/.+", m.group(8))) {
                                    saveToUploadFileInfo(buffer, m, key, 8);
                                } else {
                                    //不是文件要改回UTF-8编码中文才不会乱码
                                    value = new String(UploadUtils.getBytes(buffer), "UTF-8");
                                }

                                if(value == null) {
                                    map.put(key, null);
                                }
                                else {
                                    map.put(key, ((String) value).trim());
                                }

                            } catch (IndexOutOfBoundsException ioEx) {
                                break;
                            }
                        }
                    }
                    finally {
                        buffer.clear();
                        buffer = null;
                    }
                }
            }

            if(map.size() > 0) {
                this.params = map;
            }
            else {
                String postString = new String(requestBody, "UTF-8");
                if(StringUtils.isEmpty(postString)) {
                    return;
                }

                try {
                    if (Pattern.matches("^\\s*\\[[\\w\\W]+\\]\\s*$", postString)) {
                        this.params = JsonUtils.parse(postString, List.class);
                    } else {
                        this.params = JsonUtils.parse(postString, Map.class);
                    }
                }
                catch (Exception e) {
                    log.warn(e.getMessage());
                    this.params = postString;
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void saveToUploadFileInfo(ByteBuffer buffer, Matcher m, String key, int i) throws UnsupportedEncodingException {
        UploadFileInfo fileInfo = new UploadFileInfo();
        fileInfo.setBuffer(ByteBuffer.wrap(UploadUtils.getBytes(buffer)));
        fileInfo.setContentType(m.group(i));
        fileInfo.setFieldName(m.group(1));
        fileInfo.setFilename(new String(m.group(4).getBytes(ISO_8859_1), "UTF-8"));
        uploadFileMap.put(key, fileInfo);
    }

    private Object params;

    public Object getParameters()
    {
        return params;
    }

    public  String getRequestPostString()
    {
        return postString;
    }

    public Map<String, UploadFileInfo> getUploadFileMap() {
        return uploadFileMap;
    }

    public static String getRequestPostString(HttpServletRequest request)
            throws IOException {
        String charSetStr = request.getCharacterEncoding();
        if (charSetStr == null) {
            charSetStr = "UTF-8";
        }
        charSet = Charset.forName(charSetStr);

        return StreamUtils.copyToString(request.getInputStream(), charSet);
    }

    /**
     * 重写 getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() {
        if (requestBody == null) {
            requestBody = new byte[0];
        }

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    /**
     * 重写 getReader()
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public void setPostParameter(Object parameter) {
        this.params = parameter;
        this.postString = FastJsonUtils.toJson(parameter);
    }

    @Override
    public HttpMethod getRequestMethod() {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getRequestMethod();
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getRequestMethod();
        }
        switch (req.getMethod()) {
            case "DELETE":
                return HttpMethod.DELETE;
            case "PUT":
                return HttpMethod.PUT;
            case "PATCH":
                return HttpMethod.PATCH;
            case "OPTIONS":
                return HttpMethod.OPTIONS;
            default:
                return HttpMethod.POST;
        }
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getRequestHeaders();
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getRequestHeaders();
        }
        HttpHeaders headers = new HttpHeaders();
        String name = req.getHeaderNames().nextElement();
        while (!StringUtils.isEmpty(name)) {
            headers.add(name, req.getHeader(name));
        }
        return headers;
    }

    @Override
    public HttpHeaders getMultipartHeaders(String s) {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getMultipartHeaders(s);
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getMultipartHeaders(s);
        }
        return new HttpHeaders();
    }

    @Override
    public Iterator<String> getFileNames() {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getFileNames();
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getFileNames();
        }
        return null;
    }

    @Override
    public MultipartFile getFile(String s) {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getFile(s);
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getFile(s);
        }
        return null;
    }

    @Override
    public List<MultipartFile> getFiles(String s) {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getFiles(s);
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getFiles(s);
        }
        return new ArrayList<>();
    }

    @Override
    public Map<String, MultipartFile> getFileMap() {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getFileMap();
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getFileMap();
        }
        return new LinkedMultiValueMap();
    }

    @Override
    public MultiValueMap<String, MultipartFile> getMultiFileMap() {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getMultiFileMap();
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getMultiFileMap();
        }
        return new LinkedMultiValueMap<>();
    }

    @Override
    public String getMultipartContentType(String s) {
        if(req instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest)req).getMultipartContentType(s);
        }
        if(req instanceof CCWebRequestWrapper) {
            return ((CCWebRequestWrapper)req).getMultipartContentType(s);
        }
        return null;
    }

    public static byte[] readBody(HttpServletRequest request)
            throws IOException{
        int formDataLength = request.getContentLength();
        if(formDataLength < 1) {
            return null;
        }
        DataInputStream dataStream = new DataInputStream(request.getInputStream());
        byte body[] = new byte[formDataLength];
        int totalBytes = 0;
        while (totalBytes < formDataLength) {
            int len = dataStream.read(body, totalBytes, formDataLength);
            if(len > -1) {
                totalBytes += len;
            }
        }
        return body;
    }
}
