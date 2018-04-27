package top.onceio.core.db.dao;

import java.util.List;
import java.util.function.Consumer;

import top.onceio.core.db.dao.tpl.SelectTpl;
import top.onceio.core.db.dao.tpl.UpdateTpl;

public interface Dao<T> {
	T get(Long id);

	T insert(T entity);

	int batchInsert(List<T> entities);

	int update(T entity);

	int updateIgnoreNull(T entity);

	int updateByTpl(UpdateTpl<T> tpl);

	int updateByTplCnd(UpdateTpl<T> tpl, Cnd<T> cnd);

	int removeById(Long id);

	int removeByIds(List<Long> ids);

	int remove(Cnd<T> cnd);

	int recovery(Cnd<T> cnd);

	int deleteById(Long id);

	int deleteByIds(List<Long> ids);

	int delete(Cnd<T> cnd);

	T fetch(SelectTpl<T> tpl, Cnd<T> cnd);

	List<T> findByIds(List<Long> ids);

	Page<T> find(Cnd<T> cnd);

	Page<T> findTpl(SelectTpl<T> tpl, Cnd<T> cnd);

	void download(SelectTpl<T> tpl, Cnd<T> cnd, Consumer<T> consumer);

	long count();

	long count(Cnd<T> cnd);
}
