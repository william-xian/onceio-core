package top.onceio.core.db.meta;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import org.apache.log4j.Logger;

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
    String schema;
    String table;
    ConstraintMeta primaryKey;
    String viewDef;
    transient List<ConstraintMeta> fieldConstraint = new ArrayList<>(0);
    List<ConstraintMeta> constraints = new ArrayList<>();
    List<ColumnMeta> columnMetas = new ArrayList<>(0);
    transient Map<String, ColumnMeta> nameToColumnMeta = new HashMap<>();
    transient Class<?> entity;


    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
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

    public String getViewDef() {
        return viewDef;
    }

    public void setViewDef(String viewDef) {
        this.viewDef = viewDef;
    }

    public ConstraintMeta getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(ConstraintMeta primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        ConstraintMeta pk = new ConstraintMeta();
        pk.setSchema(this.schema);
        pk.setTable(this.table);
        pk.setName(String.format("%s%s_%s_%s", ConstraintMeta.INDEX_NAME_PREFIX_PK, pk.schema, pk.table, primaryKey));
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

    public void freshNameToField(Class<?> tblEntity) {
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
                    cnsMeta.setSchema(this.getSchema());
                    cnsMeta.setTable(this.getTable());
                    cnsMeta.setName(ConstraintMeta.INDEX_NAME_PREFIX_UN + cnsMeta.getTable() + "_" + cm.name);
                    cnsMeta.setUsing(cm.using);
                    cnsMeta.setType(ConstraintType.UNIQUE);
                    fieldConstraint.add(cnsMeta);
                } else if (cm.useFK && cm.refTable != null) {
                    ConstraintMeta cnsMeta = new ConstraintMeta();
                    List<String> cols = new ArrayList<>();
                    cols.add(cm.getName());
                    cnsMeta.setColumns(new ArrayList<String>(cols));
                    cnsMeta.setSchema(this.getSchema());
                    cnsMeta.setTable(this.getTable());
                    cnsMeta.setName(ConstraintMeta.INDEX_NAME_PREFIX_FK + cnsMeta.getTable() + "_" + cm.name);
                    cnsMeta.setUsing(cm.using);
                    cnsMeta.setType(ConstraintType.FOREIGN_KEY);
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
            String sql = String.format("ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s", schema, table, ocm.name, ocm.type);
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
            String sql = String.format("ALTER TABLE %s.%s ADD COLUMN %s %s", schema, table, ocm.name, ocm.type);
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
     * drop table if exists tbl_a;
     */
    public List<String> createTableSql() {
        List<String> sqls = new ArrayList<>();
        StringBuffer tbl = new StringBuffer();
        List<String> comments = new ArrayList<>();
        if (viewDef == null) {
            tbl.append(String.format("CREATE TABLE %s.%s (", schema, table));

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
                    comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\".\"%s\" IS '%s';", schema, table, cm.name, cm.comment));
                }
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

            sqls.addAll(comments);
        } else {
            sqls.add(String.format("DROP VIEW IF EXISTS %s.%s;", schema, table));
            tbl.append(String.format("CREATE VIEW %s.%s AS (", schema, table));
            tbl.append(viewDef);
            tbl.append(");");
            sqls.add(tbl.toString());
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
        if (viewDef == null) {
            return upgradeTableTo(other);
        } else {
            List<String> sqls = new ArrayList<>();
            sqls.add(String.format("DROP VIEW IF EXISTS %s.%s;", schema, table));
            StringBuffer tbl = new StringBuffer();
            tbl.append(String.format("CREATE OR REPLACE VIEW %s.%s AS (", schema, table));
            tbl.append(viewDef);
            tbl.append(");");
            sqls.add(tbl.toString());

            return sqls;
        }
    }

    /**
     * 升级数据库，返回需要执行的sql
     *
     * @param other
     * @return
     */
    public List<String> upgradeTableTo(TableMeta other) {
        List<String> sqls = new ArrayList<>();
        List<String> comments = new ArrayList<>();
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
                if (ocm.comment != null && !ocm.comment.equals("")) {
                    comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\".\"%s\" IS '%s';", schema, table, ocm.name, ocm.comment));
                }
            } else {
                if (cm.unique && !ocm.unique) {
                    ConstraintMeta cnstMeta = new ConstraintMeta();
                    cnstMeta.setColumns(Arrays.asList(ocm.getName()));
                    cnstMeta.setSchema(schema);
                    cnstMeta.setTable(table);
                    cnstMeta.setType(ConstraintType.UNIQUE);
                    cnstMeta.setUsing(ocm.getUsing());
                    dropIndexs.add(cnstMeta);
                }
                /** 删除外键 */
                if (cm.useFK && !ocm.useFK) {
                    ConstraintMeta cnstMeta = new ConstraintMeta();
                    cnstMeta.setColumns(Arrays.asList(cm.getName()));
                    cnstMeta.setSchema(schema);
                    cnstMeta.setTable(table);
                    cnstMeta.setType(ConstraintType.FOREIGN_KEY);
                    cnstMeta.setRefTable(cm.getRefTable());
                    cnstMeta.setUsing(cm.getUsing());
                    dropForeignKeys.add(cnstMeta);
                }
                if (!Objects.equals(cm.type, ocm.type) || !Objects.equals(cm.defaultValue, ocm.defaultValue) || cm.nullable != ocm.nullable) {
                    alterColumns.add(ocm);
                }
                if (!cm.useFK && ocm.useFK) {
                    if (ocm.useFK && ocm.refTable != null) {
                        ConstraintMeta cnstMeta = new ConstraintMeta();
                        cnstMeta.setColumns(Arrays.asList(cm.getName()));
                        cnstMeta.setSchema(schema);
                        cnstMeta.setTable(table);
                        cnstMeta.setType(ConstraintType.FOREIGN_KEY);
                        cnstMeta.setRefTable(ocm.getRefTable());
                        cnstMeta.setUsing(ocm.getUsing());
                        addForeignKeys.add(cnstMeta);
                    }
                }
                if (!Objects.equals(cm.comment, ocm.comment)) {
                    if (ocm.comment != null && !ocm.comment.equals("")) {
                        comments.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\".\"%s\" IS '%s';", schema, table, ocm.name, ocm.comment));
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

    public static final Map<Class<?>, TableMeta> tableCache = new HashMap<>();


    public static String getTableName(Class<?> clazz) {
        String defaultName = clazz.getSimpleName().replaceAll("([A-Z])", "_$1").replaceFirst("_", "").toLowerCase();
        Tbl tbl = clazz.getAnnotation(Tbl.class);
        if (tbl != null && !tbl.name().equals("")) {
            return tbl.name();
        } else {
            return defaultName;
        }
    }

    public static String getColumnName(Field field) {
        return field.getName().replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    private static String getViewName(Class<?> clazz) {
        String defaultName = clazz.getSimpleName().replaceAll("([A-Z])", "_$1").replaceFirst("_", "").toLowerCase();
        TblView tblView = clazz.getAnnotation(TblView.class);
        if (tblView != null && !tblView.name().equals("")) {
            return tblView.name();
        } else {
            return defaultName;
        }
    }

    public static TableMeta createBy(Class<?> entity) {
        TableMeta tm = new TableMeta();
        tm.entity = entity;
        Tbl tbl = entity.getAnnotation(Tbl.class);
        TblView tblView = entity.getAnnotation(TblView.class);
        if (tbl != null) {
            tm.schema = tbl.schema();
            tm.table = getTableName(entity);
            List<ConstraintMeta> constraints = new ArrayList<>();
            for (Constraint c : tbl.constraints()) {
                ConstraintMeta cm = new ConstraintMeta();
                constraints.add(cm);
                cm.setColumns(Arrays.asList(c.colNames()));
                cm.setSchema(tm.getSchema());
                cm.setTable(tm.getTable());
                if (c.unique()) {
                    cm.setType(ConstraintType.UNIQUE_INDEX);
                } else {
                    cm.setType(ConstraintType.INDEX);
                }
                cm.setUsing(c.using());
            }
            tm.setConstraints(constraints);
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
                if (field.getName().equals("id")) {
                    tm.setPrimaryKey("id");
                    cm.setPrimaryKey(true);
                    cm.setUnique(true);
                    cm.setNullable(false);

                }
                cm.setPattern(col.pattern());
                if (col.type().equals("")) {
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
                    String type = transType(javaBaseType, col);
                    cm.setType(type);
                } else {
                    cm.setType(col.type());
                }

                if (col.ref() != void.class) {
                    cm.setUseFK(col.useFK());
                    Tbl e = col.ref().getAnnotation(Tbl.class);
                    String table = getTableName(col.ref());
                    cm.setRefTable(e.schema() + "." + table);
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
        if (tblView != null) {
            tm.schema = tblView.schema();
            String viewName = tblView.name().equalsIgnoreCase("") ? TableMeta.getViewName(entity) : tblView.name();
            tm.setTable(viewName);
            tm.setViewDef(tblView.def());
        }
        tableCache.put(entity, tm);
        return tm;
    }

    /**
     * 以postgresql為准
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableMeta tableMeta = (TableMeta) o;
        return Objects.equals(schema, tableMeta.schema) &&
                Objects.equals(table, tableMeta.table) &&
                Objects.equals(primaryKey, tableMeta.primaryKey) &&
                Objects.equals(viewDef, tableMeta.viewDef) &&
                Objects.equals(constraints, tableMeta.constraints) &&
                Objects.equals(columnMetas, tableMeta.columnMetas) &&
                Objects.equals(nameToColumnMeta, tableMeta.nameToColumnMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, table, primaryKey, viewDef, constraints, columnMetas, nameToColumnMeta);
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
