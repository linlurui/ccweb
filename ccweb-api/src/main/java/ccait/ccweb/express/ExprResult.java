package ccait.ccweb.express;

import entity.tool.util.StringUtils;

import java.util.Map;

public class ExprResult {

    private ExprInfo exprInfo;
    private Map<String, Object> data;

    public String getValue() throws Exception {

        if(data == null) {
            throw new Exception("ExprResult data can not be null!!!");
        }
        if(StringUtils.isNotEmpty(getExprInfo().getFunction())) {
            return data.get(String.format("%s_%s", getExprInfo().getFunction(), getExprInfo().getColumn())).toString();
        }

        return data.get(getExprInfo().getColumn()).toString();
    }

    public ExprInfo getExprInfo() {
        return exprInfo;
    }

    public void setExprInfo(ExprInfo exprInfo) {
        this.exprInfo = exprInfo;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
