package ccait.ccweb.express;

import java.util.ArrayList;

public class VarInfoList extends ArrayList<VarInfo> {
    public String getValue() {
        return value;
    }

    private String value;

    public VarInfoList(String value) {
        this.value = value;
    }

    public String replace(String replacement) {

        for(VarInfo item : this) {
            if(!item.isMatches()) {
                continue;
            }

            value = item.replace(value, replacement);
        }

        return value;
    }
}
