package top.onceio.core.db.dao;

import java.util.List;
import java.util.function.Consumer;

import top.onceio.core.db.model.BaseTable;
import top.onceio.core.mvc.annocations.Param;

public interface Dao<T, M> {
    /**
     * 根据id获取对象
     * <b>即使数据已经被逻辑删除依然能够获取到。</b>
     *
     * @param id
     * @return 单个记录
     */
    T get(Long id);

    /**
     * 插入数据
     * <b>如果entity的id为null，会自动回填id</b>
     *
     * @param entity
     * @return 插入数据的数目
     */
    T insert(T entity);

    /**
     * 批量插入数据
     * <b>如果entity的id为null，会自动回填id</b>
     *
     * @param entities
     * @return 成功插入的个数
     */
    int batchInsert(List<T> entities);

    /**
     * 更新数据
     * <b>entity的id不可为null</b>
     *
     * @param entity
     * @return 更新的数目
     */
    int update(T entity);

    /**
     * 将非null数据更新到数据哭
     * <b>entity的id不可为null</b>
     *
     * @param entity
     * @return 更新的数目
     */
    int updateIgnoreNull(T entity);

    /**
     * 根据tpl中的id，特殊的更新数据，如count++等自操作更新等等。
     *
     * @param tpl 更新模板
     * @return 更新的数目
     */
    int updateBy(BaseTable<M> tpl);

    int deleteById(Long id);

    /**
     * 根据主键物理删除数据
     *
     * @param ids
     * @return 删除的条数
     */
    int deleteByIds(List<Long> ids);

    /**
     * 根据条件物理删除数据
     *
     * @param cnd <b>null值代表不删除</b>
     * @return 删除的条数
     */
    int delete(BaseTable<M> cnd);

    /**
     * 返回匹配到第一条符合条件的数据
     * <b>（默认是没有被删除的数据）</b>,如果获取
     *
     * @param cnd null值代表不限定条件
     * @return 返回第一条数据
     */
    T fetch(BaseTable<M> cnd);

    /**
     * 返回没有被逻辑删除的，给定ids范围内的数据
     *
     * @param ids
     * @return 列表
     */
    List<T> findByIds(List<Long> ids);

    /**
     * 根据条件筛选数据
     * <b>注意： 分页页码从0开始，并且总分页数只有再页号非正数时才会返回，如果是第1页想获取中数据数，则page传入-1即可</b>
     *
     * @param cnd null值代表不限制
     * @return 返回分页数据
     */
    List<T> find(BaseTable<M> cnd);


    Page<T> find(@Param("cnd")BaseTable<M> cnd, @Param("page") int page, @Param("pageSize") int pageSize);

    /**
     * 根据筛选条件，将数据依次传给consumer处理
     *
     * @param cnd
     * @param consumer 回调处理
     */
    void find(BaseTable<M> cnd, Consumer<T> consumer);

    /**
     * 所有数据
     *
     * @return 记录总数
     */
    long count();

    /**
     * 根据筛选条件查询数据个数
     *
     * @param cnd
     * @return 筛选到的数据个数
     */
    long count(BaseTable<M> cnd);
}
