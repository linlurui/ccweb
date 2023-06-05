/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.resolver;


import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.model.UploadFileInfo;
import entity.tool.util.FastJsonUtils;
import entity.tool.util.JsonUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Configuration
public class RequestBodyResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger( RequestBodyResolver.class );

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        CCWebRequestWrapper requestWrapper = (CCWebRequestWrapper) ((ServletWebRequest) webRequest).getRequest();
        try {
            if(requestWrapper.getParameters() == null) {
                return null;
            }
            final Type type = parameter.getGenericParameterType();
            if(type.equals(String.class)) {
                if(requestWrapper.getParameters().getClass().equals(String.class)) {
                    return requestWrapper.getParameters();
                }
                return JsonUtils.toJson(requestWrapper.getParameters());
            }

            Class clazz = null;
            if (requestWrapper.getUploadFileMap().size() > 0) {
                Map<String, Object> parameters = (Map<String, Object>) requestWrapper.getParameters();
                for (Map.Entry<String, UploadFileInfo> item : requestWrapper.getUploadFileMap().entrySet()) {
                    parameters.put(item.getKey(), item.getValue());
                }

                return parameters;
            }

            if (type.getTypeName().startsWith("java.util.List<")) {
                List parametes = (List) requestWrapper.getParameters();
                List result = new ArrayList();
                for (int i = 0; i < parametes.size(); i++) {
                    Type argType = ((ParameterizedTypeImpl) type).getActualTypeArguments()[0];
                    if (byte[].class.equals(argType) || String.class.equals(argType)) {
                        result.add(parametes.get(i));
                    }
                    else if (parametes.get(i) instanceof Number) {
                        result.add(parametes.get(i));
                    }
                    else {
                        Object obj = JsonUtils.convert(parametes.get(i), getClassByType(argType));
                        result.add(obj);
                    }
                }

                return result;
            } else {

                clazz = getClassByType(type);
            }

            if(requestWrapper.getParameters() == null) {
                return null;
            }

            try {
                return JsonUtils.convert(requestWrapper.getParameters(), clazz);
            }
            catch (Exception ex) {
                log.error(ex.getMessage());
                return FastJsonUtils.convert(requestWrapper.getParameters(), clazz);
            }
        }
        catch (Exception e) {
            log.error("RequestBody参数化失败=====>>> ", e);
        }

        return requestWrapper.getParameters().toString();
    }

    public Class getClassByType(Type type) {
        Class clazz;
        if(type.getTypeName().startsWith("java.util.Map<")) {
            clazz = Map.class;
        }
        else {
            clazz = (Class) type;
        }
        return clazz;
    }
}
