package top.onceio.core.db.meta;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import top.onceio.OnceIO;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Constraint;
import top.onceio.core.db.annotation.ConstraintType;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.annotation.TblView;
import top.onceio.core.exception.VolidateFailed;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

public class TableMeta {
	private static final Logger LOGGER = Logger.getLogger(TableMeta.class);
	String table;
	String extend;
	String entityName;
	ConstraintMeta primaryKey;
	transient List<ConstraintMeta> fieldConstraint = new ArrayList<>(0);
	List<ConstraintMeta> constraints;
	List<ColumnMeta> columnMetas = new ArrayList<>(0);
	transient Map<String, ColumnMeta> nameToColumnMeta = new HashMap<>();
	transient DDEngine engine;
	transient Class<?> entity;

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
		freshConstraintMetaTable();
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entity) {
		this.entityName = entity;
	}

	public String getExtend() {
		return extend;
	}

	public void setExtend(String extend) {
		this.extend = extend;
	}

	public List<ColumnMeta> getColumnMetas() {
		return columnMetas;
	}

	public ConstraintMeta getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(ConstraintMeta primaryKey) {
		this.primaryKey = primaryKey;
	}

	public void setPrimaryKey(String primaryKey) {
		ConstraintMeta pk = new ConstraintMeta();
		pk.setTable(this.table);
		pk.setName(String.format("pk_%s_%s", pk.table, primaryKey));
		pk.setColumns(Arrays.asList(primaryKey));
		pk.setType(ConstraintType.PRIMARY_KEY);
		pk.setUsing("BTREE");
		this.primaryKey = pk;
	}

	public List<ConstraintMeta> getFieldConstraint() {
		return fieldConstraint;
	}

	public void setFieldConstraint(List<ConstraintMeta> fieldConstraint) {
		this.fieldConstraint = fieldConstraint;
	}

	public List<ConstraintMeta> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<ConstraintMeta> constraints) {
		this.constraints = constraints;
	}

	// TODO O1
	public ColumnMeta getColumnMetaByName(String colName) {
		for (String name : nameToColumnMeta.keySet()) {
			if (name.equalsIgnoreCase(colName)) {
				return nameToColumnMeta.get(name);
			}
		}
		return null;
	}

	public void setColumnMetas(List<ColumnMeta> columnMetas) {
		this.columnMetas = columnMetas;
		this.nameToColumnMeta = new HashMap<>(columnMetas.size());
		this.fieldConstraint = new ArrayList<>(columnMetas.size());
		for (ColumnMeta cm : columnMetas) {
			this.nameToColumnMeta.put(cm.name, cm);
		}
	}

	public DDEngine getEngine() {
		return engine;
	}

	public Class<?> getEntity() {
		return entity;
	}

	public void freshNameToField() {
		try {
			Class<?> tblEntity = OnceIO.getClassLoader().loadClass(entityName);
			entity = tblEntity;
			List<Class<?>> classes = new ArrayList<>();
			for (Class<?> clazz = tblEntity; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
				classes.add(0, clazz);
			}
			Set<String> missed = new HashSet<>(nameToColumnMeta.keySet());

			for (Class<?> clazz : classes) {
				for (Field field : clazz.getDeclaredFields()) {
					ColumnMeta cm = nameToColumnMeta.get(field.getName());
					if (cm != null) {
						field.setAccessible(true);
						cm.setField(field);
						if (field.getType().equals(field.getGenericType())) {
							cm.setJavaBaseType(field.getType());
						} else {
							Class<?> jbt = OReflectUtil.searchGenType(clazz, classes.get(classes.size() - 1),
									field.getGenericType());
							cm.setJavaBaseType(jbt);
						}
						missed.remove(field.getName());
					}
				}
			}
			if (!missed.isEmpty()) {
				LOGGER.warn(String.format("以下字段没有加载到Field %s", OUtils.toJson(missed)));
			}
		} catch (ClassNotFoundException e) {
			OAssert.fatal("无法加载 %s", entityName);
		}
	}

	public void freshConstraintMetaTable() {
		if (columnMetas != null && !columnMetas.isEmpty()) {
			nameToColumnMeta.clear();
			fieldConstraint = new ArrayList<>(columnMetas.size());
			for (ColumnMeta cm : columnMetas) {
				if (cm.unique) {
					ConstraintMeta cnsMeta = new ConstraintMeta();
					List<String> cols = new ArrayList<>();
					cols.add(cm.getName());
					cnsMeta.setColumns(new ArrayList<String>(cols));
					cnsMeta.setTable(this.getTable());
					cnsMeta.setName("un_" + cnsMeta.getTable() + "_" + cm.name);
					cnsMeta.setUsing(cm.using);
					cnsMeta.setType(ConstraintType.UNIQUE);
					fieldConstraint.add(cnsMeta);
				} else if (cm.useFK && cm.refTable != null) {
					ConstraintMeta cnsMeta = new ConstraintMeta();
					List<String> cols = new ArrayList<>();
					cols.add(cm.getName());
					cnsMeta.setColumns(new ArrayList<String>(cols));
					cnsMeta.setTable(this.getTable());
					cnsMeta.setName("fk_" + cnsMeta.getTable() + "_" + cm.name);
					cnsMeta.setUsing(cm.using);
					cnsMeta.setType(ConstraintType.FOREGIN_KEY);
					cnsMeta.setRefTable(cm.refTable);
					fieldConstraint.add(cnsMeta);
				}
				nameToColumnMeta.put(cm.getName(), cm);
			}
			if (extend != null && !"".equals(extend)) {
				ConstraintMeta cnsMeta = new ConstraintMeta();
				List<String> cols = new ArrayList<>();
				cols.add("id");
				cnsMeta.setColumns(new ArrayList<String>(cols));
				cnsMeta.setTable(this.getTable());
				cnsMeta.setName("fk_" + cnsMeta.getTable() + "_id");
				cnsMeta.setUsing("btree");
				cnsMeta.setType(ConstraintType.FOREGIN_KEY);
				cnsMeta.setRefTable(extend);
				fieldConstraint.add(cnsMeta);
			}
		}
	}

	private List<String> alterColumnSql(List<ColumnMeta> columnMetas) {
		List<String> sqls = new ArrayList<>();
		for (ColumnMeta ocm : columnMetas) {
			sqls.add(String.format("ALTER TABLE %s ALTER %s %s%s;", table, ocm.name, ocm.type,
					ocm.nullable ? "" : " not null"));
		}
		return sqls;
	}

	private List<String> addColumnSql(List<ColumnMeta> columnMetas) {
		List<String> sqls = new ArrayList<>();
		for (ColumnMeta ocm : columnMetas) {
			sqls.add(String.format("ALTER TABLE %s ADD %s %s%s;", table, ocm.name, ocm.type,
					ocm.nullable ? "" : " not null"));
		}
		return sqls;
	}

	/** drop table if exists tbl_a; */
	public List<String> createTableSql() {
		List<String> sqls = new ArrayList<>();
		if (engine == null) {
			StringBuffer tbl = new StringBuffer();
			tbl.append(String.format("CREATE TABLE %s (", table));
			for (ColumnMeta cm : columnMetas) {
				tbl.append(String.format("%s %s%s,", cm.name, cm.type, cm.nullable ? "" : " not null"));
			}
			tbl.delete(tbl.length() - 1, tbl.length());
			tbl.append(");");
			sqls.add(tbl.toString());
			if (primaryKey != null) {
				sqls.add(primaryKey.addSql());
			}
			/** 添加字段约束 */
			sqls.addAll(ConstraintMeta.addConstraintSql(fieldConstraint));

			/** 添加复合约束 */
			sqls.addAll(ConstraintMeta.addConstraintSql(constraints));
		}
		return sqls;
	}

	/**
	 * 升级数据库，返回需要执行的sql
	 * 
	 * @param other
	 * @return
	 */
	public List<String> upgradeTo(TableMeta other) {
		if (!table.equals(other.table)) {
			return null;
		}
		List<String> sqls = new ArrayList<>();
		List<ColumnMeta> otherColumn = other.columnMetas;
		List<ColumnMeta> newColumns = new ArrayList<>();
		List<ConstraintMeta> dropIndexs = new ArrayList<>();
		List<ConstraintMeta> dropForeignKeys = new ArrayList<>();
		List<ColumnMeta> alterColumns = new ArrayList<>();
		List<ConstraintMeta> addForeignKeys = new ArrayList<>();
		for (ColumnMeta ocm : otherColumn) {
			ColumnMeta cm = nameToColumnMeta.get(ocm.name);
			if (cm == null) {
				newColumns.add(ocm);
			} else {
				if (cm.unique && !ocm.unique) {
					ConstraintMeta cnstMeta = new ConstraintMeta();
					cnstMeta.setColumns(Arrays.asList(ocm.getName()));
					cnstMeta.setTable(table);
					cnstMeta.setType(ConstraintType.UNIQUE);
					cnstMeta.setUsing(ocm.getUsing());
					dropIndexs.add(cnstMeta);
				}
				/** 删除外键 */
				if (cm.useFK && !ocm.useFK) {
					ConstraintMeta cnstMeta = new ConstraintMeta();
					cnstMeta.setColumns(Arrays.asList(cm.getName()));
					cnstMeta.setTable(table);
					cnstMeta.setType(ConstraintType.FOREGIN_KEY);
					cnstMeta.setRefTable(cm.getRefTable());
					cnstMeta.setUsing(cm.getUsing());
					dropForeignKeys.add(cnstMeta);
				}
				if (!cm.type.equals(ocm.type) || cm.nullable != ocm.nullable) {
					alterColumns.add(ocm);
				}
				if (!cm.useFK && ocm.useFK) {
					if (ocm.useFK && ocm.refTable != null) {
						ConstraintMeta cnstMeta = new ConstraintMeta();
						cnstMeta.setColumns(Arrays.asList(cm.getName()));
						cnstMeta.setTable(table);
						cnstMeta.setType(ConstraintType.FOREGIN_KEY);
						cnstMeta.setRefTable(cm.getRefTable());
						cnstMeta.setUsing(cm.getUsing());
						addForeignKeys.add(cnstMeta);
					}
				}
			}
		}

		Set<String> oldConstraintSet = new HashSet<String>();
		Set<String> currentSet = new HashSet<String>();

		for (ConstraintMeta tuple : fieldConstraint) {
			oldConstraintSet.add(String.join(",", tuple.columns));
		}
		List<ConstraintMeta> addUniqueConstraint = new ArrayList<>();
		for (ConstraintMeta tuple : other.fieldConstraint) {
			currentSet.add(String.join(",", tuple.columns));
			if (!oldConstraintSet.contains(String.join(",", tuple.columns))) {
				addUniqueConstraint.add(tuple);
			}
		}
		List<ConstraintMeta> dropUniqueConstraint = new ArrayList<>();
		for (ConstraintMeta tuple : fieldConstraint) {
			if (!currentSet.contains(String.join(",", tuple.columns))) {
				dropUniqueConstraint.add(tuple);
			}
		}
		
		oldConstraintSet.clear();
		currentSet.clear();
		
		for (ConstraintMeta tuple : constraints) {
			oldConstraintSet.add(String.join(",", tuple.columns));
		}
		List<ConstraintMeta> addCustomizedConstraint = new ArrayList<>();
		for (ConstraintMeta tuple : other.constraints) {
			currentSet.add(String.join(",", tuple.columns));
			if (!oldConstraintSet.contains(String.join(",", tuple.columns))) {
				addCustomizedConstraint.add(tuple);
			}
		}
		List<ConstraintMeta> dropCustomizedConstraint = new ArrayList<>();
		for (ConstraintMeta tuple : constraints) {
			if (!currentSet.contains(String.join(",", tuple.columns))) {
				dropCustomizedConstraint.add(tuple);
			}
		}
		
		if (primaryKey != null && !primaryKey.equals(other.primaryKey)) {
			sqls.add(primaryKey.dropSql());
		}
		if (other.primaryKey != null && !other.primaryKey.equals(primaryKey)) {
			sqls.add(other.primaryKey.addSql());
		}
		sqls.addAll(addColumnSql(newColumns));

		sqls.addAll(ConstraintMeta.dropConstraintSql(dropIndexs));
		sqls.addAll(ConstraintMeta.dropConstraintSql(dropForeignKeys));
		sqls.addAll(alterColumnSql(alterColumns));
		sqls.addAll(ConstraintMeta.addConstraintSql(addForeignKeys));
		sqls.addAll(ConstraintMeta.dropConstraintSql(dropUniqueConstraint));
		sqls.addAll(ConstraintMeta.addConstraintSql(addUniqueConstraint));
		sqls.addAll(ConstraintMeta.dropConstraintSql(dropCustomizedConstraint));
		sqls.addAll(ConstraintMeta.addConstraintSql(addCustomizedConstraint));
		return sqls;
	}

	public static final Map<Class<?>,TableMeta> tableCache = new HashMap<>();
	
	public static TableMeta createBy(Class<?> entity) {
		TableMeta tm = new TableMeta();
		tm.table = entity.getSimpleName().toLowerCase();
		tm.entityName = entity.getName();
		tm.entity = entity;
		Tbl tbl = entity.getAnnotation(Tbl.class);
		TblView tblView = entity.getAnnotation(TblView.class);
		if (tbl != null) {
			List<ConstraintMeta> constraints = new ArrayList<>();
			for (Constraint c : tbl.constraints()) {
				ConstraintMeta cm = new ConstraintMeta();
				constraints.add(cm);
				cm.setColumns(Arrays.asList(c.colNames()));
				cm.setTable(tm.getTable());
				cm.setType(c.type());
				cm.setUsing(c.using());
			}
			tm.setConstraints(constraints);
			if (!tbl.extend().equals(void.class)) {
				tm.setExtend(tbl.extend().getSimpleName().toLowerCase());
			}
		}
		List<Class<?>> classes = new ArrayList<>();
		for (Class<?> clazz = entity; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
			classes.add(0, clazz);
		}

		List<ColumnMeta> columnMetas = new ArrayList<>();
		List<String> colOrder = new ArrayList<>();
		for (Class<?> clazz : classes) {
			for (Field field : clazz.getDeclaredFields()) {
				Col col = field.getAnnotation(Col.class);
				if (col == null) {
					continue;
				}
				ColumnMeta cm = new ColumnMeta();
				cm.setName(field.getName());
				if (field.getName().equals("id")) {
					cm.setPrimaryKey(true);
				}
				cm.setNullable(col.nullable());
				cm.setPattern(col.pattern());
				if (col.colDef().equals("")) {
					Class<?> javaBaseType = cm.getJavaBaseType();
					if (javaBaseType == null) {
						if (field.getType() == Object.class) {
							javaBaseType = OReflectUtil.searchGenType(clazz, classes.get(classes.size() - 1),
									field.getGenericType());
							cm.setJavaBaseType(javaBaseType);
						} else {
							javaBaseType = field.getType();
							cm.setJavaBaseType(javaBaseType);
						}
					}
					String type = transType(clazz, entity, javaBaseType, col);
					cm.setType(type);
				} else {
					cm.setType(col.colDef());
				}
				cm.setUnique(col.unique());
				cm.setUsing(col.using());
				if (col.ref() != void.class) {
					cm.setUseFK(col.useFK());
					cm.setRefTable(col.ref().getSimpleName().toLowerCase());
				}
				int index = colOrder.indexOf(cm.getName());
				if (index < 0) {
					colOrder.add(cm.getName());
					columnMetas.add(cm);
				} else {
					columnMetas.set(index, cm);
				}
			}
		}
		tm.setColumnMetas(columnMetas);
		tm.setPrimaryKey("id");
		tm.freshNameToField();
		tm.freshConstraintMetaTable();
		if (tblView != null && classes.size() >= 3) {
			if (classes.size() >= 3) {
				String mainClazz = classes.get(1).getSimpleName().toLowerCase();
				DDEngine dde = new DDEngine();
				tm.engine = dde;
				Map<String, List<String>> pathToColumns = new HashMap<>();
				for (ColumnMeta cm : tm.getColumnMetas()) {
					Col col = cm.getField().getAnnotation(Col.class);
					String pathName = col.refBy();
					int sp = pathName.lastIndexOf('.');
					String path = null;
					String name = null;
					if (sp >= 0) {
						path = mainClazz + "." + pathName.substring(0, sp);
						name = pathName.substring(sp + 1);
					} else if (pathName.equals("")) {
						path = mainClazz;
						name = "";
					} else {
						OAssert.warnning("不合法的引用", path);
					}
					List<String> vals = pathToColumns.get(path);
					if (vals == null) {
						vals = new ArrayList<>();
						pathToColumns.put(path, vals);
					}
					if (path.equals("")) {
						vals.add(cm.getName());
					} else {
						vals.add(name + " " + cm.getName());
					}
				}
				for (String path : pathToColumns.keySet()) {
					dde.append(String.format("%s{%s}", path, String.join(",", pathToColumns.get(path))));
				}
				dde.build();
			} else {
				OAssert.warnning("Tbl必须继承一个Tbl", tm.getEntityName());
				return null;
			}
		}
		tableCache.put(entity, tm);
		return tm;
	}

	/**
	 * 以postgresql為准
	 */
	private static String transType(Class<?> forefather, Class<?> clazz, Class<?> type, Col col) {
		if (type.equals(Long.class) || type.equals(long.class)) {
			return "bigint";
		} else if (type.equals(String.class)) {
			return String.format("varchar(%d)", col.size());
		} else if (type.equals(Integer.class) || type.equals(int.class)) {
			return "integer";
		} else if (type.equals(BigDecimal.class)) {
			return String.format("decimal(%d,%d)", col.precision(), col.scale());
		} else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			return "boolean";
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			return "smallint";
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			return "float";
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			return "double precision";
		} else {
			OAssert.fatal("不支持的数据类型:%s", type);
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnMetas == null) ? 0 : columnMetas.hashCode());
		result = prime * result + ((constraints == null) ? 0 : constraints.hashCode());
		result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
		result = prime * result + ((extend == null) ? 0 : extend.hashCode());
		result = prime * result + ((fieldConstraint == null) ? 0 : fieldConstraint.hashCode());
		result = prime * result + ((primaryKey == null) ? 0 : primaryKey.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableMeta other = (TableMeta) obj;
		if (columnMetas == null) {
			if (other.columnMetas != null)
				return false;
		} else if (!columnMetas.equals(other.columnMetas))
			return false;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		if (entityName == null) {
			if (other.entityName != null)
				return false;
		} else if (!entityName.equals(other.entityName))
			return false;
		if (extend == null) {
			if (other.extend != null)
				return false;
		} else if (!extend.equals(other.extend))
			return false;
		if (fieldConstraint == null) {
			if (other.fieldConstraint != null)
				return false;
		} else if (!fieldConstraint.equals(other.fieldConstraint))
			return false;
		if (primaryKey == null) {
			if (other.primaryKey != null)
				return false;
		} else if (!primaryKey.equals(other.primaryKey))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		return true;
	}


	public void validate(Object obj, boolean ignoreNull) {
		for (ColumnMeta cm : this.getColumnMetas()) {
			if (cm.getName().equals("id") || cm.getName().equals("rm")) {
				continue;
			}
			Object val = null;
			try {
				val = cm.getField().get(obj);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
			if (!cm.isNullable() && val == null && !ignoreNull) {
				VolidateFailed vf = VolidateFailed.createError("%s cannot be null", cm.getName());
				vf.put(cm.getName(), "cannot be null");
				vf.throwSelf();
			} else if (val != null) {
				if (!cm.getPattern().equals("")) {
					if (val.toString().matches(cm.getPattern())) {
						VolidateFailed vf = VolidateFailed.createError("%s does not matches %s", cm.getName(),
								cm.getPattern());
						vf.put(cm.getName(), cm.getPattern());
						vf.throwSelf();
					}
				}
			}
		}

	}
}
