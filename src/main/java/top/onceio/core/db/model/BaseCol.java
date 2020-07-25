package top.onceio.core.db.model;

import top.onceio.core.db.annotation.Col;

import java.lang.reflect.Field;

public class BaseCol<T extends BaseTable> implements Queryable {
    T table;
    public String name;
    private Field field;

    public BaseCol(T table, Field field) {
        this.table = table;
        Col col = field.getAnnotation(Col.class);
        this.name = col.name().equals("") ? field.getName() : col.name();
        this.field = field;
    }

    public String name() {
        return table.alias + "." + name;
    }

    public T eq(Object val) {
        table.where.append(" " + name() + " = ?");
        table.args.add(val);
        return table;
    }

    public T ne(Object val) {
        table.where.append(" " + name() + " != ?");
        table.args.add(val);
        return table;
    }

    public T gt(Object val) {
        table.where.append(" " + name() + " > ?");
        table.args.add(val);
        return table;
    }

    public T ge(Object val) {
        table.where.append(" " + name() + " >= ?");
        table.args.add(val);
        return table;
    }

    public T lt(Object val) {
        table.where.append(" " + name() + " < ?");
        table.args.add(val);
        return table;
    }

    public T le(Object val) {
        table.where.append(" " + name() + " <= ?");
        table.args.add(val);
        return table;
    }

    public T in(Object... vals) {
        table.where.append(" " + name() + " IN (");
        for (Object val : vals) {
            table.where.append("?,");
            table.args.add(val);
        }
        table.where.deleteCharAt(table.where.length() - 1);
        table.where.append(")");
        return table;
    }

    public T in(BaseTable sub) {
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
            table.args.add(val);
        }
        table.where.deleteCharAt(table.where.length() - 1);
        table.where.append(")");
        return table;
    }

    public T notIn(BaseTable sub) {
        table.where.append(" " + name() + " NOT IN (");
        table.where.append(sub.toString());
        table.args.addAll(sub.args);
        table.where.append(")");

        table.refs.add(sub);
        return table;
    }
    public T set(Object val) {
        table.update.append(" " + name + " = ?,");
        table.args.add(val);
        return table;
    }

    public T setExp(String val) {
        table.update.append(String.format(" %s = %s + (%s),", name, name, val));
        return table;
    }
}
