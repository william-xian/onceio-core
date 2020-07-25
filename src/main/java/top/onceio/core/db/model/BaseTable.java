package top.onceio.core.db.model;

import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OReflectUtil;

import java.util.ArrayList;
import java.util.List;


public class BaseTable<M> {
    protected M meta;
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

    List<BaseTable<?>> refs = new ArrayList<>();

    public BaseCol<?> id;

    public BaseTable(String name) {
        this.name = name;
        this.alias = "t";
    }

    protected <E> void bind(M meta, Class<E> e) {
        this.meta = meta;
        id = new BaseCol(this, OReflectUtil.getField(e, "id"));
    }

    public String getName() {
        return name;
    }

    public M alias(String alias) {
        this.alias = alias;
        return meta;
    }


    public M select(Queryable... cs) {
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

    public <O extends BaseTable> M from(O... tables) {
        if (tables.length == 0) {
            from.append(" " + name + " as " + alias);
        } else {
            for (O t : tables) {
                from.append(" " + t.name + " AS " + t.alias + ",");

                refs.add(t);
            }
            from.deleteCharAt(from.length() - 1);
        }

        return meta;
    }

    public M join(BaseTable otherTable) {
        from.append(" LEFT JOIN " + otherTable.name + " AS " + otherTable.alias);

        refs.add(otherTable);
        return meta;
    }

    public M on(BaseCol ac, BaseCol bc) {
        from.append(String.format(" ON %s.%s = %s.%s", ac.table.alias, ac.name, bc.table.alias, bc.name));
        return meta;
    }

    public M where() {
        return meta;
    }

    public <C extends BaseCol> M groupBy(C... cs) {
        for (C c : cs) {
            group.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        group.deleteCharAt(group.length() - 1);
        return meta;
    }

    public <C extends BaseCol> M orderBy(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public <C extends BaseCol> M orderByDesc(C... cs) {
        for (C c : cs) {
            order.append(String.format(" %s.%s desc,", c.table.alias, c.name));
        }
        order.deleteCharAt(order.length() - 1);
        return meta;
    }

    public M limit(int s, int e) {
        limit.append(" " + s + "," + e);
        return meta;
    }

    public M as(String alias) {
        this.alias = alias;
        return meta;
    }

    public M and() {
        where.append(" AND ");
        return meta;
    }

    public M or() {
        where.append(" OR ");
        return this.meta;
    }

    public M and(BaseTable meta) {
        where.append(" AND (" + meta.toString() + ")");
        args.addAll(meta.args);

        refs.add(meta);
        return this.meta;
    }

    public M or(BaseTable meta) {
        where.append(" OR (" + meta.toString() + ")");
        args.addAll(meta.args);

        refs.add(meta);
        return this.meta;
    }

    public M not(BaseTable meta) {
        where.append(" NOT (" + meta.toString() + ")");
        args.addAll(meta.args);

        refs.add(meta);
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

    public List<BaseTable<?>> getRefs() {
        return refs;
    }
}
