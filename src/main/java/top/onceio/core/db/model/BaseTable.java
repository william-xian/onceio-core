package top.onceio.core.db.model;

import java.util.ArrayList;
import java.util.List;

public class BaseTable<T> implements Queryable {
    protected T meta;
    protected String name;
    protected String alias;
    protected StringBuilder sql = new StringBuilder();
    protected List<Object> args = new ArrayList<>();

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
        sql.append("select");
        for (Queryable c : cs) {
            sql.append(" " + c.name() + ",");
        }
        sql.deleteCharAt(sql.length() - 1);
        return meta;
    }

    public <O extends BaseTable> T from(O... tables) {
        if (tables.length == 0) {
            sql.append(" from " + name + " as " + alias);
        } else {
            sql.append(" from");
            for (O t : tables) {
                sql.append(" " + t.name + " as " + t.alias + ",");
            }
            sql.deleteCharAt(sql.length() - 1);
        }
        return meta;
    }

    public T join(BaseTable otherTable) {
        sql.append(" left join " + otherTable.name + " as " + otherTable.alias);
        return meta;
    }

    public T on(BaseCol ac, BaseCol bc) {
        sql.append(String.format(" on %s.%s = %s.%s", ac.table.alias, ac.name, bc.table.alias, bc.name));
        return meta;
    }

    public T where() {
        sql.append(" where");
        return meta;
    }

    public <C extends BaseCol> T groupBy(C... cs) {
        sql.append(" group by");
        for (C c : cs) {
            sql.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        sql.deleteCharAt(sql.length() - 1);
        return meta;
    }

    public <C extends BaseCol> T orderBy(C... cs) {
        sql.append(" order by");
        for (C c : cs) {
            sql.append(String.format(" %s.%s,", c.table.alias, c.name));
        }
        sql.deleteCharAt(sql.length() - 1);
        return meta;
    }

    public <C extends BaseCol> T orderByDesc(C... cs) {
        sql.append(" order by");
        for (C c : cs) {
            sql.append(String.format(" %s.%s desc,", c.table.alias, c.name));
        }
        sql.deleteCharAt(sql.length() - 1);
        return meta;
    }

    public T limit(int s, int e) {
        sql.append(" limit " + s + "," + e);
        return meta;
    }

    public T as(String alias) {
        this.alias = alias;
        return meta;
    }

    public T and() {
        sql.append(" and ");
        return meta;
    }

    public T or() {
        sql.append(" or ");
        return this.meta;
    }

    public T and(BaseTable meta) {
        return this.meta;
    }

    public T or(BaseTable meta) {
        return this.meta;
    }

    public T not(BaseTable meta) {
        return this.meta;
    }

    @Override
    public String toString() {
        return sql.toString();
    }
}
