package ccait.ccweb.config;

import entity.tool.util.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;


@Component
public class EnumConvertFactory implements ConverterFactory<String, Object> {
    @Override
    public <T> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToIEum(targetType);
    }

    @SuppressWarnings("all")
    private static class StringToIEum<T> implements Converter<String, T> {
        private Class<T> targerType;
        public StringToIEum(Class<T> targerType) {
            this.targerType = targerType;
        }

        @Override
        public T convert(String source) {

            if(!targerType.isEnum() || StringUtils.isEmpty(source)) {
                return (T) source;
            }

            for (T enumObj : targerType.getEnumConstants()) {
                if (source.toLowerCase().equals(String.valueOf(enumObj).toLowerCase())) {
                    return enumObj;
                }
            }

            return null;
        }
    }
}