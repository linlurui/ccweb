package ccait.ccweb.express;

import entity.query.core.ApplicationConfig;
import entity.tool.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictInfo {

    private String value;
    private String regExp;
    private boolean matches;
    private String key;
    private String fn;
    private String source;
    private String wrapChars;

    public DictInfo() {
        this.setWrapChars("");
    }

    public String getWrapChars() {
        return wrapChars;
    }

    public void setWrapChars(String wrapChars) {
        this.wrapChars = wrapChars;
    }

    public String getValue() {
        return value;
    }

    public String getRegExp() {
        return regExp;
    }

    public boolean isMatches() {
        return matches;
    }

    public String getKey() {
        return key;
    }

    public String getFn() {
        return fn;
    }


    public static DictInfoList parseList(String value) {
        DictInfoList dictInfoList = new DictInfoList(value);
        String regExp = "\\$\\[dict.keys\\s+(?<fn>get|in|start|end)\\s+(?<key>[\\d\\w]+)\\s*\\]";
        Matcher m = Pattern.compile(regExp).matcher(value);
        while (m.find()) {
            DictInfo info = new DictInfo();
            info.value = m.group(0);
            info.regExp = regExp;
            info.source = value;
            if (StringUtils.isEmpty(m.group("key")) ||
                    StringUtils.isEmpty(m.group("fn"))) {
                continue;
            }

            info.key = m.group("key");
            info.fn = m.group("fn");
            info.matches = true;
            dictInfoList.add(info);
        }

        return dictInfoList;
    }

    public static DictInfo parse(String value) {
        DictInfo info = new DictInfo();
        info.source = value;
        info.regExp = "\\$\\[dict.keys\\s+(?<fn>get|in|start|end)\\s+(?<key>[\\d\\w]+)\\s*\\]";
        Matcher m = Pattern.compile(info.regExp).matcher(value);
        if(!m.matches()) {
            return info;
        }

        if(StringUtils.isEmpty(m.group("key")) ||
                StringUtils.isEmpty(m.group("fn"))) {
            return info;
        }

        info.value = m.group(0);
        info.key = m.group("key");
        info.fn = m.group("fn");
        info.matches = true;

        return info;
    }

    public String replace() {
        return replace("");
    }

    public String replace(String text) {

        if(!isMatches()) {
            return value;
        }

        if(StringUtils.isEmpty(getFn()) || StringUtils.isEmpty(getKey())) {
            return value;
        }

        Map dict = ApplicationConfig.getInstance().getMap("ccweb.dict");
        if(dict == null || dict.size()==0) {
            return value;
        }

        if(!dict.containsKey("map") || dict.get("map")==null || !(dict.get("map") instanceof Map)) {
            return value;
        }

        if(StringUtils.isEmpty(text)) {
            text = this.source;
        }

        String replacement = "";
        if(dict.containsKey("default") && dict.get("default")!=null) {
            replacement = dict.get("default").toString();
        }

        Map<Object, Object> map = (Map) dict.get("map");
        Optional<String> opt = null;

        switch(getFn()) {
            case "get":
                opt = map.entrySet().stream().filter(a->a.getKey().toString().equals(getKey()))
                        .map(a->a.getValue().toString()).findFirst();
                break;
            case "in":
                opt = map.entrySet().stream().filter(a->getKey().indexOf(a.getKey().toString())>-1)
                        .map(a->a.getValue().toString()).findFirst();
                break;
            case "start":
                opt = map.entrySet().stream().filter(a->getKey().startsWith(a.getKey().toString()))
                        .map(a->a.getValue().toString()).findFirst();
                break;
            case "end":
                opt = map.entrySet().stream().filter(a->getKey().endsWith(a.getKey().toString()))
                        .map(a->a.getValue().toString()).findFirst();
                break;
        }

        if(opt.isPresent()) {
            replacement = opt.get();
        }

        if(StringUtils.isNotEmpty(wrapChars)) {
            replacement = String.format("%s%s%s", getWrapChars(), replacement, getWrapChars());
        }

        text = text.replace(value, replacement);

        return text;
    }
}
