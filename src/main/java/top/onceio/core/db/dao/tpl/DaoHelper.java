package top.onceio.core.db.dao.tpl;

import org.apache.log4j.Logger;
import top.onceio.core.OConfig;
import top.onceio.core.db.annotation.ConstraintType;
import top.onceio.core.db.dao.DDLDao;
import top.onceio.core.db.dao.IdGenerator;
import top.onceio.core.db.dao.Page;
import top.onceio.core.db.dao.TransDao;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.ConstraintMeta;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.db.tbl.OEntity;
import top.onceio.core.exception.Failed;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.function.Consumer;

public class DaoHelper implements DDLDao, TransDao {
    private static final Logger LOGGER = Logger.getLogger(DaoHelper.class);

    private JdbcHelper jdbcHelper;
    private Map<Class<?>, TableMeta> classToTableMeta;
    private Map<String, Class<?>> tableToClass;
    private IdGenerator idGenerator;
    private List<Class<? extends OEntity>> entities;

    public DaoHelper() {
    }

    public DaoHelper(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends OEntity>> entitys) {
        super();
        init(jdbcHelper, idGenerator, entitys);
    }

    public boolean exist(Class<?> tbl) {
        Long cnt = 0L;
        try {
            cnt = (Long) jdbcHelper.queryForObject(
                    String.format("SELECT count(*) FROM %s", TableMeta.getTableName(tbl)));
        } catch (Failed e) {
            cnt = -1L;
        }
        if (cnt != null && cnt >= 0) {
            return true;
        } else {
            return false;
        }
    }

    private Map<String, TableMeta> findPGTableMeta(JdbcHelper jdbcHelper, Collection<String> schemaTables) {
        Map<String, TableMeta> result = new HashMap<>();
        if (schemaTables.isEmpty()) {
            return result;
        }
        String qColumns = "select\n" +
                "ns.nspname as schemaname,\n" +
                "c.relname as tablename,\n" +
                "a.attnum,\n" +
                "a.attname AS field,\n" +
                "t.typname AS type,\n" +
                "isc.character_maximum_length max_length,\n" +
                "isc.numeric_precision,\n" +
                "isc.numeric_scale,\n" +
                "isc.column_default,\n" +
                "isc.is_nullable = 'YES' as nullable,\n" +
                "b.description AS comment,\n" +
                "pk.conname pk_conname,\n" +
                "uk.conname uk_conname,\n" +
                "fk.conname fk_conname,\n" +
                "fc.relname f_tablename,\n" +
                "fns.nspname f_schemaname\n" +
                "from pg_attribute a \n" +
                "left join pg_type t on a.atttypid = t.oid\n" +
                "left join pg_class c on a.attrelid = c.oid\n" +
                "left join pg_namespace ns on ns.oid = c.relnamespace\n" +
                "left join pg_description b ON a.attrelid=b.objoid AND a.attnum = b.objsubid\n" +
                "left join pg_constraint pk on pk.conrelid = c.oid and pk.contype='p' and a.attnum = pk.conkey[1]\n" +
                "left join pg_constraint uk on uk.conrelid = c.oid and uk.contype='u' and a.attnum = uk.conkey[1]\n" +
                "left join pg_constraint fk on fk.conrelid = c.oid and fk.contype='f' and a.attnum = fk.conkey[1] \n" +
                "left join pg_class fc on fk.confrelid = fc.oid\n" +
                "left join pg_namespace fns on fns.oid = fc.relnamespace\n" +
                "left join information_schema.columns isc on isc.table_schema = ns.nspname and isc.table_name = c.relname and isc.column_name =  a.attname\n" +
                "WHERE a.attnum > 0\n" +
                "and a.attrelid = c.oid\n" +
                "and a.atttypid = t.oid\n" +
                "and ns.oid = c.relnamespace\n" +
                "and concat(ns.nspname,'.',c.relname) IN " + String.format("(%s)\n", OUtils.genStub("?", ",", schemaTables.size()), String.join("','")) +
                "ORDER BY ns.nspname,c.relname,a.attnum";
        Map<String, Map<String, ColumnMeta>> tableToColumns = new HashMap<>();

        jdbcHelper.query(qColumns, schemaTables.toArray(), (rs) -> {
            try {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                int varcharMaxLen = rs.getInt("max_length");
                int numericPrecision = rs.getInt("numeric_precision");
                int numericScale = rs.getInt("numeric_scale");
                String comment = rs.getString("comment");
                String column_default = rs.getString("column_default");
                boolean nullable = rs.getBoolean("nullable");
                String field = rs.getString("field");
                String typename = rs.getString("type");
                String pk_conname = rs.getString("pk_conname");
                String uk_conname = rs.getString("uk_conname");
                String fk_conname = rs.getString("fk_conname");
                String refTable = null;
                if (fk_conname != null) {
                    refTable = rs.getString("f_schemaname") + "." + rs.getString("f_tablename");
                }

                String schemaTable = schema + "." + table;
                Map<String, ColumnMeta> columnMetaList = tableToColumns.get(schemaTable);
                if (columnMetaList == null) {
                    columnMetaList = new HashMap<>();
                    tableToColumns.put(schemaTable, columnMetaList);
                }
                ColumnMeta cm = new ColumnMeta();
                cm.setName(field);
                cm.setNullable(nullable);
                cm.setUnique(uk_conname != null);
                if (pk_conname != null) {
                    cm.setPrimaryKey(true);
                    cm.setUnique(true);
                    cm.setNullable(false);
                }
                cm.setUseFK(fk_conname != null);
                cm.setRefTable(refTable);
                cm.setType(typename);
                cm.setUsing("");
                cm.setPattern("");
                Class<?> javaBaseType = TableMeta.parseType(typename, varcharMaxLen, 0);
                if (typename.equals("varchar")) {
                    cm.setType(String.format("varchar(%d)", varcharMaxLen));
                } else if (typename.equals("numeric")) {
                    cm.setType(String.format("numeric(%d,%d)", numericPrecision, numericScale));
                }
                cm.setJavaBaseType(javaBaseType);
                cm.setDefaultValue(column_default != null ? column_default : "");
                cm.setComment(comment != null ? comment : "");
                columnMetaList.put(cm.getName(), cm);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });

        Map<String, List<ConstraintMeta>> tableToConstraintMeta = new HashMap<>();
        String qIndexes = "select *\n" +
                "from information_schema.table_constraints as c \n" +
                "left join pg_indexes as d on c.table_schema = d.schemaname and c.table_name = d.tablename and c.\"constraint_name\" = d.indexname\n" +
                " WHERE concat(c.table_schema,'.',c.table_name) IN " + String.format("('%s')", String.join("','", schemaTables)) +
                " ORDER BY c.table_schema,c.table_name";
        jdbcHelper.query(qIndexes, null, (rs) -> {
            try {
                String schema = rs.getString("table_schema");
                String table = rs.getString("table_name");
                String constraintName = rs.getString("constraint_name");
                String indexDef = rs.getString("indexdef");
                String schemaTable = schema + "." + table;
                Map<String, ColumnMeta> nameToColumnMeta = tableToColumns.get(schemaTable);
                String col = indexDef.substring(indexDef.lastIndexOf('(') + 1, indexDef.lastIndexOf(')'));
                if (col.contains(",") && constraintName.startsWith(ConstraintMeta.INDEX_NAME_PREFIX_NQ)) {
                    ConstraintMeta constraintMeta = new ConstraintMeta();
                    List<String> columns = new ArrayList<>();
                    for (String c : col.split(",")) {
                        columns.add(c.trim());
                    }
                    constraintMeta.setColumns(columns);
                    constraintMeta.setTable(table);
                    constraintMeta.setSchema(schema);
                    if (indexDef.toUpperCase().contains(" " + ConstraintMeta.UNIQUE + " ")) {
                        constraintMeta.setType(ConstraintType.UNIQUE);
                    } else {
                        constraintMeta.setType(ConstraintType.INDEX);
                    }
                    constraintMeta.setUsing(indexDef.replaceAll("^.* USING ([^( ]+).*$", "$1").trim());
                    List<ConstraintMeta> constraintMetas = tableToConstraintMeta.get(schema);
                    if (constraintMetas == null) {
                        constraintMetas = new ArrayList<>();
                        tableToConstraintMeta.put(schemaTable, constraintMetas);
                    }
                    constraintMetas.add(constraintMeta);
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });
        tableToColumns.forEach((schemaTable, columnNameToCol) -> {
            List<ConstraintMeta> constraints = tableToConstraintMeta.getOrDefault(schemaTable, new ArrayList<>());
            String schema = schemaTable.split("\\.")[0];
            String table = schemaTable.split("\\.")[1];
            TableMeta tm = new TableMeta();
            tm.setTable(table);
            tm.setSchema(schema);
            tm.setColumnMetas(new ArrayList<>(columnNameToCol.values()));
            tm.setConstraints(constraints);
            tm.setPrimaryKey("id");
            tm.freshConstraintMetaTable();
            result.put(schemaTable, tm);
        });
        return result;
    }

    private Map<String, TableMeta> findTableMeta(JdbcHelper jdbcHelper, Collection<String> schemaTables) {
        switch (jdbcHelper.getDBType()) {
            case POSTGRESQL:
                return findPGTableMeta(jdbcHelper, schemaTables);
            default:
                OAssert.err("不支持数据库类型：%s", jdbcHelper.getDBType());
        }
        return new HashMap<>();
    }

    public void init(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends OEntity>> entities) {
        this.jdbcHelper = jdbcHelper;
        this.idGenerator = idGenerator;
        this.classToTableMeta = new HashMap<>();
        this.tableToClass = new HashMap<>();
        if (entities != null) {
            this.entities = entities;
            for (Class<? extends OEntity> tbl : entities) {
                TableMeta tm = TableMeta.createBy(tbl);
                tm.freshNameToField(tbl);
                tm.freshConstraintMetaTable();
                classToTableMeta.put(tbl, tm);
                tableToClass.put(tm.getSchema() + "." + tm.getTable(), tbl);
            }
        }

        Map<String, TableMeta> oldTableMeta = findTableMeta(jdbcHelper, tableToClass.keySet());

        Map<String, List<String>> tblSqls = new HashMap<>();
        for (Class<?> tbl : classToTableMeta.keySet()) {
            TableMeta tm = classToTableMeta.get(tbl);
            String schemaTable = tm.getSchema() + "." + tm.getTable();
            TableMeta old = oldTableMeta.get(schemaTable);
            if (old == null) {
                List<String> createSQL = tm.createTableSql();
                if (!createSQL.isEmpty()) {
                    tblSqls.put(schemaTable, createSQL);
                }
            } else if (!old.equals(tm)) {
                List<String> updateSQL = old.upgradeTo(tm);
                if (!updateSQL.isEmpty()) {
                    tblSqls.put(schemaTable, updateSQL);
                }
            }
        }
        List<String> order = new ArrayList<>();
        for (String schemaTable : tblSqls.keySet()) {
            sorted(order, schemaTable);
        }

        List<String> sqls = new ArrayList<>();
        for (String tbl : order) {
            List<String> list = tblSqls.get(tbl);
            if (list != null && !list.isEmpty()) {
                sqls.addAll(list);
            }
        }
        if (!sqls.isEmpty()) {
            jdbcHelper.batchExec(sqls.toArray(new String[0]));
        }
    }

    private void sorted(List<String> order, String schemaTable) {
        if (!order.contains(schemaTable)) {
            Class<?> tbl = tableToClass.get(schemaTable);
            TableMeta tblMeta = classToTableMeta.get(tbl);
            if (tblMeta != null) {
                for (ConstraintMeta cm : tblMeta.getFieldConstraint()) {
                    if (cm.getType().equals(ConstraintType.FOREIGN_KEY)) {
                        sorted(order, cm.getRefTable());
                    }
                }
            }
            order.add(schemaTable);
        }
    }

    public List<Class<? extends OEntity>> getEntities() {
        return entities;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public JdbcHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public void setJdbcHelper(JdbcHelper jdbcHelper) {
        this.jdbcHelper = jdbcHelper;
    }

    public Map<Class<?>, TableMeta> getTableToTableMata() {
        return classToTableMeta;
    }

    public void setTableToTableMata(Map<Class<?>, TableMeta> tableToTableMeta) {
        this.classToTableMeta = tableToTableMeta;
    }

    public <E extends OEntity> boolean drop(Class<E> tbl) {
        TableMeta tm = classToTableMeta.get(tbl);
        if (tm == null) {
            return false;
        }
        String sql = String.format("DROP TABLE IF EXISTS %s;", TableMeta.getTableName(tbl));
        jdbcHelper.batchUpdate(sql);
        return true;
    }

    public int[] batchUpdate(final String... sql) {
        return jdbcHelper.batchExec(sql);
    }

    public int[] batchUpdate(final String sql, List<Object[]> batchArgs) {
        return jdbcHelper.batchUpdate(sql, batchArgs);
    }

    private static <E extends OEntity> E createBy(Class<E> tbl, TableMeta tm, ResultSet rs) throws SQLException {
        E row = null;
        try {
            row = tbl.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            OAssert.warnning("%s InstantiationException", tbl);
        }
        if (row != null) {
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String colName = rsmd.getColumnName(i);
                ColumnMeta cm = tm.getColumnMetaByName(colName);
                if (cm != null) {
                    try {
                        Object val = rs.getObject(colName);
                        cm.getField().set(row, val);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    row.put(colName, rs.getObject(i));
                }
            }
            return row;
        }
        return row;
    }

    /**
     * @param sql
     * @param args
     * @return list[?>0]:row data
     */
    public List<Object[]> call(String sql, Object[] args) {
        return jdbcHelper.call(sql, args);
    }

    public void beginTransaction(int level, boolean readOnly) {
        jdbcHelper.beginTransaction(level, readOnly);
    }

    public Savepoint setSavepoint() {
        return jdbcHelper.setSavepoint();
    }

    public void rollback() {
        jdbcHelper.rollback();
    }

    public void rollback(Savepoint sp) {
        jdbcHelper.rollback(sp);
    }

    public void commit() {
        jdbcHelper.commit();
    }

    public <E extends OEntity> E get(Class<E> tbl, Long id) {
        Cnd<E> cnd = new Cnd<E>(tbl, true);
        cnd.setPage(1);
        cnd.setPagesize(1);
        cnd.eq().setId(id);
        Page<E> page = findByTpl(tbl, null, cnd);
        if (page.getData().size() == 1) {
            return page.getData().get(0);
        }
        return null;
    }

    public <E extends OEntity> E insert(E entity) {
        OAssert.warnning(entity != null, "不可以插入null");
        Class<?> tbl = entity.getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        tm.validate(entity, false);
        TblIdNameVal<E> idNameVal = new TblIdNameVal<>(tm.getColumnMetas(), Arrays.asList(entity));
        if (idNameVal.getIdAt(0) == null) {
            Long id = idGenerator.next(tbl);
            idNameVal.setIdAt(0, id);
            entity.setId(id);
        }
        idNameVal.dropAllNullColumns();
        List<Object> vals = idNameVal.getIdValsList().get(0);
        List<String> names = idNameVal.getIdNames();
        String stub = OUtils.genStub("?", ",", names.size());
        String sql = String.format("INSERT INTO %s(%s) VALUES(%s);", tm.getTable(), String.join(",", names), stub);
        jdbcHelper.update(sql, vals.toArray());
        return entity;
    }

    public <E extends OEntity> int batchInsert(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        Class<?> tbl = entities.get(0).getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));

        for (E entity : entities) {
            tm.validate(entity, false);
            if (entity.getId() == null) {
                Long id = idGenerator.next(tbl);
                entity.setId(id);
            }
        }

        TblIdNameVal<E> idNameVal = new TblIdNameVal<>(tm.getColumnMetas(), entities);

        idNameVal.dropAllNullColumns();
        List<String> names = idNameVal.getIdNames();
        List<List<Object>> valsList = idNameVal.getIdValsList();

        String stub = OUtils.genStub("?", ",", names.size());
        //TODO
        String sql = String.format("INSERT INTO %s(%s) VALUES(%s);", tm.getTable(), String.join(",", names), stub);
        List<Object[]> vals = new ArrayList<>(valsList.size());
        for (int i = 0; i < valsList.size(); i++) {
            vals.add(valsList.get(i).toArray());
        }
        int[] cnts = jdbcHelper.batchUpdate(sql, vals);
        int cnt = 0;
        for (int c : cnts) {
            cnt += c;
        }
        return cnt;
    }

    private <E extends OEntity> int update(E entity, boolean ignoreNull) {
        OAssert.warnning(entity != null, "不可以插入null");
        Class<?> tbl = entity.getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        tm.validate(entity, ignoreNull);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        TblIdNameVal<E> idNameVal = new TblIdNameVal<>(tm.getColumnMetas(), Arrays.asList(entity));
        Object id = idNameVal.getIdAt(0);
        OAssert.err(id != null, "Long 不能为NULL");
        /** ignore rm */
        idNameVal.dropColumns("rm");
        if (ignoreNull) {
            idNameVal.dropAllNullColumns();
        }
        List<String> names = idNameVal.getNames();
        List<Object> vals = idNameVal.getValsList().get(0);
        String sql = String.format("UPDATE %s SET %s=? WHERE id=? AND rm = false;", tm.getTable(),
                String.join("=?,", names));
        vals.add(id);
        return jdbcHelper.update(sql, vals.toArray());
    }

    public <E extends OEntity> int update(E entity) {
        return update(entity, false);
    }

    public <E extends OEntity> int updateIgnoreNull(E entity) {
        return update(entity, true);
    }

    public <E extends OEntity> int updateByTpl(Class<E> tbl, UpdateTpl<E> tpl) {
        OAssert.warnning(tpl.getId() != null && tpl != null, "Are you sure to update a null value?");
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        // validate(tm,tpl,false);
        String setTpl = tpl.getSetTpl();
        List<Object> vals = new ArrayList<>(tpl.getArgs().size() + 1);
        vals.addAll(tpl.getArgs());
        vals.add(tpl.getId());
        String sql = String.format("UPDATE %s SET %s WHERE id=? AND rm=false;", tm.getTable(), setTpl);
        return jdbcHelper.update(sql, vals.toArray());
    }

    public <E extends OEntity> int updateByTplCnd(Class<E> tbl, UpdateTpl<E> tpl, Cnd<E> cnd) {
        OAssert.warnning(tpl != null, "Are you sure to update a null value?");
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        // validate(tm,tpl,false);
        List<Object> vals = new ArrayList<>();
        vals.addAll(tpl.getArgs());
        List<Object> sqlArgs = new ArrayList<>();
        String cndSql = cnd.whereSql(sqlArgs);
        if (cndSql.isEmpty()) {
            OAssert.warnning("查询条件不能为空");
        }
        if (tpl.getSetTpl().isEmpty()) {
            OAssert.err("更新内容不能为空");
        }
        vals.addAll(sqlArgs);
        String sql = String.format("UPDATE %s SET %s WHERE (%s) AND rm=false;", tm.getTable(), tpl.getSetTpl(), cndSql);
        return jdbcHelper.update(sql, vals.toArray());
    }

    public <E extends OEntity> int removeById(Class<E> tbl, Long id) {
        if (id == null)
            return 0;
        OAssert.warnning(id != null, "Long不能为null");
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        String sql = String.format("UPDATE %s SET rm=true WHERE id=?", tm.getTable());
        return jdbcHelper.update(sql, new Object[]{id});
    }

    public <E> int removeByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        String stub = OUtils.genStub("?", ",", ids.size());
        String sql = String.format("UPDATE %s SET rm=true WHERE rm = false AND id IN (%s)", tm.getTable(), stub);
        LOGGER.debug(sql);
        return jdbcHelper.update(sql, ids.toArray());
    }

    public <E extends OEntity> int remove(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        List<Object> sqlArgs = new ArrayList<>();
        String whereCnd = cnd.whereSql(sqlArgs);
        String sql = String.format("UPDATE %s SET rm=true WHERE %s", tm.getTable(), whereCnd);
        return jdbcHelper.update(sql, sqlArgs.toArray());
    }

    public <E extends OEntity> int recovery(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        List<Object> sqlArgs = new ArrayList<>();
        String whereCnd = cnd.whereSql(sqlArgs);
        String sql = String.format("UPDATE %s SET rm=false WHERE rm=true AND %s", tm.getTable(), whereCnd);
        if (whereCnd.equals("")) {
            sql = String.format("UPDATE %s SET rm=false WHERE rm=true", tm.getTable());
        }
        return jdbcHelper.update(sql, sqlArgs.toArray());
    }

    public <E> int deleteById(Class<E> tbl, Long id) {
        if (id == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        String sql = String.format("DELETE FROM %s WHERE id=?", tm.getTable());
        return jdbcHelper.update(sql, new Object[]{id});
    }

    public <E> int deleteByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        String stub = OUtils.genStub("?", ",", ids.size());
        String sql = String.format("DELETE FROM %s WHERE id IN (%s) ", tm.getTable(), stub);
        return jdbcHelper.update(sql, ids.toArray());
    }

    public <E extends OEntity> int delete(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        List<Object> sqlArgs = new ArrayList<>();
        cnd.and().eq().setRm(true);
        String whereCnd = cnd.whereSql(sqlArgs);
        String sql = String.format("DELETE FROM %s WHERE %s;", tm.getTable(), whereCnd);
        return jdbcHelper.update(sql, sqlArgs.toArray());
    }

    public <E extends OEntity> long count(Class<E> tbl) {
        return count(tbl, null, new Cnd<E>(tbl));
    }

    public <E extends OEntity> long count(Class<E> tbl, Cnd<E> cnd) {
        return count(tbl, null, cnd);
    }

    public <E extends OEntity> long count(Class<E> tbl, SelectTpl<E> tpl, Cnd<E> cnd) {
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        List<Object> sqlArgs = new ArrayList<>();
        String sql = cnd.countSql(tm, tpl, sqlArgs);
        LOGGER.debug(sql);
        return (Long) jdbcHelper.queryForObject(sql, sqlArgs.toArray(new Object[0]));
    }

    public <E extends OEntity> Page<E> find(Class<E> tbl, Cnd<E> cnd) {
        return findByTpl(tbl, null, cnd);
    }

    public <E extends OEntity> Page<E> findByTpl(Class<E> tbl, SelectTpl<E> tpl, Cnd<E> cnd) {
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        Page<E> page = new Page<E>();
        if (cnd.getPage() == null || cnd.getPage() <= 0) {
            page.setPage(cnd.getPage());
            if (cnd.getPage() == null || cnd.getPage() == 0) {
                cnd.setPage(1);
                page.setPage(1);
            } else {
                cnd.setPage(Math.abs(cnd.getPage()));
            }
            page.setTotal(count(tbl, tpl, cnd));
        }
        if (cnd.getPagesize() == null) {
            cnd.setPagesize(OConfig.PAGE_SIZE_DEFAULT);
            page.setPagesize(OConfig.PAGE_SIZE_DEFAULT);
        } else if (cnd.getPagesize() > OConfig.PAGE_SIZE_MAX) {
            cnd.setPagesize(OConfig.PAGE_SIZE_MAX);
            page.setPagesize(OConfig.PAGE_SIZE_MAX);
        }
        if (page.getTotal() == null || page.getTotal() > 0) {
            List<Object> sqlArgs = new ArrayList<>();
            String sql = cnd.pageSql(tm, tpl, sqlArgs);
            LOGGER.debug(sql);
            List<E> data = new ArrayList<>();
            jdbcHelper.query(sql, sqlArgs.toArray(), new Consumer<ResultSet>() {
                @Override
                public void accept(ResultSet rs) {
                    E row = null;
                    try {
                        row = createBy(tbl, tm, rs);
                        data.add(row);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Failed.throwError(e.getMessage());
                    }
                }
            });
            page.setData(data);
        } else {
            page.setData(new ArrayList<>(0));
        }
        return page;
    }

    public <E extends OEntity> E fetch(Class<E> tbl, SelectTpl<E> tpl, Cnd<E> cnd) {
        if (cnd == null) {
            cnd = new Cnd<E>(tbl);
        }
        cnd.setPage(1);
        cnd.setPagesize(1);
        Page<E> page = findByTpl(tbl, tpl, cnd);
        if (page.getData().size() > 0) {
            return page.getData().get(0);
        }
        return null;
    }

    public <E extends OEntity> void download(Class<E> tbl, SelectTpl<E> tpl, Cnd<E> cnd, Consumer<E> consumer) {
        TableMeta tm = classToTableMeta.get(tbl);
        if (tm == null) {
            return;
        }
        List<Object> args = new ArrayList<>();
        StringBuffer sql = cnd.wholeSql(tm, tpl, args);
        jdbcHelper.query(sql.toString(), args.toArray(new Object[0]), new Consumer<ResultSet>() {
            @Override
            public void accept(ResultSet rs) {
                E row = null;
                try {
                    row = createBy(tbl, tm, rs);
                    consumer.accept(row);
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        });
    }

    public <E extends OEntity> List<E> findByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<E>();
        }
        Cnd<E> cnd = new Cnd<E>(tbl);
        cnd.setPage(1);
        cnd.setPagesize(ids.size());
        cnd.in(ids.toArray(new Object[0])).setId(null);
        Page<E> page = findByTpl(tbl, null, cnd);
        return page.getData();
    }

}
