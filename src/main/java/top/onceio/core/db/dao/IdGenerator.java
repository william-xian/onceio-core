package top.onceio.core.db.dao;

import java.io.Serializable;

public interface IdGenerator {
    Serializable next(Class<?> entityClass);
}
