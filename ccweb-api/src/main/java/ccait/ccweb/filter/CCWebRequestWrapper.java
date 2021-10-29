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

import entity.tool.util.FastJsonUtils;
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
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CCWebRequestWrapper extends HttpServletRequestWrapper implements MultipartHttpServletRequest {

    private byte[] requestBody;
    private static Charset charSet;
    private String postString;
    private HttpServletRequest req;

    private static final Logger log = LoggerFactory.getLogger(CCWebRequestWrapper.class);

    public CCWebRequestWrapper(HttpServletRequest request, Map newParams)
    {
        super(request);
        this.params = newParams;
        this.postString = FastJsonUtils.toJson(newParams);
    }

    public CCWebRequestWrapper(HttpServletRequest request) {
        super(request);
        req = request;

        //缓存请求body
        try {
            requestBody = readBody(request);
            if(requestBody == null) {
                requestBody = new byte[0];
            }

            postString = new String(requestBody, "ISO-8859-1"); //上传文件的编码要用ISO-8859-1格式才不会变
            Map<String, Object> map = new HashMap<String, Object>();
            List<String> list = StringUtils.splitString2List(postString, "\\-{6}\\-*[\\d\\w]{6}[\\d\\w]*");
            Pattern regex = Pattern.compile("Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"(;\\s*filename=\"([^\"]+)\")?\\s*(Content-Type:\\s*([^/]+/[^\\s]+)\\s*)?", Pattern.CASE_INSENSITIVE);
            if(list.size() == 1 && (request.getRequestURI().endsWith("/upload") || request.getRequestURI().endsWith("/import"))) {
                byte[] bytes = requestBody;
                map.put("temp_upload_filename", "application/octet-stream"); //contentType
                map.put("temp", bytes);
            }
            for(String content : list) {
                if(list.size() == 1) {
                    break;
                }
                Matcher m = regex.matcher(content);
                while (m.find()) {
                    String key = new String(m.group(1).getBytes("ISO-8859-1"), "UTF-8");
                    Object value = content.substring(m.group(0).length());

                    if (m.group(5) != null && Pattern.matches("[^/]+/.+", m.group(5))) {

                        //返回字节数组，fastjson序列化时会进行Base64编码
                        value = value.toString().getBytes("ISO-8859-1");
                        map.put(String.format("%s_upload_filename", key), new String(m.group(3).getBytes("ISO-8859-1"), "UTF-8"));
                    } else {
                        //不是文件要改回UTF-8编码中文才不会乱码
                        value = new String(m.group(6).getBytes("ISO-8859-1"), "UTF-8");
                    }

                    map.put(key, value);
                }
            }

            if(map.size() > 0) {
                postString = FastJsonUtils.toJson(map);
                this.params = map;
            }
            else {
                postString = new String(requestBody, "UTF-8");
                if(Pattern.matches("\\s*^\\[[^\\[\\]]+\\]$\\s*", postString)) {
                    this.params = FastJsonUtils.convert(postString, List.class);
                }
                else {
                    this.params = FastJsonUtils.convert(postString, Map.class);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
