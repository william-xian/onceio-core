package top.onceio.core.db.dao;

import java.util.List;

import top.onceio.core.db.model.BaseModel;

public interface DDLDao {

    <E extends BaseModel> boolean drop(Class<E> tbl);

    int[] batchUpdate(final String... sql);

    int[] batchUpdate(final String sql, List<Object[]> batchArgs);

    /**
     * 返回数据中list[0] 是字段名，list[1-n]是字段所对应的数据
     * @param sql 使用替代符的SQL语句
     * @param args SQL参数列表
     * @return list[n]:row data list[0] is the columnNames,list[1] is the first row data of thus columns.
    */
    List<Object[]> call(String sql, Object[] args);
}
