package ccait.ccweb.abstracts;

import ccait.ccweb.context.ApplicationContext;
import ccait.ccweb.context.TriggerContext;
import ccait.ccweb.enums.EventType;
import ccait.ccweb.model.PageInfo;
import ccait.ccweb.model.ResponseData;
import ccait.ccweb.model.UserModel;
import ccait.ccweb.utils.EncryptionUtil;
import entity.query.Datetime;
import entity.query.core.ApplicationConfig;
import entity.tool.util.FastJsonUtils;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static ccait.ccweb.utils.StaticVars.*;
import static ccait.ccweb.utils.StaticVars.CURRENT_TABLE;

public abstract class AbstractBaseController {

    @Autowired
    protected HttpServletRequest request;

    @Autowired
    protected HttpServletResponse response;

    private static final Logger log = LoggerFactory.getLogger(AbstractBaseController.class);

    protected <T> void setSession(String key, T data) {
        ApplicationContext.setSession( request, key, data );
    }

    protected <T> T getSession(String key, Class<T> clazz) throws IOException {
        return (T) ApplicationContext.getSession(request, key, clazz);
    }

    @SuppressWarnings( "unchecked" )
    protected UserModel getLoginUser() throws IOException {
        return ApplicationContext.getSession( request, LOGIN_KEY, UserModel.class );
    }


    protected Mono<ResponseData> successAs() {
        return Mono.just(success());
    }

    protected <T> Mono<ResponseData<T>> successAs(T data) {
        return Mono.just(success(data));
    }

    protected <T> Mono<ResponseData<T>> successAs(T data, PageInfo pageInfo) {
        return Mono.just(success(data, pageInfo));
    }

    protected Mono<ResponseData> errorAs(String message) {
        return Mono.just(error(message));
    }

    protected Mono<ResponseData> errorAs(int code, Exception e) {

        return Mono.just(error(code, e));
    }

    protected Mono<ResponseData> errorAs(int code, String message) {
        return Mono.just(error(code, message));
    }

    protected ResponseData<String> success() {
        return this.result(0, "OK", null, null);
    }

    protected <T> ResponseData<T> success(T data) {
        return this.result(0, "OK", data, null);
    }

    protected ResponseData<String> message(String text) {
        return this.result(0, text, null, null);
    }

    protected <T> ResponseData<T> success(T data, PageInfo pageInfo) {
        return this.result(0, "OK", data, pageInfo);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseData error(String message) {
        return this.result(-1, message, "", null);
    }

    protected ResponseData error(int code, Exception e) {

        if(e instanceof HttpException) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        if(StringUtils.isEmpty(e.getMessage())) {
            return this.result(code, e.toString(), "", null);
        }
        return this.result(code, e.getMessage(), "", null);
    }

    protected ResponseData error(int code, String message) {
        return this.result(code, message, "", null);
    }

    public <T> ResponseData<T> result(int code, String message, T data, PageInfo pageInfo) {
        ResponseData<T> result = new ResponseData<T>();

        result.setStatus(code);
        result.setMessage(message);
        result.setData(data);
        result.setPageInfo(pageInfo);

        try {
            result = handleResultEvent(result);
        } catch (InvocationTargetException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }

        response.addHeader("finish", "yes");

        return result;
    }

    public static String getTablename() {
        Map map = ApplicationContext.getThreadLocalMap();
        if(!map.containsKey(CURRENT_TABLE)) {
            return "";
        }

        return map.get(CURRENT_TABLE).toString();
    }

    private <T> ResponseData<T> handleResultEvent(ResponseData<T> result) throws InvocationTargetException, IllegalAccessException {

        String tablename = getTablename();

        try {
            result.setUuid(UUID.randomUUID());

            if(result.getStatus() != 0 || response.getStatus() != 200) {
                errorTrigger(result.getMessage(), tablename);
            }

            else {
                successTrigger(result, tablename);
            }

            if(result.getData() == null || hasPrimitive(result.getData())) {
                return result;
            }

            //格式化输出数据
            if(result.getData() instanceof String || result.getData() instanceof Number) {
                result.setData(result.getData());
            }

            else {
                result.setData(getFormatedData(tablename, result.getData())); //set format data
            }

        } catch (Throwable throwable) {
            log.error(LOG_PRE_SUFFIX + throwable.getMessage(), throwable);
            result.setMessage(throwable.getMessage());
            errorTrigger(throwable.getMessage(), tablename);
        }
        finally {
            TriggerContext.exec(tablename, EventType.Response, response, request);
        }

        return result;
    }

    private void successTrigger(ResponseData result, String tablename) throws InvocationTargetException, IllegalAccessException {
        TriggerContext.exec(tablename, EventType.Success, result, request);
    }

    private void errorTrigger(String message, String tablename) {
        if(StringUtils.isEmpty(message)) {
            message = "request error!!!";
        }

        try {
            TriggerContext.exec(tablename, EventType.Error, new Exception(message), request);
        } catch (InvocationTargetException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(LOG_PRE_SUFFIX + e.getMessage(), e);
        }
    }

    protected <T> T getFormatedData(String tablename, T data) throws IOException {

        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        boolean needReset = false;
        boolean returnList = false;
        Map<String, Object> map = ApplicationConfig.getInstance().getMap("ccweb.formatter");
        if(map != null) {

            if(data == null) {
                return null;
            }

            else if(data instanceof String && StringUtils.isEmpty(data.toString())) {
                return null;
            }

            else if(data instanceof Map) {
                dataList.add((Map<String, Object>) data);
            }

            else if(data instanceof List) {
                dataList = (List<Map<String, Object>>) data;
                returnList = true;
            }

            else {
                dataList.add(FastJsonUtils.convert(data, Map.class));
            }

            try {
                for (Object obj : dataList) {
                    Map<String, Object> item = JsonUtils.convert(obj, Map.class);
                    for (String key : item.keySet()) {
                        Optional opt = map.keySet().stream()
                                .filter(a -> a.equals(key) ||
                                        String.format("%s.%s", tablename, key).equals(a))
                                .findAny();

                        if (opt.isPresent() && item.get(key) != null) {
                            if (item.get(key) instanceof Date) {
                                item.put(key, Datetime.format((Date) item.get(key), opt.get().toString()));
                                needReset = true;
                            } else if (item.get(key) instanceof Long || item.get(key).getClass().equals(long.class)) {
                                item.put(key, Datetime.format((Date) StringUtils.cast(Date.class, item.get(key).toString()),
                                        map.get(opt.get()).toString()));

                                needReset = true;
                            } else if (item.get(key) instanceof Map && ((Map) item.get(key)).containsKey("time") &&
                                    (((Map) item.get(key)).get("time") instanceof Long || ((Map) item.get(key)).get("time").getClass().equals(long.class))) {
                                item.put(key, Datetime.format(new Date((Long) ((Map) item.get(key)).get("time")),
                                        map.get(opt.get()).toString()));

                                needReset = true;
                            } else if (hasPrimitive(item.get(key))) {
                                item.put(key, String.format(opt.get().toString(), item.get(key)));
                                needReset = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        if(needReset) {

            if(returnList) {
                return (T) dataList;
            }

            if(dataList.size() != 1) {
                return (T) dataList;
            }

            return (T) dataList.get(0);
        }

        return data;
    }

    protected String base64Encode(String data) throws Exception {
        return EncryptionUtil.base64Encode(data, ApplicationConfig.getInstance().get("${ccweb.encoding}", "UTF-8"));
    }

    protected String base64EncodeSafe(String data) throws Exception {
        return EncryptionUtil.base64EncodeSafe(data, ApplicationConfig.getInstance().get("${ccweb.encoding}", "UTF-8"));
    }

    protected String base64Decode(String data) throws Exception {
        return EncryptionUtil.base64Decode(data, ApplicationConfig.getInstance().get("${ccweb.encoding}", "UTF-8"));
    }

    public static boolean hasPrimitive(Object obj) {
        try {
            return ((Class<?>) obj.getClass().getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    protected String md5(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        return EncryptionUtil.md5(str, ApplicationConfig.getInstance().get("${ccweb.security.encrypt.MD5.publicKey}", "ccait"),
                ApplicationConfig.getInstance().get("${ccweb.encoding}", "UTF-8"));
    }

    public String getRequestPostString() throws IOException {
        return getRequestPostString(request);
    }

    public static String getRequestPostString(HttpServletRequest request)
            throws IOException {
        String charSetStr = request.getCharacterEncoding();
        if (charSetStr == null) {
            charSetStr = "UTF-8";
        }

        Charset charSet = Charset.forName(charSetStr);

        return StreamUtils.copyToString(request.getInputStream(), charSet);
    }

}
