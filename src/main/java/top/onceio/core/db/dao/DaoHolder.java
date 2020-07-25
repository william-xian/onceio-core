package top.onceio.core.db.dao;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

import top.onceio.core.annotation.Using;
import top.onceio.core.beans.ApiMethod;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.DaoHelper;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.Param;
import top.onceio.core.util.OReflectUtil;

public abstract class DaoHolder<T extends BaseEntity, M extends BaseEntity.Meta> implements Dao<T, M> {
    @Using
    protected DaoHelper daoHelper;

    private Class<T> tbl;
    private Class<M> mode;

    @SuppressWarnings("unchecked")
    public DaoHolder() {
        Type t = DaoHolder.class.getTypeParameters()[0];
        tbl = (Class<T>) OReflectUtil.searchGenType(DaoHolder.class, this.getClass(), t);
        Type m = DaoHolder.class.getTypeParameters()[1];
        mode = (Class<M>) OReflectUtil.searchGenType(DaoHolder.class, this.getClass(), m);
    }

    public DaoHelper getDaoHelper() {
        return daoHelper;
    }

    public void setDaoHelper(DaoHelper daoHelper) {
        this.daoHelper = daoHelper;
    }

    @Api(value = "/{id}", method = ApiMethod.GET)
    @Override
    public T get(@Param("id") Long id) {
        return daoHelper.get(tbl, id);
    }

    @Api(value = "/", method = ApiMethod.POST)
    @Override
    public T insert(@Param T entity) {
        return daoHelper.insert(entity);
    }

    @Api(value = "/batch", method = ApiMethod.POST)
    @Override
    public int batchInsert(@Param List<T> entities) {
        return daoHelper.batchInsert(entities);
    }

    @Override
    public int update(@Param T entity) {
        return daoHelper.update(entity);
    }

    @Api(value = "/{id}", method = ApiMethod.PUT)
    public T save(@Param T entity) {
        T e = this.get(entity.getId());
        if (e == null) {
            return daoHelper.insert(entity);
        } else {
            daoHelper.updateIgnoreNull(entity);
            return this.get(entity.getId());
        }
    }

    @Api(value = "/", method = ApiMethod.PATCH)
    @Override
    public int updateIgnoreNull(@Param T entity) {
        return daoHelper.updateIgnoreNull(entity);
    }

    @Api(value = "/by", method = ApiMethod.PATCH)
    @Override
    public int updateBy(BaseTable<M> tpl) {
        return daoHelper.updateBy(tbl, tpl);
    }

    @Override
    public int deleteById(Long id) {
        return daoHelper.deleteById(tbl, id);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return daoHelper.deleteByIds(tbl, ids);
    }

    @Override
    public int delete(BaseTable<M> cnd) {
        return daoHelper.delete(tbl, cnd);
    }

    @Api(value = "/fetch", method = {ApiMethod.GET})
    @Override
    public T fetch(@Param("tpl") BaseTable<M> tpl) {
        return daoHelper.fetch(tbl, tpl);
    }

    @Api(value = "/byIds", method = {ApiMethod.GET})
    @Override
    public List<T> findByIds(@Param("ids") List<Long> ids) {
        return daoHelper.findByIds(tbl, ids);
    }

    @Override
    public List<T> find(@Param("cnd") BaseTable<M> cnd) {
        return daoHelper.find(tbl, cnd);
    }

    @Api(value = "/", method = {ApiMethod.GET})
    @Override
    public Page<T> find(@Param("cnd") BaseTable<M> cnd, @Param("page") int page, @Param("pageSize") int pageSize) {
        return daoHelper.find(tbl, cnd, page, pageSize);
    }

    @Override
    public void find(BaseTable<M> cnd, Consumer<T> consumer) {
        daoHelper.find(tbl, cnd, consumer);
    }

    @Override
    public long count() {
        return daoHelper.count(tbl);
    }

    @Api(value = "/count", method = {ApiMethod.GET})
    @Override
    public long count(@Param("cnd") BaseTable<M> cnd) {
        return daoHelper.count(tbl, cnd);
    }

}
