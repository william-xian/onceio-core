package top.onceio.core.db.dao;

import java.sql.Savepoint;

public interface TransDao {
    void beginTransaction(int level, boolean readOnly);

    Savepoint setSavepoint();

    void rollback();

    void rollback(Savepoint sp);

    void commit();
}
