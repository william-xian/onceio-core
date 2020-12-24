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

    public static BaseMeta createFindBaseMeta(Class<?> entityClass, Map<String, Object> nameToArg) {
        TableMeta tm = TableMeta.getTableMetaBy(entityClass);
        OAssert.err(tm != null, "%s is not a table.", entityClass.getName());
        BaseMeta def = new BaseMeta();
        String table = TableMeta.getTableName(entityClass);
        def.bind(table, def, entityClass);
        List<Object> args = new ArrayList<>();
        String select = (String) nameToArg.get("$columns");
        if (select == null) {
            def.select.append(String.format(" *\n"));
        } else {
            for (String col : select.split(",")) {
                for (ColumnMeta cm : tm.getColumnMetas()) {
                    if (cm.getField().getName().equals(col)) {
                        def.select.append(" "+ cm.getName() + ",");
                    }
                }
            }
            if (def.select.charAt(def.select.length() - 1) == ',') {
                def.select.setCharAt(def.select.length() - 1, '\n');
            } else {
                def.select.append(" *\n");
            }
        }


        def.from.append(String.format(" %s\n", def.table));
        nameToArg.forEach((name, val) -> {
            ColumnMeta cm = tm.getColumnMetaByName(name);
            if (cm != null) {
                args.add(val);
                if (val instanceof String) {
                    def.where.append(String.format("%s LIKE ? AND", cm.getName()));
                } else {
                    def.where.append(String.format("%s = ? AND", cm.getName()));
                }
            }
        });
        if (def.where.length() > 0) {
            def.where.deleteCharAt(def.where.length() - 4);
        }
        return def;
    }

    public static <M> M getMeta(BaseMeta<M> def) {
        return def.meta;
    }

    public static String getAlias(BaseMeta def) {
        return def.alias;
    }

    public static StringBuilder getSelect(BaseMeta def) {
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
