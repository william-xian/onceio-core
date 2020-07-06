package top.onceio.core.db.model;

public class BaseCol<T extends BaseTable> implements Queryable {
    T table;
    public String name;

    public BaseCol(T table, String name) {
        this.table = table;
        this.name = name;
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
        table.sql.deleteCharAt(table.sql.length() - 1);
        table.sql.append(")");
        return table;
    }
}
