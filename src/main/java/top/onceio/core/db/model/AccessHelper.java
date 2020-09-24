package top.onceio.core.db.model;

import java.util.List;

public class AccessHelper {
    public static String getTable(BaseMeta def) {
        return def.table;
    }

    public static List<BaseMeta<?>> getRefs(BaseMeta def) {
        return def.refs;
    }
}
