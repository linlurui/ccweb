package ccait.ccweb.express;

import ccait.ccweb.context.EntityContext;
import ccait.ccweb.entites.ConditionInfo;
import ccait.ccweb.enums.Algorithm;
import entity.query.*;
import entity.tool.util.DBUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExprInfo {

    private ExprInfo() {}
    private static final Logger log = LoggerFactory.getLogger(ExprInfo.class);
    private static final String FIELD_REGEX = "\\w[\\w\\d]*";
    private static final String TABLE_REGEX = "\\$\\{\\s*(" + FIELD_REGEX + "\\." + FIELD_REGEX + "\\." + FIELD_REGEX + ")\\s*\\}";

    private String id;
    private String datasource;
    private String table;
    private String column;
    private String expr;
    private String function;
    private List<ConditionInfo> conditionInfos;
    private List<String> fieldList;
    private Set<String> groupList;
    private Set<String> orderByList;

    public static ExprInfo parse(String value) {
        ExprInfo result = new ExprInfo();
        String regex = String.format("expr\\(\\s*(max|min|sum|count|avg)?(\\()?\\s*(%s)\\s*\\)?\\s*,\\s*(.+)\\s*\\)", TABLE_REGEX);
        Matcher m = Pattern.compile(regex).matcher(value);
        if(!m.matches()) {
            return null;
        }

        result.setExpr(value);
        if(StringUtils.isNotEmpty(m.group(1))) {
            result.setFunction(m.group(1));
        }

        if(StringUtils.isEmpty(m.group(3))) {
            return null;
        }

        Matcher m1 = Pattern.compile(TABLE_REGEX).matcher(m.group(3));
        if(!m1.matches()) {
            return null;
        }

        TreeSet ts = new TreeSet();
        StringBuffer buffer = new StringBuffer();
        List<String> list = StringUtils.splitString2List(m1.group(1), "\\.");
        result.setDatasource(list.get(0));
        result.setTable(list.get(1));
        buffer.append(result.getDatasource());
        buffer.append(".");
        buffer.append(result.getTable());

        result.setColumn(DBUtils.getSqlInjText(list.get(2)));
        if(StringUtils.isEmpty(m.group(5))) {
            return null;
        }
        list = StringUtils.splitString2List(m.group(5), ",");

        buffer.append("-->");
        result.fieldList = new ArrayList<>();
        result.conditionInfos = new ArrayList<>();
        result.groupList = new HashSet<>();
        result.orderByList = new HashSet<>();

        for(String item : list) {
            if(StringUtils.isEmpty(item)) {
                continue;
            }

            Matcher m2 = Pattern.compile("group\\(\\s*(\\w[\\w\\d]*)\\s*\\)").matcher(item);
            if(m2.matches()) {
                result.groupList.add(m2.group(1));
                continue;
            }

            m2 = Pattern.compile("asc\\(\\s*(\\w[\\w\\d]*)\\s*\\)").matcher(item);
            if(m2.matches()) {
                result.orderByList.add(String.format("%s ASC", m2.group(1)));
                continue;
            }

            m2 = Pattern.compile("desc\\(\\s*(\\w[\\w\\d]*)\\s*\\)").matcher(item);
            if(m2.matches()) {
                result.orderByList.add(String.format("%s DESC", m2.group(1)));
                continue;
            }

            m2 = Pattern.compile(String.format("(%s)\\s*([>=<]{1,2}| LIKE )\\s*([\\W\\w]+)", FIELD_REGEX)).matcher(item.trim());
            if(!m2.matches()) {
                log.error(String.format("fail to parse expr for %s\r\n", item));
                return null;
            }
            ConditionInfo conditionInfo = new ConditionInfo();
            conditionInfo.setName(DBUtils.getSqlInjText(m2.group(1)));
            result.conditionInfos.add(conditionInfo);
            result.fieldList.add(conditionInfo.getName());
            String algValue = m2.group(2);
            switch(algValue) {
                case "=":
                case ">":
                case "<":
                case ">=":
                case "<=":
                case "!=":
                case "<>":
                case " LIKE ":
                    List<Algorithm> algoLost = Arrays.asList(Algorithm.values());
                    Algorithm algo = algoLost.stream().filter(a -> a.getValue().equals(algValue)).findFirst().get();
                    conditionInfo.setAlgorithm(algo);
                    break;
                default:
                    log.error(String.format("can not support algorithm %s\r\n", m2.group(2)));
                    return null;
            }

            String expVal = "";
            if(StringUtils.isNotEmpty(m2.group(3))) {
                expVal = m2.group(3).trim();
            }
            if(StringUtils.isNotEmpty(expVal) && ((expVal.indexOf("\"")==0 && expVal.lastIndexOf("\"")==(m2.group(3).length()-1)) )) {
                conditionInfo.setValue(String.format("'%s'", DBUtils.getSqlInjText(expVal.substring(1, expVal.length()-1))));
            }

            else {
                conditionInfo.setValue(DBUtils.getSqlInjText(expVal));
            }

            ts.add(String.format("%s%s%s", conditionInfo.getName(), conditionInfo.getAlgorithm().getValue(), conditionInfo.getValue()));
        }

        ts.addAll(result.groupList);
        ts.addAll(result.orderByList);
        buffer.append(String.join(",", ts));
        result.id = buffer.toString();

        return result;
    }

    public ExprResult exec() throws SQLException {
        Queryable query = (Queryable) EntityContext.getEntity(datasource, table, fieldList);
        QueryableAction action = null;
        for(int i=0; i<conditionInfos.size(); i++) {
            if(i==0) {
                action = query.where(conditionInfos.get(i).toString());
            }

            else {
                action = ((Where) action).and(conditionInfos.get(i).toString());
            }
        }

        if(groupList.size() > 0) {
            action = ((Where) action).groupby(groupList.toArray(new String[groupList.size()]));
        }

        if(orderByList.size() > 0) {
            if(action instanceof GroupBy) {
                action = ((GroupBy) action).orderby(orderByList.toArray(new String[orderByList.size()]));
            }

            else {
                action = ((Where) action).orderby(orderByList.toArray(new String[orderByList.size()]));
            }
        }

        if(StringUtils.isNotEmpty(getFunction())) {
            if(action instanceof GroupBy) {
                action = ((GroupBy) action).select("*",
                        String.format("%s([%s]) as %s_%s", getFunction(), getColumn(),
                                getFunction(), getColumn()));
            }

            else if(action instanceof OrderBy) {
                action = ((OrderBy) action).select("*",
                        String.format("%s([%s]) as %s_%s", getFunction(), getColumn(),
                                getFunction(), getColumn()));
            }

            else {
                action = ((Where) action).select("*",
                        String.format("%s([%s]) as %s_%s", getFunction(), getColumn(),
                                getFunction(), getColumn()));
            }
        }

        if(action == null) {
            return null;
        }

        Map data = (Map) action.first(Map.class);
        if(data == null) {
            return null;
        }

        ExprResult result = new ExprResult();
        result.setExprInfo(this);
        result.setData(data);

        return result;
    }

    public void buildId() {
        TreeSet<String> ts = new TreeSet<>();
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s.%s-->", datasource, table));
        for(ConditionInfo conditionInfo : this.conditionInfos) {
            ts.add(String.format("%s%s%s", conditionInfo.getName(), conditionInfo.getAlgorithm().getValue(), conditionInfo.getValue()));
        }

        ts.addAll(groupList);
        ts.addAll(orderByList);
        buffer.append(String.join(",", ts));
        id = buffer.toString();
    }

    public String getId() {
        return id;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public List<ConditionInfo> getConditionInfos() {
        return conditionInfos;
    }
}
