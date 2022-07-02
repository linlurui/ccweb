package ccait.ccweb.express;

import entity.tool.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VarInfo {
    private boolean isSubstring;
    private boolean isReplace;
    private int startIndex;
    private int endIndex;
    private String replaceBegin;
    private String replaceEnd;
    private String name;
    private String source;
    private String wrapChars;

    public VarInfo() {
        this.setWrapChars("");
    }

    public String getWrapChars() {
        return wrapChars;
    }

    public void setWrapChars(String wrapChars) {
        this.wrapChars = wrapChars;
    }

    public String getName() {
        return name;
    }

    public boolean isMatches() {
        return matches;
    }

    private boolean matches;

    public String getValue() {
        return value;
    }

    private String value;

    public String getRegExp() {
        return regExp;
    }

    private String regExp;

    public static VarInfoList parseList(String value){
        return parseList(value, "", "");
    }

    public static VarInfoList parseList(String value, String prefix){
        return parseList(value, prefix, "");
    }

    public static VarInfoList parseList(String value, String prefix, String key) {
        String regExp = getRegExpText(prefix, key);
        Matcher m = Pattern.compile(regExp).matcher(value);
        VarInfoList varInfoList = new VarInfoList(value);
        while (m.find()) {

            VarInfo varInfo = new VarInfo();
            varInfo.source = value;
            varInfo.regExp = regExp;
            if (StringUtils.isEmpty(m.group("key"))) {
                continue;
            }

            setData(m, varInfo);

            varInfoList.add(varInfo);
        }

        return varInfoList;
    }

    public static VarInfo parse(String value){
        return parse(value, "", "");
    }

    public static VarInfo parse(String value, String prefix){
        return parse(value, prefix, "");
    }

    public static VarInfo parse(String value, String prefix, String key) {

        String regExp = getRegExpText(prefix, key);
        Matcher m = Pattern.compile(regExp).matcher(value);
        VarInfo varInfo = new VarInfo();
        varInfo.source = value;
        if(!m.matches()) {
            return varInfo;
        }

        varInfo.regExp = regExp;
        if(StringUtils.isEmpty(m.group("key"))) {
            return varInfo;
        }

        setData(m, varInfo);

        return varInfo;
    }

    private static void setData(Matcher m, VarInfo varInfo) {
        varInfo.name = m.group("key");
        varInfo.matches = true;
        varInfo.value = m.group(0);
        if ("substr".equals(m.group("fn"))) {
            if (StringUtils.isNotEmpty(m.group("param1")) && StringUtils.isNotEmpty(m.group("param2"))) {
                varInfo.isSubstring = true;
                varInfo.startIndex = Integer.parseInt(m.group("param1"));
                varInfo.endIndex = Integer.parseInt(m.group("param2"));
            }
        } else if ("replace".equals(m.group("fn"))) {
            varInfo.isReplace = true;
            varInfo.replaceBegin = m.group("param1");
            varInfo.replaceEnd = m.group("param2");
        }
    }

    private static String getRegExpText(String prefix, String key) {
        if (StringUtils.isNotEmpty(prefix)) {
            if (".".equals(prefix.substring(prefix.length() - 1))) {
                prefix = prefix.substring(0, prefix.length() - 2) + "\\.";
            } else {
                prefix = prefix + "\\.";
            }
        }

        if (StringUtils.isEmpty(key)) {
            key = "(?<key>\\w[\\w\\d]*)";
        } else {
            key = String.format("(?<key>%s)", key);
        }
        return String.format("\\$\\{\\s*%s%s(\\.(?<fn>substr)\\(\\s*(?<param1>(\"[^\"]+\"|\\d+))\\s*\\,\\s*(?<param2>(\"[^\"]+\"|\\d+))\\s*\\))?\\s*\\}", prefix, key);
    }

    public String replace(String replacement) {
        return replace("", replacement);
    }

    public String replace(String text, String replacement) {

        if(!isMatches()) {
            return value;
        }

        if(StringUtils.isEmpty(text)) {
            text = this.source;
        }

        if(StringUtils.isEmpty(replacement)) {
            replacement = "";
        }

        else if(isSubstring) {
            replacement = replacement.substring(startIndex, endIndex);
        }

        else if(isReplace) {
            replacement.replace(replaceBegin, replaceEnd);
        }

        if(StringUtils.isNotEmpty(getWrapChars())) {
            replacement = String.format("%s%s%s", getWrapChars(), replacement, getWrapChars());
        }

        text = text.replace(value, replacement);

        return text;
    }
}
