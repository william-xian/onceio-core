package top.onceio.core.db.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccessHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(AccessHelper.class);

    public static String getTable(BaseMeta def) {
        return def.table;
    }

    public static List<BaseMeta<?>> getRefs(BaseMeta def) {
        return def.refs;
    }

    public static BaseMeta createDeleteBaseMeta(Class<?> entityClass, Map<String, Object> nameToArg) {
        TableMeta tm = TableMeta.getTableMetaBy(entityClass);
        OAssert.err(tm != null, "%s is not a table.", entityClass.getName());
        BaseMeta def = new BaseMeta();
        String table = TableMeta.getTableName(entityClass);
        def.bind(table, def, entityClass);
        initWhere(def, tm, nameToArg);
        return def;
    }

    public static void initWhere(BaseMeta def, TableMeta tm, Map<String, Object> nameToArg) {
        nameToArg.forEach((name, val) -> {
            if (name.endsWith("$in")) {
                String arr[] = val.toString().split(",");
                if (arr.length >= 1) {
                    ColumnMeta cm = tm.getColumnMetaByName(name.substring(0, name.length() - 3));
                    if (cm != null) {
                        List<Object> args = new ArrayList<>();
                        for (int i = 0; i < arr.length; i++) {
                            Object arg = OUtils.trans(arr[i], cm.getJavaBaseType());
                            if (arg != null) {
                                args.add(arg);
                            } else {
                                break;
                            }
                        }
                        if (args.size() == arr.length) {
                            def.args.addAll(args);
                            def.where.append(String.format(" %s IN (%s) AND", cm.getName(), OUtils.genStub("?", ",", arr.length)));
                        } else {
                            LOGGER.error("参数不正确：{}", val);
                        }
                    }
                }
            } else if (name.endsWith("$range")) {
                String arr[] = val.toString().split(",");
                if (arr.length == 2) {
                    ColumnMeta cm = tm.getColumnMetaByName(name.substring(0, name.length() - 6));
                    if (cm != null) {
                        Object arg1 = OUtils.trans(arr[0], cm.getJavaBaseType());
                        Object arg2 = OUtils.trans(arr[1], cm.getJavaBaseType());
                        if (arg1 != null && arg2 != null) {
                            def.args.add(arg1);
                            def.args.add(arg2);
                            def.where.append(String.format(" (%s >= ? AND %s < ?) AND", cm.getName(), cm.getName()));
                        } else {
                            LOGGER.error("参数不正确：{}", val);
                        }
                    }
                }
            } else {
                ColumnMeta cm = tm.getColumnMetaByName(name);
                if (cm != null) {
                    Object arg = OUtils.trans(val, cm.getJavaBaseType());
                    if (arg != null) {
                        def.args.add(arg);
                        if (Number.class.isAssignableFrom(cm.getJavaBaseType()) || Boolean.class.isAssignableFrom(cm.getJavaBaseType())) {
                            def.where.append(String.format(" %s = ? AND", cm.getName()));
                        } else {
                            def.where.append(String.format(" %s LIKE ? AND", cm.getName()));
                        }
                    } else {
                        LOGGER.error("参数不正确：{}", val);
                    }
                }
            }
        });
        if (def.where.length() > 0) {
            def.where.delete(def.where.length() - 4, def.where.length());
        }
    }

    public static BaseMeta createFindBaseMeta(Class<?> entityClass, Map<String, Object> nameToArg) {
        TableMeta tm = TableMeta.getTableMetaBy(entityClass);
        OAssert.err(tm != null, "%s is not a table.", entityClass.getName());
        BaseMeta def = new BaseMeta();
        String table = TableMeta.getTableName(entityClass);
        def.bind(table, def, entityClass);
        String select = (String) nameToArg.get("$columns");
        if (select == null) {
            def.select.append(String.format(" *\n"));
        } else {
            for (String col : select.split(",")) {
                for (ColumnMeta cm : tm.getColumnMetas()) {
                    if (cm.getField().getName().equals(col)) {
                        def.select.append(" " + cm.getName() + ",");
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

        initWhere(def, tm, nameToArg);

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
