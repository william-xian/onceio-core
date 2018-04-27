package top.onceio.core.db.dao.impl;

import java.util.ArrayList;
import java.util.List;

import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.util.OAssert;

public class TblIdNameVal<E> {
	List<Object> ids;
	List<String> names;
	List<List<Object>> valsList;

	public TblIdNameVal(List<ColumnMeta> columnMetas, List<E> entities) {
		ids = new ArrayList<>(columnMetas.size());
		names = new ArrayList<>(columnMetas.size());
		valsList = new ArrayList<>(entities.size());
		boolean hasNames = false;
		for (E entity : entities) {
			if (entity == null)
				continue;
			List<Object> vals = new ArrayList<>(columnMetas.size());
			valsList.add(vals);
			for (ColumnMeta cm : columnMetas) {
				if (!hasNames) {
					if (!cm.isPrimaryKey()) {
						names.add(cm.getName());
					}
				}
				try {
					Object val = cm.getField().get(entity);
					if (cm.isPrimaryKey()) {
						ids.add(val);
					} else {
						vals.add(val);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					OAssert.warnning("%s.%s 访问异常:%s", entity.getClass().getSimpleName(), cm.getName(), e.getMessage());
				}
			}
			hasNames = true;
		}
	}

	public Object getIdAt(int index) {
		return ids.get(0);
	}

	public Object setIdAt(int index, Object val) {
		return ids.set(index, val);
	}

	public List<String> getNames() {
		return names;
	}

	public List<String> getIdNames() {
		List<String> idNames = new ArrayList<>();
		idNames.add("id");
		idNames.addAll(names);
		return idNames;
	}

	public List<List<Object>> getValsList() {
		return valsList;
	}

	public List<List<Object>> getIdValsList() {
		List<List<Object>> idValsList = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			Object id = ids.get(i);
			List<Object> row = valsList.get(i);
			List<Object> idRow = new ArrayList<>(row.size() + 1);
			idRow.add(id);
			idRow.addAll(row);
			idValsList.add(idRow);
		}
		return idValsList;
	}

	public void dropAllNullColumns() {
		List<Integer> nullColumnsIndex = new ArrayList<>();
		OUTER: for (Integer i = names.size() - 1; i >= 0; i--) {
			for (List<Object> row : valsList) {
				if (row.get(i) != null) {
					continue OUTER;
				}
			}
			nullColumnsIndex.add(i);
		}
		for (Integer j : nullColumnsIndex) {
			names.remove((int) j);
			for (List<Object> row : valsList) {
				row.remove((int) j);
			}
		}
	}

	public void dropColumns(String colName) {
		int rm = -1;
		for (int i = 0; i < names.size(); i++) {
			if (names.get(i).equals(colName)) {
				rm = i;
				break;
			}
		}
		if (rm >= 0) {
			names.remove(rm);
			for (List<Object> row : valsList) {
				row.remove(rm);
			}
		}
	}
}