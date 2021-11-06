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


import ccait.ccweb.config.EnumConvertFactory;
import entity.query.Datetime;
import entity.query.Queryable;
import entity.query.annotation.AutoIncrement;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.annotation.Tablename;
import entity.query.core.ApplicationConfig;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Configuration
public class PathVariableResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger( PathVariableResolver.class );

    @Autowired
    private EnumConvertFactory enumConvertFactory;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if(Pattern.matches("^arg(\\d+)$", parameter.getParameterName())) {
            return false;
        }
        return parameter.hasParameterAnnotation(PathVariable.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object result = null;
        try {
            Map attrMap = (Map) ((ServletWebRequest) webRequest).getRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            String name = parameter.getParameterName();
            if(parameter.getParameterType().isEnum()) {
                if(!attrMap.containsKey(name)) {
                    return result;
                }
                result = enumConvertFactory.getConverter(parameter.getParameterType()).convert(attrMap.get(name).toString());
                return result;
            }

            else if(parameter.getParameterType().equals(String.class)){
                if(attrMap.containsKey(name)) {
                    result = attrMap.get(name);
                }

                return result;
            }

            else if(parameter.getParameterType().isPrimitive() ||
                    (parameter.getParameterType().getSuperclass()!=null && parameter.getParameterType().getSuperclass().equals(Number.class))){
                if(attrMap.containsKey(name)) {
                    result = StringUtils.cast(parameter.getParameterType(), String.valueOf(attrMap.get(name)));
                }

                return result;
            }

            else if(parameter.getParameterType().equals(Date.class)) {
                if(attrMap.containsKey(name)) {
                    result = Datetime.parse(String.valueOf(attrMap.get(name)));
                }

                return result;
            }

            else if(parameter.getParameterType().getSuperclass() != null) {
                if(parameter.getParameterType().getSuperclass().isAssignableFrom(Queryable.class)) {
                    Tablename ann = parameter.getParameterType().getAnnotation(Tablename.class);
                    List<String> keyset = (List<String>) attrMap.keySet().stream().map(a->a.toString()).collect(Collectors.toList());
                    name = keyset.get(parameter.getParameterIndex());
                    Field field = parameter.getParameterType().getDeclaredField(name);
                    List<String> splitList = StringUtils.splitString2List(parameter.getParameterType().getName(), "\\.");
                    String table = splitList.get(splitList.size() - 1);
                    if(ann != null && StringUtils.isNotEmpty(ann.value())) {
                        table = ApplicationConfig.getInstance().get(ann.value());
                    }

                    if(StringUtils.isNotEmpty(table) && field!=null &&
                            (field.getAnnotation(PrimaryKey.class)!=null || field.getAnnotation(AutoIncrement.class) != null)) {
                        String fieldName = name;
                        Object entity = JsonUtils.convert(attrMap, parameter.getParameterType());
                        Fieldname annFieldname = field.getAnnotation(Fieldname.class);
                        if(annFieldname != null) {
                            fieldName = ApplicationConfig.getInstance().get(annFieldname.value());
                        }

                        Queryable queryable = (Queryable) entity;

                        result = queryable.where(fieldName + "=#{"  + name + "}").first();

                        return result;
                    }
                }

                result = JsonUtils.convert(attrMap.get(name), parameter.getParameterType());

                return result;
            }

            if(attrMap.containsKey(name)) {
                result = StringUtils.cast(parameter.getParameterType(), String.valueOf(attrMap.get(name)));
            }

            return result;
        }
        catch (Exception e) {
            log.error("PathVariable参数化失败=====>>> ", e);
        }

        return null;
    }

    public Class getClassByType(Type type) {
        Class clazz;
        if(type.getTypeName().indexOf("Map<") > 0) {
            clazz = Map.class;
        }
        else {
            clazz = (Class) type;
        }
        return clazz;
    }
}
