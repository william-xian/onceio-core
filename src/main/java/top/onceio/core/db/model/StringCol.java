package top.onceio.core.db.model;

import java.lang.reflect.Field;

public class StringCol<T extends BaseTable> extends BaseCol<T> {

    public StringCol(T table, Field field) {
        super(table, field);
    }

    public T like(String other) {
        table.sql.append(" " + name() + " like ?");
        table.args.add(other);
        return table;
    }
}
