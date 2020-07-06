package top.onceio.core.db.model;

public class StringCol<T extends BaseTable> extends BaseCol<T> {

    public StringCol(T table, String name) {
        super(table, name);
    }

    public T like(String other) {
        table.sql.append(" " + name() + " like ?");
        table.args.add(other);
        return table;
    }
}
