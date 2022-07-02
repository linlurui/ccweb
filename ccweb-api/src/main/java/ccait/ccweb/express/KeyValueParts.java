package ccait.ccweb.express;

import entity.tool.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeyValueParts extends ArrayList<KeyValueParts.Part> {

    public void put(String key, Object value) {
        if(value == null) {
            return;
        }

        if(StringUtils.isEmpty(key)) {
            return;
        }

        if(this.containsKey(key)) {
            this.stream().filter(a->a.getKey().equals(key))
                    .forEach(a->a.setValue(value.toString()));
        }

        else {
            Part part = new Part() {{
                setKey(key);
                setValue(value.toString());
            }};

            add(part);
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for(Part part : this) {
            map.put(part.getKey(), part.getValue());
        }

        return map;
    }

    public String get(String key) {
        Optional<Part> opt = this.stream()
                .filter(a -> a.getKey().equals(key) && a.getValue()!=null)
                .findFirst();

        if(opt.isPresent()) {
            return opt.get().getValue().toString();
        }

        return "";
    }

    public boolean containsKey(String key) {
        return this.stream().filter(a->a.getKey().equals(key)).findFirst().isPresent();
    }

    public class Part {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        private Object value;
    }
}
