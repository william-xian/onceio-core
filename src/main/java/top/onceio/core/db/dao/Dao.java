package top.onceio.core.db.dao;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.mvc.annocations.Param;

public interface Dao<E> {
    /**
     * 根据id获取对象
     * <b>即使数据已经被逻辑删除依然能够获取到。</b>
     *
     * @param id 主键
     * @return 单个记录
     */
    <ID extends Serializable> E get(ID id);

    /**
     * 插入数据
     * <b>如果entity的id为null，会自动回填id</b>
     *
     * @param entity 实体
     * @return 插入数据的数目
     */
    E insert(E entity);

    /**
     * 批量插入数据
     * <b>如果entity的id为null，会自动回填id</b>
     *
     * @param entities 实体列表
     * @return 成功插入的个数
     */
    int batchInsert(List<E> entities);

    /**
     * 更新数据
     * <b>entity的id不可为null</b>
     *
     * @param entity 实体
     * @return 更新的数目
     */
    int update(E entity);

    /**
     * 将非null数据更新到数据哭
     * <b>entity的id不可为null</b>
     *
     * @param entity 实体
     * @return 更新的数目
     */
    int updateIgnoreNull(E entity);

    /**
     * 根据tpl中的id，特殊的更新数据，如count++等自操作更新等等。
     *
     * @param tpl 更新模板
     * @return 更新的数目
     */
    <M extends BaseMeta<M>> int updateBy(M tpl);

    /**
     * 根据条件物理删除数据
     *
     * @param cnd <b>null值代表不删除</b>
     * @return 删除的条数
     */
    <M extends BaseMeta<M>> int delete(M cnd);

    /**
     * 返回匹配到第一条符合条件的数据
     * <b>（默认是没有被删除的数据）</b>,如果获取
     *
     * @param cnd null值代表不限定条件
     * @return 返回第一条数据
     */
    <M extends BaseMeta<M>> E fetch(M cnd);

    /**
     * 返回没有被逻辑删除的，给定ids范围内的数据
     *
     * @param ids 主键列表
     * @return 列表
     */
    <ID extends Serializable> List<E> findByIds(List<ID> ids);

    /**
     * 根据条件筛选数据
     * <b>注意： 分页页码从0开始，并且总分页数只有再页号非正数时才会返回，如果是第1页想获取中数据数，则page传入-1即可</b>
     *
     * @param cnd null值代表不限制
     * @return 返回分页数据
     */
    <M extends BaseMeta<M>> List<E> find(M cnd);


    <M extends BaseMeta<M>> Page<E> find(@Param("cnd") M cnd, @Param("page") int page, @Param("pageSize") int pageSize);

    /**
     * 根据筛选条件，将数据依次传给consumer处理
     *
     * @param cnd 条件
     * @param consumer 回调处理
     */
    <M extends BaseMeta<M>> void find(M cnd, Consumer<E> consumer);

    /**
     * 所有数据
     *
     * @return 记录总数
     */
    long count();

    /**
     * 根据筛选条件查询数据个数
     *
     * @param cnd 条件
     * @return 筛选到的数据个数
     */
    <M extends BaseMeta<M>> long count(M cnd);
}
