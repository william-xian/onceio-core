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
            String[] nameOpt = name.split("\\$");
            String fieldName = nameOpt[0];
            ColumnMeta cm = tm.getColumnMetaByFieldName(fieldName);
            if (cm == null) {
                return;
            }
            if (nameOpt.length == 1) {
                Object arg = OUtils.trans(val, cm.getJavaBaseType());
                if (arg != null) {
                    def.args.add(arg);
                    def.where.append(String.format(" %s = ? AND", cm.getName()));
                } else {
                    LOGGER.error("参数不正确：{}", val);
                }
            }
            if (nameOpt.length == 2) {
                String opt = nameOpt[1];
                if (opt.equals("in")) {
                    String arr[] = val.toString().split(",");
                    if (arr.length >= 1) {
                        List<Object> args = new ArrayList<>();
                        for (int i = 0; i < arr.length; i++) {
                            Object arg = OUtils.trans(arr[i].trim(), cm.getJavaBaseType());
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
                } else {
                    Object arg = OUtils.trans(val, cm.getJavaBaseType());
                    if (arg != null) {
                        def.args.add(arg);
                        switch (opt) {
                            case "eq":
                                def.where.append(String.format(" %s = ? AND", cm.getName()));
                                break;
                            case "gt":
                                def.where.append(String.format(" %s > ? AND", cm.getName()));
                                break;
                            case "ge":
                                def.where.append(String.format(" %s >= ? AND", cm.getName()));
                                break;
                            case "lt":
                                def.where.append(String.format(" %s < ? AND", cm.getName()));
                                break;
                            case "le":
                                def.where.append(String.format(" %s <= ? AND", cm.getName()));
                                break;
                            case "like":
                                def.where.append(String.format(" %s like ? AND", cm.getName()));
                                break;
                            case "match":
                                def.where.append(String.format(" %s @@ ? AND", cm.getName()));
                                break;
                            case "regexp":
                                def.where.append(String.format(" %s ~ ? AND", cm.getName()));
                                break;
                            case "i_regexp":
                                def.where.append(String.format(" %s ~* ? AND", cm.getName()));
                                break;
                            case "regexp_not":
                                def.where.append(String.format(" %s !~ ? AND", cm.getName()));
                                break;
                            case "i_regexp_not":
                                def.where.append(String.format(" %s !~* ? AND", cm.getName()));
                                break;

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
