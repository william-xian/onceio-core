package top.onceio.core.db.model;

import java.util.ArrayList;
import java.util.List;

public class BaseTable<T> {
    protected T meta;
    protected String name;
    protected String alias;
    protected List<Object> args = new ArrayList<>();

    private StringBuilder select = new StringBuilder();
    private StringBuilder from = new StringBuilder();
    StringBuilder where = new StringBuilder();
    private StringBuilder group = new StringBuilder();
    private StringBuilder having = new StringBuilder();
    private StringBuilder limit = new StringBuilder();
    private StringBuilder order = new StringBuilder();

    public BaseTable(String name) {
        this.name = name;
        this.alias = "t";
    }

    public BaseTable(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    protected void bind(T meta) {
        this.meta = meta;
    }

    public String name() {
        return name + "." + name;
    }

    public T select(Queryable... cs) {
        if (cs.length > 0) {
            for (Queryable c : cs) {
                select.append(" " + c.name() + ",");
            }
            select.deleteCharAt(select.length() - 1);
        } else {
            select.append("*");
        }
        return meta;
    }

    public <O extends BaseTable> T from(O... tables) {
        if (tables.length == 0) {
            from.append(" " + name + " as " + alias);
        } else {
            for (O t : tables) {
                from.append(" " + t.name + " AS " + t.alias + ",");
            }
            from.deleteCharAt(from.length() - 1);
        }
        return meta;
    }

    public T join(BaseTable otherTable) {
        from.append(" LEFT JOIN " + otherTable.name + " AS " + otherTable.alias);
        return meta;
    }

    public T on(BaseCol ac, BaseCol bc) {
        from.append(String.format(" ON %s.%s = %s.%s", ac.table.alias, ac.name, bc.table.alias, bc.name));
        return meta;
    }

    public T where() {
        return meta;
    }

    public <C extends BaseCol> T groupBy(C... cs) {
        for (C c : cs) {
            group.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        group.deleteCharAt(group.length() - 1);
        return meta;
    }

    public <C extends BaseCol> T orderBy(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public <C extends BaseCol> T orderByDesc(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s desc,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public T limit(int s, int e) {
        limit.append(" " + s + "," + e);
        return meta;
    }

    public T as(String alias) {
        this.alias = alias;
        return meta;
    }

    public T and() {
        where.append(" AND ");
        return meta;
    }

    public T or() {
        where.append(" OR ");
        return this.meta;
    }

    public T and(BaseTable meta) {
        where.append(" AND (" + meta.toString() + ")");
        args.addAll(meta.args);
        return this.meta;
    }

    public T or(BaseTable meta) {
        where.append(" OR (" + meta.toString() + ")");
        args.addAll(meta.args);
        return this.meta;
    }

    public T not(BaseTable meta) {
        where.append(" NOT (" + meta.toString() + ")");
        args.addAll(meta.args);
        return this.meta;
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        if (select.length() > 0) {
            sql.append("SELECT" + select);
        }
        if (from.length() > 0) {
            sql.append(" FROM" + from);
        }
        if (where.length() > 0) {
            sql.append(" WHERE" + where);
        }
        if (group.length() > 0) {
            sql.append(" GROUP BY" + group);
        }
        if (having.length() > 0) {
            sql.append(" HAVING" + having);
        }
        if (order.length() > 0) {
            sql.append(" ORDER BY" + order);
        }
        if (limit.length() > 0) {
            sql.append(" LIMIT" + limit);
        }
        return sql.toString();
    }

    public String toSql() {
        StringBuilder sql = new StringBuilder(toString());
        int start = 0;
        for (int i = 0; i < args.size(); i++) {
            Object val = args.get(i);
            int index = sql.indexOf("?", start);
            sql.deleteCharAt(index);
            String str = val != null ? val.toString() : "NULL";
            start = index + str.length();
            if (val instanceof Number) {
                sql.insert(index, str);
            } else {
                sql.insert(index, "'" + str + "'");
            }
        }
        return sql.toString();
    }
}
