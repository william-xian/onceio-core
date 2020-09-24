package top.onceio.core.db.model;

import java.util.List;

public class AccessHelper {

    public static String getTable(BaseMeta def) {
        return def.table;
    }

    public static List<BaseMeta<?>> getRefs(BaseMeta def) {
        return def.refs;
    }

    public <M> M getMeta(BaseMeta<M> def) {
        return def.meta;
    }

    public  String getAlias(BaseMeta def) {
        return def.alias;
    }

    public StringBuilder getSelect(BaseMeta def)  {
        return def.select;
    }

    public StringBuilder getFrom(BaseMeta def) {
        return def.from;
    }

    public StringBuilder getWhere(BaseMeta def) {
        return def.where;
    }

    public StringBuilder getUpdate(BaseMeta def) {
        return def.update;
    }

}
