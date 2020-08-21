package top.onceio.core.db.model;

import org.apache.log4j.Logger;
import top.onceio.core.db.annotation.IndexType;
import top.onceio.core.db.annotation.TblType;
import top.onceio.core.db.dao.DDLDao;
import top.onceio.core.db.dao.IdGenerator;
import top.onceio.core.db.dao.Page;
import top.onceio.core.db.dao.TransDao;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.IndexMeta;
import top.onceio.core.db.meta.SqlPlanBuilder;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.exception.Failed;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OLog;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.function.Consumer;

/**
 * 增 单个添加，批量添加, 不重复添加删 主键删除，条件删除改 主键更改，非null更改，表达式更改，条件批量更改
 * 删 主键删除，条件删除
 * 改 主键更改，非null更改，表达式更改，条件批量更改
 * 查 外连接，内连接，子查询，视图（子查询，With，视图，物化视图，union）
 * 函数
 **/
public class DaoHelper implements DDLDao, TransDao {

    private static final Logger LOGGER = Logger.getLogger(DaoHelper.class);

    private JdbcHelper jdbcHelper;
    private Map<Class<?>, TableMeta> classToTableMeta;
    private Map<String, TableMeta> nameToMeta;
    private IdGenerator idGenerator;
    private List<Class<? extends BaseEntity>> entities;

    public DaoHelper() {
    }

    public DaoHelper(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends BaseEntity>> entitys) {
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

    private Map<String, TableMeta> findPGTableMeta(JdbcHelper jdbcHelper, Collection<String> tables) {
        Map<String, TableMeta> result = new HashMap<>();
        if (tables.isEmpty()) {
            return result;
        }
        List<String> schemaTables = new ArrayList<>();
        for(String table:tables) {
            if(!table.contains(".")){
                schemaTables.add("public."+table);
            }else {
                schemaTables.add(table);
            }
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

                String schemaTable = (schema + "." + table).toLowerCase().replace("public.","");
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
                cm.setType(typename.toLowerCase());
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

        Map<String, List<IndexMeta>> tableToConstraintMeta = new HashMap<>();
        String qIndexes = "select * from pg_indexes i\n" +
                "where i.indexname like ? and concat(i.schemaname,'.',i.tablename) IN " + String.format("(%s)", OUtils.genStub("?", ",", schemaTables.size()), String.join("','")) +
                " ORDER BY i.schemaname,i.tablename";
        List<String> args = new ArrayList<>(schemaTables.size() + 1);
        args.add(IndexMeta.INDEX_NAME_PREFIX_NQ + "%");
        args.addAll(schemaTables);
        jdbcHelper.query(qIndexes, args.toArray(), (rs) -> {
            try {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                String indexname = rs.getString("indexname");
                String indexDef = rs.getString("indexdef");
                String schemaTable = (schema + "." + table).toLowerCase().replace("public.","");
                Map<String, ColumnMeta> nameToColumnMeta = tableToColumns.get(schemaTable);
                String col = indexDef.substring(indexDef.lastIndexOf('(') + 1, indexDef.lastIndexOf(')'));
                if (col.contains(",") && indexname.startsWith(IndexMeta.INDEX_NAME_PREFIX_NQ)) {
                    IndexMeta constraintMeta = new IndexMeta();
                    List<String> columns = new ArrayList<>();
                    for (String c : col.split(",")) {
                        columns.add(c.trim());
                    }
                    constraintMeta.setColumns(columns);
                    constraintMeta.setTable(table);
                    if (indexDef.toUpperCase().contains(" " + IndexMeta.UNIQUE + " ")) {
                        constraintMeta.setType(IndexType.UNIQUE_FIELD);
                    } else {
                        constraintMeta.setType(IndexType.INDEX);
                    }
                    constraintMeta.setUsing(indexDef.replaceAll("^.* USING ([^( ]+).*$", "$1").trim());
                    List<IndexMeta> constraintMetas = tableToConstraintMeta.get(schema);
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
            List<IndexMeta> constraints = tableToConstraintMeta.getOrDefault(schemaTable, new ArrayList<>());
            String table = schemaTable;
            TableMeta tm = new TableMeta();
            tm.setTable(table);
            tm.setColumnMetas(new ArrayList<>(columnNameToCol.values()));
            tm.setIndexes(constraints);
            tm.setPrimaryKey("id");
            tm.freshConstraintMetaTable();
            result.put(schemaTable.toLowerCase().replace("public.",""), tm);
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

    public void init(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends BaseEntity>> entities) {
        this.jdbcHelper = jdbcHelper;
        this.idGenerator = idGenerator;
        this.classToTableMeta = new HashMap<>();
        this.nameToMeta = new HashMap<>();
        if (entities != null) {
            this.entities = entities;
            for (Class<? extends BaseEntity> tbl : entities) {
                TableMeta tm = TableMeta.createBy(tbl);
                tm.freshNameToField(tbl);
                tm.freshConstraintMetaTable();
                classToTableMeta.put(tbl, tm);
                nameToMeta.put(tm.getTable(), tm);
            }
        }

        Map<String, TableMeta> oldTableMeta = findTableMeta(jdbcHelper, nameToMeta.keySet());

        SqlPlanBuilder planBuilder = new SqlPlanBuilder();
        for (Class<?> tbl : classToTableMeta.keySet()) {
            TableMeta tm = classToTableMeta.get(tbl);
            String schemaTable = tm.getTable();
            TableMeta old = oldTableMeta.get(schemaTable);
            if (old == null) {
                planBuilder.append(tm.createTableSql());
            } else if (!old.equals(tm)) {
                planBuilder.append(old.upgradeTo(tm));
            }
        }
        List<String> sqlList = planBuilder.build(nameToMeta);

        if (!sqlList.isEmpty()) {
            jdbcHelper.batchExec(sqlList.toArray(new String[0]));
        }
    }

    public List<Class<? extends BaseEntity>> getEntities() {
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

    public <E extends BaseEntity> boolean drop(Class<E> tbl) {
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

    private static <E extends BaseEntity, M extends BaseTable> E createBy(Class<E> tbl, TableMeta tm, ResultSet rs) throws SQLException {
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
                        if(val != null && !val.getClass().equals(cm.getJavaBaseType())) {
                            Object fieldVal = OReflectUtil.strToBaseType(cm.getField().getType(), val.toString());
                            cm.getField().set(row, fieldVal);
                        }else {
                            cm.getField().set(row, val);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        OLog.error(e.getMessage());
                    }
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

    public <E extends BaseEntity, M extends BaseTable> E get(Class<E> tbl, Long id) {
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        String sql = String.format("SELECT * FROM %s WHERE id = ?", tm.getTable());
        final List<E> rows = new ArrayList<>(1);
        jdbcHelper.query(sql, new Object[]{id}, rs -> {
            E row = null;
            try {
                row = createBy(tbl, tm, rs);
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            } finally {
                rows.add(row);
            }
        });
        return rows.get(0);
    }

    public <E extends BaseEntity, M extends BaseTable> E insert(E entity) {
        batchInsert(Arrays.asList(entity));
        return entity;
    }

    public <E extends BaseEntity, M extends BaseTable> int batchInsert(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        Class<?> tbl = entities.get(0).getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));

        List<String> names = new ArrayList<>();
        for (ColumnMeta cm : tm.getColumnMetas()) {
            names.add(cm.getName());
        }
        List<Object[]> valArray = new ArrayList<>();
        for (E entity : entities) {
            tm.validate(entity, false);
            Object[] val = initEntity(tm, names, entity);
            valArray.add(val);
        }

        String stub = OUtils.genStub("?", ",", names.size());
        String sql = String.format("INSERT INTO %s(%s) VALUES(%s);", tm.getTable(), String.join(",", names), stub);
        int[] cntArray = jdbcHelper.batchUpdate(sql, valArray);
        int cnt = 0;
        for (int c : cntArray) {
            cnt += c;
        }
        return cnt;
    }

    private <E extends BaseEntity> Object[] initEntity(TableMeta tm, List<String> names, E entity) {
        if (entity.getId() == null) {
            Long id = idGenerator.next(entity.getClass());
            entity.setId(id);
        }
        Object[] val = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            try {
                Object v = tm.getColumnMetaByName(name).getField().get(entity);
                val[i] = v;
            } catch (IllegalAccessException e) {
                OLog.error(e.getMessage());
            }
        }
        return val;
    }

    private <E extends BaseEntity, M extends BaseTable> int update(E entity, boolean ignoreNull) {
        OAssert.warnning(entity != null, "不可以插入null");
        Class<?> tbl = entity.getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        tm.validate(entity, ignoreNull);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));

        List<String> names = new ArrayList<>();
        long id = entity.getId();
        for (ColumnMeta cm : tm.getColumnMetas()) {
            if (!cm.isPrimaryKey()) {
                names.add(cm.getName());
            }
        }
        Object[] val = initEntity(tm, names, entity);

        if (ignoreNull) {
            Iterator<String> iterator = names.iterator();
            List<Object> notNull = new ArrayList<>();
            for (Object v : val) {
                iterator.next();
                if (v != null) {
                    notNull.add(v);
                } else {
                    iterator.remove();
                }
            }
            val = notNull.toArray();
        }

        List<Object> args = new ArrayList<>();
        for (Object v : val) {
            args.add(v);
        }
        args.add(id);
        String sql = String.format("UPDATE %s SET %s=? WHERE id=?", tm.getTable(), String.join("=?,", names));
        return jdbcHelper.update(sql, args.toArray());
    }

    public <E extends BaseEntity, M extends BaseTable> int update(E entity) {
        return update(entity, false);
    }

    public <E extends BaseEntity, M extends BaseTable> int updateIgnoreNull(E entity) {
        return update(entity, true);
    }

    public <E extends BaseEntity, M extends BaseTable> int updateBy(Class<E> tbl, BaseTable<M> tpl) {
        return jdbcHelper.update(tpl.toString(), tpl.getArgs().toArray());
    }

    public <E> int deleteById(Class<E> tbl, Long id) {
        if (id == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        String sql = String.format("DELETE FROM %s WHERE id = ?", tm.getTable());
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

    public <E extends BaseEntity, M extends BaseTable> int delete(Class<E> tbl, BaseTable<M> cnd) {
        if (cnd == null || cnd.toString().trim().isEmpty()) {
            TableMeta tm = classToTableMeta.get(tbl);
            String sql = String.format("DELETE FROM %s;", tm.getTable());
            return jdbcHelper.update(sql, new Object[0]);
        } else {
            TableMeta tm = classToTableMeta.get(tbl);
            String sql = String.format("DELETE FROM %s %s;", tm.getTable(), cnd.toString());
            return jdbcHelper.update(sql, cnd.getArgs().toArray());
        }
    }

    public <E extends BaseEntity, M extends BaseTable> long count(Class<E> tbl) {
        return count(tbl, null);
    }

    public <E extends BaseEntity, M extends BaseTable> long count(Class<E> tbl, BaseTable<M> cnd) {
        if (cnd == null || cnd.toString().trim().isEmpty()) {
            TableMeta tm = classToTableMeta.get(tbl);
            String sql = String.format("SELECT COUNT(1) FROM %s;", tm.getTable());
            return (long) jdbcHelper.queryForObject(sql);

        } else {
            TableMeta tm = classToTableMeta.get(tbl);
            String withName = cnd.alias + "_with";
            String sql = String.format("WITH %s AS (%s) SELECT COUNT(1) FROM %s", withName, cnd.toString(), withName);
            return (long) jdbcHelper.queryForObject(sql, cnd.getArgs().toArray());
        }

    }

    public <E extends BaseEntity, M extends BaseTable> List<E> find(Class<E> tbl, BaseTable<M> cnd) {
        List<E> data = new ArrayList<>();
        find(tbl, cnd, e -> {
            data.add(e);
        });
        return data;
    }

    public <E extends BaseEntity, M extends BaseTable> Page<E> find(Class<E> tbl, BaseTable<M> cnd, int page, int pageSize) {
        if (cnd.select.length() == 0) {
            cnd.select();
        }
        if (cnd.from.length() == 0) {
            cnd.from();
        }

        Page<E> result = new Page<>();
        result.setPagesize(pageSize);
        List<E> data = new ArrayList<>(10);
        result.setData(data);
        if (page <= 0) {
            result.setTotal(count(tbl, cnd));
        }
        if (page != 0) {
            page = Math.abs(page) - 1;
        }
        result.setPage(page + 1);
        BaseTable<M> limitCnd = cnd.copy();

        limitCnd.limit(pageSize, pageSize * page);
        find(tbl, limitCnd, e -> {
            result.getData().add(e);
        });
        return result;
    }

    public <E extends BaseEntity, M extends BaseTable> E fetch(Class<E> tbl, BaseTable<M> cnd) {
        Page<E> page = find(tbl, cnd, 1, 1);
        if (page.getData().size() == 0) {
            return null;
        }
        return page.getData().get(0);
    }

    public <E extends BaseEntity, M extends BaseTable> void find(Class<E> tbl, BaseTable<M> cnd, Consumer<E> consumer) {
        TableMeta tm = classToTableMeta.get(tbl);
        if (tm == null) {
            return;
        }
        if (cnd.select.length() == 0) {
            cnd.select();
        }
        if (cnd.from.length() == 0) {
            cnd.from();
        }
        String sql = null;
        if(TblType.TABLE.equals(tm.getType())) {
            sql = cnd.toString();
        } else if(TblType.WITH.equals(tm.getType())) {
            sql = String.format("WITH %s AS (%s) %s ", tm.getTable(), tm.getViewDef().toSql() , cnd.toString());
        }
        jdbcHelper.query(sql, cnd.getArgs().toArray(new Object[0]), rs -> {
            E row = null;
            try {
                row = createBy(tbl, tm, rs);
                consumer.accept(row);
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            }
        });
    }

    public <E extends BaseEntity, M extends BaseTable> List<E> findByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<E>();
        }
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        String sql = String.format("SELECT * FROM %s WHERE id IN ()", tm.getTable(), OUtils.genStub("?", ",", ids.size()));
        final List<E> rows = new ArrayList<>(ids.size());
        jdbcHelper.query(sql, ids.toArray(), rs -> {
            try {
                E row = null;
                while (rs.next()) {
                    row = createBy(tbl, tm, rs);
                    rows.add(row);
                }
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            }
        });
        return rows;
    }
}