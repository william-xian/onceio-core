package top.onceio.core.db.dao;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

import top.onceio.core.annotation.Using;
import top.onceio.core.beans.HttpMethod;
import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.db.model.DaoHelper;
import top.onceio.core.db.model.BaseModel;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.Param;
import top.onceio.core.util.OReflectUtil;

public class DaoHolder<E extends BaseModel> implements Dao<E> {
    @Using
    protected DaoHelper daoHelper;

    private Class<E> tbl;

    @SuppressWarnings("unchecked")
    public DaoHolder() {
        Type t = DaoHolder.class.getTypeParameters()[0];
        tbl = (Class<E>) OReflectUtil.searchGenType(DaoHolder.class, this.getClass(), t);
    }

    public DaoHelper getDaoHelper() {
        return daoHelper;
    }

    public void setDaoHelper(DaoHelper daoHelper) {
        this.daoHelper = daoHelper;
    }

    @Api(value = "/{id}", method = HttpMethod.GET)
    @Override
    public E get(@Param("id") Serializable id) {
        return daoHelper.get(tbl, id);
    }

    @Api(value = "/", method = HttpMethod.POST)
    @Override
    public E insert(@Param E entity) {
        return daoHelper.insert(entity);
    }

    @Api(value = "/batch", method = HttpMethod.POST)
    @Override
    public int batchInsert(@Param List<E> entities) {
        return daoHelper.batchInsert(entities);
    }

    @Override
    public int update(E entity) {
        return daoHelper.update(entity);
    }

    @Api(value = "/{id}", method = HttpMethod.PUT)
    public E save(@Param E entity) {
        E e = this.get(entity.getId());
        if (e == null) {
            return daoHelper.insert(entity);
        } else {
            daoHelper.updateIgnoreNull(entity);
            return this.get(entity.getId());
        }
    }

    @Api(value = "/", method = HttpMethod.PATCH)
    @Override
    public int updateIgnoreNull(@Param E entity) {
        return daoHelper.updateIgnoreNull(entity);
    }

    @Override
    public <M extends BaseMeta<M>> int updateBy(M tpl) {
        return daoHelper.updateBy(tbl, tpl);
    }

    @Override
    @Api(value = "/", method = HttpMethod.DELETE)
    public <M extends BaseMeta<M>> int delete(@Param M cnd) {
        return daoHelper.delete(tbl, cnd);
    }

    @Override
    public <M extends BaseMeta<M>> E fetch(M tpl) {
        return daoHelper.fetch(tbl, tpl);
    }

    @Override
    public <ID extends Serializable> List<E> findByIds(List<ID> ids) {
        return daoHelper.findByIds(tbl, ids);
    }

    @Override
    public <M extends BaseMeta<M>> List<E> find(M cnd) {
        return daoHelper.find(tbl, cnd);
    }

    @Api(value = "/", method = HttpMethod.GET)
    @Override
    public <M extends BaseMeta<M>> Page<E> find(@Param M cnd, @Param("$page") int page, @Param("$pageSize") int pageSize) {
        return daoHelper.find(tbl, cnd, page, pageSize);
    }

    @Override
    public <M extends BaseMeta<M>> void find(M cnd, Consumer<E> consumer) {
        daoHelper.find(tbl, cnd, consumer);
    }

    @Override
    public long count() {
        return daoHelper.count(tbl);
    }

    @Api(value = "/count", method = HttpMethod.GET)
    @Override
    public <M extends BaseMeta<M>> long count(@Param M cnd) {
        return daoHelper.count(tbl, cnd);
    }

}
