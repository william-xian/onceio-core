package top.onceio.core.db.dao;

import java.util.List;

import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.db.tbl.OEntity;

public interface DDLDao {

    public <E extends OEntity> boolean drop(Class<E> tbl);

    public int[] batchUpdate(final String... sql);

    public int[] batchUpdate(final String sql, List<Object[]> batchArgs);

    /**
     * @param sql
     * @param args
     * @return list[?>0]:row data
     */
    public List<Object[]> call(String sql, Object[] args);
}
