package top.onceio.core.db.model;

import java.util.List;

public class AccessHelper {
    public static String getName(BaseMeta def) {
        return def.name;
    }

    public static List<BaseMeta<?>> getRefs(BaseMeta def) {
        return def.refs;
    }
}
