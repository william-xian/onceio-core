package top.onceio.core.db.model;

public class Func implements Queryable {
    private String name;

    @Override
    public String name() {
        return name;
    }

    private Func(String alias) {
        this.name = alias;
    }

    public static <A extends BaseCol, B extends BaseCol> Func alias(A col, B alias) {
        return new Func(String.format("%s.%s %s", col.table.alias, col.name, alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func max(A col) {
        return new Func(String.format("max(%s.%s) %s", col.table.alias, col.name, col.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func min(A col) {
        return new Func(String.format("min(%s.%s) %s", col.table.alias, col.name, col.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func sum(A col) {
        return new Func(String.format("sum(%s.%s) %s", col.table.alias, col.name, col.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func avg(A col) {
        return new Func(String.format("avg(%s.%s) %s", col.table.alias, col.name, col.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func count(A alias) {
        return new Func(String.format("count(1) %s", alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func max(A col, B alias) {
        return new Func(String.format("max(%s.%s) %s", col.table.alias, col.name, alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func min(A col, B alias) {
        return new Func(String.format("min(%s.%s) %s", col.table.alias, col.name, alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func sum(A col, B alias) {
        return new Func(String.format("sum(%s.%s) %s", col.table.alias, col.name, alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func avg(A col, B alias) {
        return new Func(String.format("avg(%s.%s) %s", col.table.alias, col.name, alias.name));
    }

    public static <A extends BaseCol, B extends BaseCol> Func count(A col, B alias) {
        return new Func(String.format("count(%s.%s) %s", col.table.alias, col.name, alias.name));
    }
}
