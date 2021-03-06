package top.onceio.core.db.model;

import java.lang.reflect.Field;

public class StringCol<T extends BaseMeta> extends BaseCol<T> {

    public StringCol(T table, Field field) {
        super(table, field);
    }

    public T like(String other) {
        table.where.append(" " + name() + " like ?");
        table.args.add(other);
        return table;
    }

    public T notLike(String other) {
        table.where.append(" " + name() + " NOT like ?");
        table.args.add(other);
        return table;
    }
}
