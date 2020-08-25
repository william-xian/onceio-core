package top.onceio.core.db.meta;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import org.apache.log4j.Logger;

import top.onceio.core.db.annotation.*;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.DefView;
import top.onceio.core.exception.VolidateFailed;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

public class TableMeta {
    private static final Logger LOGGER = Logger.getLogger(TableMeta.class);
    String table;
    BaseTable viewDef;
    TblType type;

    transient List<IndexMeta> fieldConstraint = new ArrayList<>(0);
    List<IndexMeta> indexes = new ArrayList<>();
    List<ColumnMeta> columnMetas = new ArrayList<>(0);
    transient Map<String, ColumnMeta> nameToColumnMeta = new HashMap<>();
    transient Class<?> entity;

    public static final Map<Class<?>, TableMeta> tableCache = new HashMap<>();

    public String name() {
        return table;
    }

    public static String getTableName(Class<?> clazz) {
        String defaultName = clazz.getSimpleName().replaceAll("([A-Z])", "_$1").toLowerCase();
        if (defaultName.startsWith("_")) {
            defaultName = defaultName.substring(1);
        }
        Tbl tbl = clazz.getAnnotation(Tbl.class);
        if (tbl != null && !tbl.name().equals("")) {
            return tbl.name().toLowerCase().replace("public.","");
        } else {
            return defaultName;
        }
    }

    public static String getColumnName(Field field) {
        String defaultName = field.getName().replaceAll("([A-Z])", "_$1").toLowerCase();
        Col col = field.getAnnotation(Col.class);
        if (col != null && !"".equals(col.name())) {
            return col.name();
        } else {
            return defaultName;
        }
    }

    public static TableMeta createBy(Class<?> entity) {
        TableMeta tm = new TableMeta();
        tm.entity = entity;
        Tbl tbl = entity.getAnnotation(Tbl.class);
        tm.table = getTableName(entity);
        tm.type = tbl.type();
        if (tbl.type().equals(TblType.TABLE)) {
            List<IndexMeta> constraints = new ArrayList<>();
            for (Index c : tbl.indexes()) {
                IndexMeta cm = new IndexMeta();
                constraints.add(cm);
                cm.setColumns(Arrays.asList(c.columns()));
                cm.setTable(tm.getTable());
                if (c.unique()) {
                    cm.setType(IndexType.UNIQUE_INDEX);
                } else {
                    cm.setType(IndexType.INDEX);
                }
                cm.setUsing(c.using());
            }
            tm.setIndexes(constraints);
        } else if (DefView.class.isAssignableFrom(entity)) {
            try {
                DefView view = (DefView) (entity.newInstance());
                tm.viewDef = view.def();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
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
                cm.setName(getColumnName(field));
                cm.setUnique(col.unique());
                cm.setUsing(col.using());
                cm.setDefaultValue(col.defaultValue());
                cm.setComment(col.comment());
                cm.setNullable(col.nullable());
                if (getColumnName(field).equals("id")) {
                    cm.setPrimaryKey(true);
                    cm.setUnique(true);
                    cm.setNullable(false);
                    if (tbl.extend() != void.class) {
                        cm.setUseFK(col.useFK());
                        String table = getTableName(tbl.extend());
                        cm.setRefTable(table);
                    }
                }
                cm.setPattern(col.pattern());
                if (col.type().equals("")) {
                    Class<?> javaBaseType = cm.getJavaBaseType();
                    if (javaBaseType == null) {
                        if (field.getType() == Object.class) {
                            javaBaseType = OReflectUtil.searchGenType(clazz, classes.get(classes.size() - 1),
                                    field.getGenericType());
                        } else {
                            javaBaseType = field.getType();
                        }
                        cm.setJavaBaseType(javaBaseType);
                    }
                    String type = transType(javaBaseType, col);
                    cm.setType(type);
                } else {
                    cm.setType(col.type().toLowerCase());
                }

                if (col.ref() != void.class) {
                    cm.setUseFK(col.useFK());
                    String table = getTableName(col.ref());
                    cm.setRefTable(table);
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
        tm.freshNameToField(entity);
        tm.freshConstraintMetaTable();

        tableCache.put(entity, tm);
        return tm;
    }

    /**
     * Java类型转换成postgres字段类型
     * @param type Java类型
     * @param col 字段属性
     * @return  postgres字段类型
     */
    public static String transType(Class<?> type, Col col) {
        if (type.equals(Long.class) || type.equals(long.class)) {
            return "int8";
        } else if (type.equals(String.class)) {
            return String.format("varchar(%d)", col.size());
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return "int4";
        } else if (type.equals(BigDecimal.class)) {
            return String.format("numeric(%d,%d)", col.precision(), col.scale());
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "bool";
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            return "int2";
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            return "float4";
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return "float8";
        } else if (type.equals(Timestamp.class)) {
            return "timestamptz";
        } else {
            OAssert.fatal("不支持的数据类型:%s", type);
        }
        return null;
    }

    public static Class<?> parseType(String udtName, int size, int i) {
        if (udtName.equals("bool")) {
            return Boolean.TYPE;
        } else if (udtName.equals("int2")) {
            return Short.TYPE;
        } else if (udtName.equals("int4")) {
            return Integer.TYPE;
        } else if (udtName.equals("int8")) {
            return Long.TYPE;
        } else if (udtName.equals("float4")) {
            return Float.TYPE;
        } else if (udtName.equals("float8")) {
            return Double.TYPE;
        } else if (udtName.equals("numeric")) {
            return BigDecimal.class;
        } else if (udtName.equals("varchar")) {
            return String.class;
        } else if (udtName.equals("text")) {
            return String.class;
        } else if (udtName.equals("timestamptz") || udtName.equals("timestamp")) {
            return Timestamp.class;
        } else if (udtName.equals("time") || udtName.equals("date")) {
            return Date.class;
        }
        return Object.class;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
        freshConstraintMetaTable();
    }

    public List<ColumnMeta> getColumnMetas() {
        return columnMetas;
    }

    public BaseTable getViewDef() {
        return viewDef;
    }


    public List<IndexMeta> getFieldConstraint() {
        return fieldConstraint;
    }

    public void setFieldConstraint(List<IndexMeta> fieldConstraint) {
        this.fieldConstraint = fieldConstraint;
    }

    public List<IndexMeta> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<IndexMeta> indexes) {
        this.indexes = indexes;
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

    public void freshNameToField(Class<?> tblEntity) {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> clazz = tblEntity; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            classes.add(0, clazz);
        }
        Set<String> missed = new HashSet<>(nameToColumnMeta.keySet());

        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                ColumnMeta cm = nameToColumnMeta.get(getColumnName(field));
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
                    missed.remove(getColumnName(field));
                }
            }
        }
        if (!missed.isEmpty()) {
            LOGGER.warn(String.format("以下字段没有加载到Field %s", OUtils.toJson(missed)));
        }
    }

    public void freshConstraintMetaTable() {
        if (columnMetas != null && !columnMetas.isEmpty()) {
            nameToColumnMeta.clear();
            fieldConstraint = new ArrayList<>(columnMetas.size());
            for (ColumnMeta cm : columnMetas) {
                if (cm.isPrimaryKey()) {
                    IndexMeta cnsMeta = new IndexMeta();
                    List<String> cols = new ArrayList<>();
                    cols.add(cm.getName());
                    cnsMeta.setColumns(new ArrayList<String>(cols));
                    cnsMeta.setTable(this.getTable());
                    cnsMeta.setName(IndexMeta.INDEX_NAME_PREFIX_PK + IndexMeta.indexName(cnsMeta.table) + "_" + cm.name);
                    cnsMeta.setUsing(cm.using);
                    cnsMeta.setType(IndexType.PRIMARY_KEY);
                    fieldConstraint.add(cnsMeta);
                } else if (cm.unique) {
                    IndexMeta cnsMeta = new IndexMeta();
                    List<String> cols = new ArrayList<>();
                    cols.add(cm.getName());
                    cnsMeta.setColumns(new ArrayList<String>(cols));
                    cnsMeta.setTable(this.getTable());
                    cnsMeta.setName(IndexMeta.INDEX_NAME_PREFIX_UN + IndexMeta.indexName(cnsMeta.table) + "_" + cm.name);
                    cnsMeta.setUsing(cm.using);
                    cnsMeta.setType(IndexType.UNIQUE_FIELD);
                    fieldConstraint.add(cnsMeta);
                } else if (cm.useFK && cm.refTable != null) {
                    IndexMeta cnsMeta = new IndexMeta();
                    List<String> cols = new ArrayList<>();
                    cols.add(cm.getName());
                    cnsMeta.setColumns(new ArrayList<String>(cols));
                    cnsMeta.setTable(this.getTable());
                    cnsMeta.setName(IndexMeta.INDEX_NAME_PREFIX_FK + IndexMeta.indexName(cnsMeta.table) + "_" + cm.name);
                    cnsMeta.setUsing(cm.using);
                    cnsMeta.setType(IndexType.FOREIGN_KEY);
                    cnsMeta.setRefTable(cm.refTable);
                    fieldConstraint.add(cnsMeta);
                }
                nameToColumnMeta.put(cm.getName(), cm);
            }
        }
    }

    private List<String> alterColumnSql(List<ColumnMeta> columnMetas) {
        List<String> sqls = new ArrayList<>();
        for (ColumnMeta ocm : columnMetas) {
            String sql = String.format("ALTER TABLE %s ALTER COLUMN %s TYPE %s", table, ocm.name, ocm.type);
            if (!ocm.nullable) {
                sql = sql + String.format(", ALTER COLUMN %s SET NOT NULL", ocm.name);
            }
            sql = sql + ";";
            sqls.add(sql);
        }
        return sqls;
    }

    private List<String> addColumnSql(List<ColumnMeta> columnMetas) {
        List<String> sqls = new ArrayList<>();
        for (ColumnMeta ocm : columnMetas) {
            String sql = String.format("ALTER TABLE %%s ADD COLUMN %s %s", table, ocm.name, ocm.type);
            if (!ocm.nullable) {
                sql = sql + String.format(" NOT NULL");
            }
            if (ocm.defaultValue != null && !ocm.defaultValue.equals("")) {
                String dft = "";
                if (ocm.getJavaBaseType().equals(String.class)) {
                    dft = " DEFAULT '" + ocm.defaultValue + "'";
                } else {
                    dft = " DEFAULT " + ocm.defaultValue;
                }
                sql = sql + dft;
            }
            sql = sql + ";";
            sqls.add(sql);
        }
        return sqls;
    }

    /**
     * @return SQL执行计划Builder
     */
    public SqlPlanBuilder createTableSql() {
        SqlPlanBuilder planBuilder = new SqlPlanBuilder();
        StringBuffer tbl = new StringBuffer();
        List<String> comments = new ArrayList<>();
        if (viewDef == null) {
            tbl.append(String.format("CREATE TABLE %s (", table));

            for (ColumnMeta cm : columnMetas) {
                String dft = "";
                if (cm.defaultValue != null && !cm.defaultValue.equals("")) {
                    if (cm.getJavaBaseType().equals(String.class)) {
                        dft = " DEFAULT '" + cm.defaultValue + "'";
                    } else {
                        dft = " DEFAULT " + cm.defaultValue;
                    }
                }
                tbl.append(String.format("%s %s%s%s,", cm.name, cm.type, cm.nullable ? "" : " NOT NULL", dft));
                if (cm.comment != null && !cm.comment.equals("")) {
                    comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\" IS '%s';", table, cm.name, cm.comment));
                }
            }
            tbl.delete(tbl.length() - 1, tbl.length());
            tbl.append(");");
            planBuilder.append(SqlPlanBuilder.CREATE, this, tbl.toString());
            /** 添加字段约束 */
            planBuilder.append(SqlPlanBuilder.ALTER, this, IndexMeta.addConstraintSql(fieldConstraint));

            /** 添加复合约束 */
            planBuilder.append(SqlPlanBuilder.ALTER, this, IndexMeta.addConstraintSql(indexes));

            planBuilder.append(SqlPlanBuilder.COMMENT, this, comments);
        } else {
            planBuilder.append(SqlPlanBuilder.DROP, this, String.format("DROP VIEW IF EXISTS %s;", table));
            tbl.append(String.format("CREATE VIEW %s AS (", table));
            tbl.append(viewDef.toSql());
            tbl.append(");");
            planBuilder.append(SqlPlanBuilder.CREATE, this, tbl.toString());
        }
        return planBuilder;
    }

    /**
     * 升级数据库，返回需要执行的sql
     *
     * @param other 其他实例
     * @return SQL执行计划
     */
    public SqlPlanBuilder upgradeTo(TableMeta other) {
        if (!table.equals(other.table)) {
            return null;
        }
        if (viewDef == null) {
            return upgradeTableTo(other);
        } else {
            SqlPlanBuilder planBuilder = new SqlPlanBuilder();
            planBuilder.append(SqlPlanBuilder.DROP, other, String.format("DROP VIEW IF EXISTS %s;", table));
            StringBuffer tbl = new StringBuffer();
            tbl.append(String.format("CREATE VIEW %s AS (", table));
            tbl.append(viewDef);
            tbl.append(");");
            planBuilder.append(SqlPlanBuilder.CREATE, other, tbl.toString());
            return planBuilder;
        }
    }

    /**
     * 升级数据库，返回需要执行的sql
     *
     * @param other 其他实例
     * @return SQL执行计划
     */
    public SqlPlanBuilder upgradeTableTo(TableMeta other) {
        List<String> comments = new ArrayList<>();
        List<ColumnMeta> otherColumn = other.columnMetas;
        List<ColumnMeta> newColumns = new ArrayList<>();
        List<IndexMeta> dropIndexs = new ArrayList<>();
        List<IndexMeta> dropForeignKeys = new ArrayList<>();
        List<ColumnMeta> alterColumns = new ArrayList<>();
        List<IndexMeta> addForeignKeys = new ArrayList<>();

        for (ColumnMeta ocm : otherColumn) {
            ColumnMeta cm = nameToColumnMeta.get(ocm.name);
            if (cm == null) {
                newColumns.add(ocm);
                if (ocm.comment != null && !ocm.comment.equals("")) {
                    comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\" IS '%s';", table, ocm.name, ocm.comment));
                }
            } else {
                if (cm.unique && !ocm.unique) {
                    IndexMeta cnstMeta = new IndexMeta();
                    cnstMeta.setColumns(Arrays.asList(ocm.getName()));
                    cnstMeta.setTable(table);
                    cnstMeta.setType(IndexType.UNIQUE_FIELD);
                    cnstMeta.setUsing(ocm.getUsing());
                    dropIndexs.add(cnstMeta);
                }
                /** 删除外键 */
                if (cm.useFK && !ocm.useFK) {
                    IndexMeta cnstMeta = new IndexMeta();
                    cnstMeta.setColumns(Arrays.asList(cm.getName()));
                    cnstMeta.setTable(table);
                    cnstMeta.setType(IndexType.FOREIGN_KEY);
                    cnstMeta.setRefTable(cm.getRefTable());
                    cnstMeta.setUsing(cm.getUsing());
                    dropForeignKeys.add(cnstMeta);
                }
                if (!Objects.equals(cm.type, ocm.type) || !Objects.equals(cm.defaultValue, ocm.defaultValue) || cm.nullable != ocm.nullable) {
                    alterColumns.add(ocm);
                }
                if (!cm.useFK && ocm.useFK) {
                    if (ocm.useFK && ocm.refTable != null) {
                        IndexMeta cnstMeta = new IndexMeta();
                        cnstMeta.setColumns(Arrays.asList(cm.getName()));
                        cnstMeta.setTable(table);
                        cnstMeta.setType(IndexType.FOREIGN_KEY);
                        cnstMeta.setRefTable(ocm.getRefTable());
                        cnstMeta.setUsing(ocm.getUsing());
                        addForeignKeys.add(cnstMeta);
                    }
                }
                if (!Objects.equals(cm.comment, ocm.comment)) {
                    if (ocm.comment != null && !ocm.comment.equals("")) {
                        comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\" IS '%s';", table, ocm.name, ocm.comment));
                    }
                }
            }
        }

        Set<String> oldConstraintSet = new HashSet<String>();
        Set<String> currentSet = new HashSet<String>();

        for (IndexMeta tuple : fieldConstraint) {
            oldConstraintSet.add(String.join(",", tuple.columns));
        }
        List<IndexMeta> addUniqueConstraint = new ArrayList<>();
        for (IndexMeta tuple : other.fieldConstraint) {
            currentSet.add(String.join(",", tuple.columns));
            if (!oldConstraintSet.contains(String.join(",", tuple.columns))) {
                addUniqueConstraint.add(tuple);
            }
        }
        List<IndexMeta> dropUniqueConstraint = new ArrayList<>();
        for (IndexMeta tuple : fieldConstraint) {
            if (!currentSet.contains(String.join(",", tuple.columns))) {
                dropUniqueConstraint.add(tuple);
            }
        }

        oldConstraintSet.clear();
        currentSet.clear();

        for (IndexMeta tuple : indexes) {
            oldConstraintSet.add(String.join(",", tuple.columns));
        }
        List<IndexMeta> addCustomizedConstraint = new ArrayList<>();
        for (IndexMeta tuple : other.indexes) {
            currentSet.add(String.join(",", tuple.columns));
            if (!oldConstraintSet.contains(String.join(",", tuple.columns))) {
                addCustomizedConstraint.add(tuple);
            }
        }
        List<IndexMeta> dropCustomizedConstraint = new ArrayList<>();
        for (IndexMeta tuple : indexes) {
            if (!currentSet.contains(String.join(",", tuple.columns))) {
                dropCustomizedConstraint.add(tuple);
            }
        }

        SqlPlanBuilder planBuilder = new SqlPlanBuilder();

        planBuilder.append(SqlPlanBuilder.ALTER, other, addColumnSql(newColumns));
        planBuilder.append(SqlPlanBuilder.DROP, other, IndexMeta.dropConstraintSql(dropIndexs));
        planBuilder.append(SqlPlanBuilder.DROP, other, IndexMeta.dropConstraintSql(dropForeignKeys));
        planBuilder.append(SqlPlanBuilder.ALTER, other, alterColumnSql(alterColumns));
        planBuilder.append(SqlPlanBuilder.ALTER, other, IndexMeta.addConstraintSql(addForeignKeys));
        planBuilder.append(SqlPlanBuilder.DROP, other, IndexMeta.dropConstraintSql(dropUniqueConstraint));
        planBuilder.append(SqlPlanBuilder.ALTER, other, IndexMeta.addConstraintSql(addUniqueConstraint));
        planBuilder.append(SqlPlanBuilder.DROP, other, IndexMeta.dropConstraintSql(dropCustomizedConstraint));
        planBuilder.append(SqlPlanBuilder.ALTER, other, IndexMeta.addConstraintSql(addCustomizedConstraint));
        return planBuilder;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableMeta tableMeta = (TableMeta) o;
        return Objects.equals(table, tableMeta.table) &&
                Objects.equals(viewDef, tableMeta.viewDef) &&
                Objects.equals(indexes, tableMeta.indexes) &&
                Objects.equals(columnMetas, tableMeta.columnMetas) &&
                Objects.equals(nameToColumnMeta, tableMeta.nameToColumnMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, viewDef, indexes, columnMetas, nameToColumnMeta);
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

    public TblType getType() {
        return type;
    }
}
