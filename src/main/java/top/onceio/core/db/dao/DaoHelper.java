package top.onceio.core.db.dao;



/**
 * 增 单个添加，批量添加, 不重复添加
 * 删 主键删除，条件删除
 * 改 主键更改，非null更改，表达式更改，条件批量更改
 * 查 外连接，内连接，子查询，视图（子查询，With，视图，物化视图，union）
 * 函数
 */
import org.apache.log4j.Logger;
import top.onceio.core.db.annotation.IndexType;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.IndexMeta;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.exception.Failed;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.function.Consumer;

/**增 单个添加，批量添加, 不重复添加*/
/**删 主键删除，条件删除*/
/**改 主键更改，非null更改，表达式更改，条件批量更改*/

/**查 外连接，内连接，子查询，视图（子查询，With，视图，物化视图）*/
public class DaoHelper implements DDLDao, TransDao {

    private static final Logger LOGGER = Logger.getLogger(DaoHelper.class);

    private JdbcHelper jdbcHelper;
    private Map<Class<?>, TableMeta> classToTableMeta;
    private Map<String, Class<?>> tableToClass;
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
                "left join pg_constraint uk on uk.conrelid = c.oid and uk.contype='u' and a.attnum = uk.conkey[1] and array_length(uk.conkey,0) = 1\n" +
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
                String schemaTable = schema + "." + table;
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
                    constraintMeta.setSchema(schema);
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
            String schema = schemaTable.split("\\.")[0];
            String table = schemaTable.split("\\.")[1];
            TableMeta tm = new TableMeta();
            tm.setTable(table);
            tm.setSchema(schema);
            tm.setColumnMetas(new ArrayList<>(columnNameToCol.values()));
            tm.setIndexes(constraints);
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

    public void init(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends BaseEntity>> entities) {
        this.jdbcHelper = jdbcHelper;
        this.idGenerator = idGenerator;
        this.classToTableMeta = new HashMap<>();
        this.tableToClass = new HashMap<>();
        if (entities != null) {
            this.entities = entities;
            for (Class<? extends BaseEntity> tbl : entities) {
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
                for (IndexMeta cm : tblMeta.getFieldConstraint()) {
                    if (cm.getType().equals(IndexType.FOREIGN_KEY)) {
                        sorted(order, cm.getRefTable());
                    }
                }
            }
            order.add(schemaTable);
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

    private static <E extends BaseEntity, M extends BaseEntity.Meta> E createBy(Class<E> tbl, TableMeta tm, ResultSet rs) throws SQLException {
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

    public <E extends BaseEntity, M extends BaseEntity.Meta> E get(Class<E> tbl, Long id) {
        BaseTable<M> cnd = new BaseTable<M>(null);
        Page<E> page = find(tbl, cnd);
        if (page.getData().size() == 1) {
            return page.getData().get(0);
        }
        return null;
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> E insert(E entity) {
        OAssert.warnning(entity != null, "不可以插入null");
        Class<?> tbl = entity.getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        tm.validate(entity, false);
        List<Object> vals = new ArrayList<>();//TODO idNameVal.getIdValsList().get(0);
        List<String> names = new ArrayList<>();//TODO idNameVal.getIdNames();
        String stub = OUtils.genStub("?", ",", names.size());
        String sql = String.format("INSERT INTO %s(%s) VALUES(%s);", tm.getTable(), String.join(",", names), stub);
        jdbcHelper.update(sql, vals.toArray());
        return entity;
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> int batchInsert(List<E> entities) {
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
        List<String> names = new ArrayList<>();//TODO idNameVal.getIdNames();
        List<List<Object>> valsList = new ArrayList<>();//TODO idNameVal.getIdValsList();

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

    private <E extends BaseEntity, M extends BaseEntity.Meta> int update(E entity, boolean ignoreNull) {
        OAssert.warnning(entity != null, "不可以插入null");
        Class<?> tbl = entity.getClass();
        TableMeta tm = classToTableMeta.get(tbl);
        tm.validate(entity, ignoreNull);
        OAssert.fatal(tm != null, "无法找到表：%s", TableMeta.getTableName(tbl));
        List<String> names = new ArrayList<>();//TODO idNameVal.getNames();
        List<Object> vals = new ArrayList<>();//TODO idNameVal.getValsList().get(0);
        String sql = String.format("UPDATE %s SET %s=? WHERE id=? AND rm = false;", tm.getTable(),
                String.join("=?,", names));
        vals.add(entity.getId());
        return jdbcHelper.update(sql, vals.toArray());
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> int update(E entity) {
        return update(entity, false);
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> int updateIgnoreNull(E entity) {
        return update(entity, true);
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> int updateBy(Class<E> tbl, BaseTable<M> tpl) {
        return 0;
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

    public <E extends BaseEntity, M extends BaseEntity.Meta> int delete(Class<E> tbl, BaseTable<M> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = classToTableMeta.get(tbl);
        List<Object> sqlArgs = new ArrayList<>();
        String whereCnd = "";//TODO cnd.whereSql(sqlArgs);
        String sql = String.format("DELETE FROM %s WHERE %s;", tm.getTable(), whereCnd);
        return jdbcHelper.update(sql, sqlArgs.toArray());
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> long count(Class<E> tbl) {
        return 0;
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> long count(Class<E> tbl, BaseTable<M> cnd) {
        return 0;
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> Page<E> find(Class<E> tbl, BaseTable<M> cnd) {
        return new Page<>();
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> E fetch(Class<E> tbl, BaseTable<M> cnd) {
        return null;
    }

    public <E extends BaseEntity, M extends BaseEntity.Meta> void find(Class<E> tbl, BaseTable<M> cnd, Consumer<E> consumer) {
        TableMeta tm = classToTableMeta.get(tbl);
        if (tm == null) {
            return;
        }
        List<Object> args = new ArrayList<>();
        StringBuffer sql = new StringBuffer();//TODO cnd.wholeSql(tm, tpl, args);
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

    public <E extends BaseEntity, M extends BaseEntity.Meta> List<E> findByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<E>();
        }
        BaseTable<M> cnd = new BaseTable<M>(null);
        Page<E> page = new Page<>();//TODO fetch(tbl, cnd);
        return page.getData();
    }
}
