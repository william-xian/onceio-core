package top.onceio.core.db.dao;

import java.util.List;

import javax.sql.DataSource;

/**
 * @author William
 */
public interface DataSourceManager {

    public List<DataSource> getDataSource(Class<?> entityClass);

    Long nextId(Class<?> entityClass);
}
