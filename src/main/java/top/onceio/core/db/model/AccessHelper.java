package top.onceio.core.db.model;

import java.util.List;

public class AccessHelper {
    public static String getName(BaseTable def) {
        return def.name;
    }

    public static List<BaseTable<?>> getRefs(BaseTable def) {
        return def.refs;
    }
}
