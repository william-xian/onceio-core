package top.onceio.core.db.model;

import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseCol<T extends BaseMeta> implements Queryable {
    T table;
    String name;
    Field field;

    public BaseCol(T table, Field field) {
        this.table = table;
        this.name = TableMeta.getColumnName(field);
        this.field = field;
    }

    public String name() {
        return table.alias + "." + name;
    }

    private void addArg(Object val) {
        if (val != null) {
            if (OReflectUtil.isBaseType(val.getClass())) {
                table.args.add(val);
            } else {
                table.args.add(val.toString());
            }
        } else {
            table.args.add(null);
        }
    }

    public T eq(Object val) {
        table.where.append(" " + name() + " = ?");
        addArg(val);
        return table;
    }


    public T ne(Object val) {
        table.where.append(" " + name() + " != ?");
        addArg(val);
        return table;
    }

    public T gt(Object val) {
        table.where.append(" " + name() + " > ?");
        addArg(val);
        return table;
    }

    public T ge(Object val) {
        table.where.append(" " + name() + " >= ?");
        addArg(val);
        return table;
    }

    public T lt(Object val) {
        table.where.append(" " + name() + " < ?");
        addArg(val);
        return table;
    }

    public T le(Object val) {
        table.where.append(" " + name() + " <= ?");
        addArg(val);
        return table;
    }

    private static void resolve(List<Object> result, Object... vals) {
        for (Object val : vals) {
            if (val instanceof Collection) {
                for (Object iVal : (Collection) val) {
                    resolve(result, iVal);
                }
            } else {
                result.add(val);
            }
        }
    }


    public T in(Object... vals) {
        table.where.append(" " + name() + " IN (");
        List<Object> args = new ArrayList<>();
        resolve(args, vals);
        for (Object val : args) {
            table.where.append("?,");
            addArg(val);
        }
        table.where.deleteCharAt(table.where.length() - 1);
        table.where.append(")");
        return table;
    }

    public T in(BaseMeta sub) {
        table.where.append(" " + name() + " IN (");
        table.where.append(sub.toString());
        table.args.addAll(sub.args);
        table.where.append(")");

        table.refs.add(sub);
        return table;
    }

    public T notIn(Object... vals) {
        table.where.append(" " + name() + " NOT IN (");
        for (Object val : vals) {
            table.where.append("?,");
            addArg(val);
        }
        table.where.deleteCharAt(table.where.length() - 1);
        table.where.append(")");
        return table;
    }

    public T notIn(BaseMeta sub) {
        table.where.append(" " + name() + " NOT IN (");
        table.where.append(sub.toString());
        for (Object val : sub.args) {
            addArg(val);
        }
        table.where.append(")");

        table.refs.add(sub);
        return table;
    }

    public T set(Object val) {
        table.update.append(" " + name + " = ?,");
        addArg(val);
        return table;
    }

    public T setExp(String val) {
        table.update.append(String.format(" %s = %s + (%s),", name, name, val));
        return table;
    }
}
