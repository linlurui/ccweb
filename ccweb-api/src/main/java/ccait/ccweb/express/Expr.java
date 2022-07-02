package ccait.ccweb.express;

import ccait.ccweb.entites.ConditionInfo;
import entity.tool.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static ccait.ccweb.express.ExprInfo.*;

public class Expr {

    private static final Map<String, ExprResult> cacheMap = new HashMap<>();

    public static void fillData(KeyValueParts postDataList) throws Exception {
        if(postDataList == null) {
            return;
        }
        for(KeyValueParts.Part item : postDataList) {
            ExprInfo exprInfo = ExprInfo.parse(item.getValue().toString());
            if(exprInfo == null) {
                continue;
            }

            //处理变量
            boolean isRebuildId = false;
            for(ConditionInfo conditionInfo : exprInfo.getConditionInfos()) {
                String source = conditionInfo.getValue().toString();
                VarInfoList varInfos = VarInfo.parseList(source);
                for(VarInfo varInfo : varInfos) {
                    if (!varInfo.isMatches()) {
                        continue;
                    }

                    if (!postDataList.containsKey(varInfo.getName())) {
                        continue;
                    }

                    source = varInfo.replace(postDataList.get(varInfo.getName()));
                }
                conditionInfo.setValue(source);
                isRebuildId = true;
            }

            if(isRebuildId) {
                exprInfo.buildId();
            }

            Map<String, Object> data = getCacheData(exprInfo);
            if(data != null && data.containsKey(exprInfo.getColumn())) {
                postDataList.put(item.getKey(), data.get(exprInfo.getColumn()).toString());
                continue;
            }

            ExprResult result = exprInfo.exec();
            if(result == null) {
                continue;
            }

            cacheMap.put(result.getExprInfo().getId(), result);
            data = getCacheData(exprInfo);
            if(data != null && data.containsKey(exprInfo.getColumn())) {
                postDataList.put(item.getKey(), data.get(exprInfo.getColumn()).toString());
                continue;
            }
        }
    }

    private static Map<String, Object> getCacheData(ExprInfo exprInfo) throws Exception {
        if(exprInfo == null || StringUtils.isEmpty(exprInfo.getId())) {
            return null;
        }

        if(cacheMap.containsKey(exprInfo.getId())) {
            if(cacheMap.get(exprInfo.getId()) == null) {
                return null;
            }

           return cacheMap.get(exprInfo.getId()).getData();
        }
        return null;
    }
}
