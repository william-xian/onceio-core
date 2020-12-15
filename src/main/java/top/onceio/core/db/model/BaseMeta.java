package top.onceio.core.db.model;

import java.util.ArrayList;
import java.util.List;


public class BaseMeta<M> {
    protected M meta;
    protected String table;
    protected String alias;
    protected List<Object> args = new ArrayList<>();

    StringBuilder select = new StringBuilder();
    StringBuilder from = new StringBuilder();
    StringBuilder where = new StringBuilder();
    private StringBuilder group = new StringBuilder();
    private StringBuilder having = new StringBuilder();
    private StringBuilder limit = new StringBuilder();
    private StringBuilder order = new StringBuilder();

    StringBuilder update = new StringBuilder();
    List<BaseMeta<?>> refs = new ArrayList<>();

    protected <E> void bind(String table,M meta, Class<E> e) {
        this.alias = "t";
        this.table = table;
        this.meta = meta;
    }

    public M as(String alias) {
        this.alias = alias;
        return meta;
    }

    List<Object> getArgs() {
        return args;
    }

    public final M select(Queryable... cs) {
        if (cs.length > 0) {
            for (Queryable c : cs) {
                select.append(" " + c.name() + ",");
            }
            select.deleteCharAt(select.length() - 1);
        } else {
            select.append(" *");
        }
        return meta;
    }

    public final <O extends BaseMeta> M from(O... tables) {
        if (tables.length == 0) {
            from.append(" " + table + " AS " + alias);
        } else {
            for (O t : tables) {
                from.append(" " + t.table + " AS " + t.alias + ",");

                refs.add(t);
            }
            from.deleteCharAt(from.length() - 1);
        }

        return meta;
    }

    public final M join(BaseMeta otherTable) {
        from.append(" LEFT JOIN " + otherTable.table + " AS " + otherTable.alias);

        refs.add(otherTable);
        return meta;
    }

    public final M on(BaseCol ac, BaseCol bc) {
        from.append(String.format(" ON %s.%s = %s.%s", ac.table.alias, ac.name, bc.table.alias, bc.name));
        return meta;
    }

    public final M where() {
        return meta;
    }

    public final <C extends BaseCol> M groupBy(C... cs) {
        for (C c : cs) {
            group.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        group.deleteCharAt(group.length() - 1);
        return meta;
    }

    public final <C extends BaseCol> M orderBy(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public final <C extends BaseCol> M orderByDesc(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s DESC,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public final M limit(int s, int e) {
        limit.append(String.format(" %d OFFSET %d", s, e));
        return meta;
    }


    public final M and() {
        where.append(" AND ");
        return meta;
    }

    public final M or() {
        where.append(" OR ");
        return this.meta;
    }

    public final M and(BaseMeta meta) {
        where.append(" AND (" + meta.where + ")");
        args.addAll(meta.args);

        refs.add(meta);
        return this.meta;
    }

    public final M or(BaseMeta meta) {
        where.append(" OR (" + meta.where + ")");
        args.addAll(meta.args);

        refs.add(meta);
        return this.meta;
    }


    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        if (select.length() > 0) {
            sql.append("SELECT" + select);
            if (from.length() > 0) {
                sql.append(" FROM" + from);
            }
        }
        if (update.length() > 0) {
            if (from.length() <= 0) {
                sql.append("UPDATE " + table + " AS " + alias);
            } else {
                sql.append("UPDATE" + from);
            }
            sql.append(" SET" + update);
            sql.deleteCharAt(sql.length() - 1);
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
            sql.append(" LIMIT " + limit);
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

    public BaseMeta<M> copy() {
        BaseMeta<M> other = new BaseMeta<>();
        other.table = this.table;
        other.meta = this.meta;
        other.select.append(this.select);
        other.from.append(this.from);
        other.where.append(this.where);
        other.group.append(this.group);
        other.having.append(this.having);
        other.order.append(this.order);
        other.limit.append(this.limit);
        other.args.addAll(this.args);
        //other.refs.addAll(this.refs);
        return other;
    }

    public String getTable() {
        return table;
    }

}
