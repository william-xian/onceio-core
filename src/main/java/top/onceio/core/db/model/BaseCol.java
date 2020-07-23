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
        this.name = col.name().equals("")?field.getName():col.name();
        this.field = field;
    }

    public String name() {
        return table.alias + "." + name;
    }

    public T eq(Object val) {
        table.sql.append(" " + name() + " = ?");
        table.args.add(val);
        return table;
    }

    public T ne(Object val) {
        table.sql.append(" " + name() + " != ?");
        table.args.add(val);
        return table;
    }

    public T gt(Object val) {
        table.sql.append(" " + name() + " > ?");
        table.args.add(val);
        return table;
    }

    public T ge(Object val) {
        table.sql.append(" " + name() + " >= ?");
        table.args.add(val);
        return table;
    }

    public T lt(Object val) {
        table.sql.append(" " + name() + " < ?");
        table.args.add(val);
        return table;
    }

    public T le(Object val) {
        table.sql.append(" " + name() + " <= ?");
        table.args.add(val);
        return table;
    }

    public T in(Object... vals) {
        table.sql.append(" " + name() + " in (");
        for (Object val : vals) {
            table.sql.append("?,");
            table.args.add(val);
        }
        table.sql.deleteCharAt(table.sql.length() - 1);
        table.sql.append(")");
        return table;
    }

    public T in(BaseTable sub) {
        table.sql.append(" " + name() + " in (");
        table.sql.append(sub.sql.toString());
        table.args.addAll(sub.args);
        table.sql.append(")");
        return table;
    }

    public T set(Object val) {
        return table;
    }

    public T setExp(String val) {
        return table;
    }
}
