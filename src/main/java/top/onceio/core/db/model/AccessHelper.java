package top.onceio.core.db.model;

import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccessHelper {

    public static String getTable(BaseMeta def) {
        return def.table;
    }

    public static List<BaseMeta<?>> getRefs(BaseMeta def) {
        return def.refs;
    }

    public static BaseMeta createFindBaseMeta(Class<?> entityClass, Map<String,Object> nameToArg) {
        TableMeta tm = TableMeta.getTableMetaBy(entityClass);
        OAssert.err(tm != null, "%s is not a table.", entityClass.getName());
        BaseMeta def = new BaseMeta();
        String table  = TableMeta.getTableName(entityClass);
        def.bind(table, def, entityClass);
        List<Object> args = new ArrayList<>();
        String select = (String)nameToArg.getOrDefault(":columns", "*");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SELECT %s\n", select.replaceAll("([A-Z])", "_$1").toLowerCase()));
        sb.append(String.format("FROM %s\nWHERE ", def.table));
        nameToArg.forEach((name,val) -> {
            ColumnMeta cm = tm.getColumnMetaByName(name);
            if(cm != null) {
                args.add(val);
                if(val instanceof String) {
                    sb.append(String.format("%s LIKE ? AND" , cm.getName()));
                } else {
                    sb.append(String.format("%s = ? AND" , cm.getName()));
                }
            }
        });
        if(sb.length() >0) {
            sb.deleteCharAt(sb.length() - 4);
        }
        return def;
    }

    public static <M> M getMeta(BaseMeta<M> def) {
        return def.meta;
    }

    public static String getAlias(BaseMeta def) {
        return def.alias;
    }

    public static StringBuilder getSelect(BaseMeta def)  {
        return def.select;
    }

    public static StringBuilder getFrom(BaseMeta def) {
        return def.from;
    }

    public static StringBuilder getWhere(BaseMeta def) {
        return def.where;
    }

    public static StringBuilder getUpdate(BaseMeta def) {
        return def.update;
    }

}
