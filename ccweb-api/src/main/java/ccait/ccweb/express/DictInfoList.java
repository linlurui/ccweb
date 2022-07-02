package ccait.ccweb.express;

import java.util.ArrayList;

public class DictInfoList extends ArrayList<DictInfo> {

    public String getValue() {
        return value;
    }

    private String value;

    public DictInfoList(String value) {
        this.value = value;
    }

    public String replace() {

        for(DictInfo item : this) {
            if(!item.isMatches()) {
                continue;
            }

            value = item.replace(value);
        }

        return value;
    }
}
