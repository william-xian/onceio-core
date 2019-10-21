package top.onceio.core.db.dao;

public interface IdGenerator {
    Long next(Class<?> entityClass);
}
