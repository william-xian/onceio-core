package top.onceio.core.db.dao.tpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import top.onceio.core.db.tbl.OTableMeta;
import top.onceio.core.exception.Failed;
import top.onceio.core.util.IDGenerator;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OUtils;

public class DaoHelper implements DDLDao, TransDao {
    private static final Logger LOGGER = Logger.getLogger(DaoHelper.class);

    private JdbcHelper jdbcHelper;
    private Map<String, TableMeta> tableToTableMeta;
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
                    String.format("SELECT count(*) FROM %s", tbl.getSimpleName().toLowerCase()));
        } catch (Failed e) {
            cnt = -1L;
        }
        if (cnt != null && cnt >= 0) {
            return true;
        } else {
            return false;
        }
    }

    private List<TableMeta> findPGTableMeta(JdbcHelper jdbcHelper) {
        List<TableMeta> result = new ArrayList<>();
        String qColumns = "select *\n" +
                "from information_schema.columns\n" +
                "where table_schema not in ('information_schema','pg_catalog')\n" +
                "ORDER BY table_schema,table_name";
        Map<String, Map<String, ColumnMeta>> tableToColumns = new HashMap<>();
        jdbcHelper.query(qColumns, null, (rs) -> {
            try {
                String table = rs.getString("table_schema") + "." + rs.getString("table_name");
                Map<String, ColumnMeta> columnMetaList = tableToColumns.get(table);
                if (columnMetaList == null) {
                    columnMetaList = new HashMap<>();
                    tableToColumns.put(table, columnMetaList);
                }
                ColumnMeta cm = new ColumnMeta();
                cm.setName(rs.getString("column_name"));
                cm.setNullable(rs.getString("is_nullable").equals("YES"));
                String udtName = rs.getString("udt_name");
                if (udtName.equals("bool")) {
                    cm.setJavaBaseType(Boolean.TYPE);
                } else if (udtName.equals("int2")) {
                    cm.setJavaBaseType(Short.TYPE);
                } else if (udtName.equals("int4")) {
                    cm.setJavaBaseType(Integer.TYPE);
                } else if (udtName.equals("int8")) {
                    cm.setJavaBaseType(Long.TYPE);
                } else if (udtName.equals("float4")) {
                    cm.setJavaBaseType(Float.TYPE);
                } else if (udtName.equals("float8")) {
                    cm.setJavaBaseType(Double.TYPE);
                } else if (udtName.equals("varchar") || udtName.equals("text")) {
                    cm.setJavaBaseType(String.class);
                } else if (udtName.equals("timestamptz") || udtName.equals("timestamp")) {
                    cm.setJavaBaseType(Timestamp.class);
                } else if (udtName.equals("time") || udtName.equals("date")) {
                    cm.setJavaBaseType(Date.class);
                }
                columnMetaList.put(cm.getName(), cm);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });

        String qIndexes = "select * from pg_indexes";
        jdbcHelper.query(qIndexes, null, (rs) -> {
            try {
                String table = rs.getString("schemaname") + "." + rs.getString("tablename");
                String indexDef = rs.getString("indexdef");
                //TODO
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        });
        return result;
    }

    private List<TableMeta> findTableMeta(JdbcHelper jdbcHelper) {
        switch (jdbcHelper.getDBType()) {
            case POSTGRESQL:
                return findPGTableMeta(jdbcHelper);
        }
        return null;
    }

    public void init(JdbcHelper jdbcHelper, IdGenerator idGenerator, List<Class<? extends OEntity>> entities) {
        this.jdbcHelper = jdbcHelper;
        this.idGenerator = idGenerator;
        this.tableToTableMeta = new HashMap<>();
        TableMeta tm = TableMeta.createBy(OTableMeta.class);
        tableToTableMeta.put(tm.getTable().toLowerCase(), tm);
        Cnd<OTableMeta> cnd = new Cnd<>(OTableMeta.class);
        List<TableMeta> page = findTableMeta(jdbcHelper);
        for (TableMeta old : page) {
            old.getFieldConstraint();
            old.freshConstraintMetaTable();
            old.freshNameToField();
            tableToTableMeta.put(old.getTable().toLowerCase(), old);
        }
        if (entities != null) {
            this.entities = entities;
            Map<String, List<String>> tblSqls = new HashMap<>();
            for (Class<? extends OEntity> tbl : entities) {
                List<String> sqls = this.createOrUpdate(tbl);
                /** 说明有变更之处  */
                if (sqls != null) {
                    /** 说明有数据库字段更改 */
                    if (!sqls.isEmpty()) {
                        tblSqls.put(tbl.getSimpleName().toLowerCase(), sqls);
                    }
                }
            }
            List<String> order = new ArrayList<>();
            for (String tbl : tblSqls.keySet()) {
                sorted(tbl, order);
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

    }

    private void sorted(String tbl, List<String> order) {
        if (!order.contains(tbl)) {
            TableMeta tblMeta = tableToTableMeta.get(tbl.toLowerCase());
            if (tblMeta != null) {
                for (ConstraintMeta cm : tblMeta.getFieldConstraint()) {
                    if (cm.getType().equals(ConstraintType.FOREGIN_KEY)) {
                        sorted(cm.getRefTable(), order);
                    }
                }
            }
            order.add(tbl);
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

    public Map<String, TableMeta> getTableToTableMata() {
        return tableToTableMeta;
    }

    public void setTableToTableMata(Map<String, TableMeta> tableToTableMeta) {
        this.tableToTableMeta = tableToTableMeta;
    }

    public <E extends OEntity> List<String> createOrUpdate(Class<E> tbl) {
        TableMeta old = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        if (old == null) {
            old = TableMeta.createBy(tbl);
            List<String> sqls = old.createTableSql();
            tableToTableMeta.put(old.getTable().toLowerCase(), old);
            return sqls;
        } else {
            TableMeta tm = TableMeta.createBy(tbl);
            if (old.equals(tm)) {
            } else {
                List<String> sqls = old.upgradeTo(tm);
                tableToTableMeta.put(tm.getTable().toLowerCase(), tm);
                return sqls;
            }
        }
        return null;
    }

    public <E extends OEntity> boolean drop(Class<E> tbl) {
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        if (tm == null) {
            return false;
        }
        String sql = String.format("DROP TABLE IF EXISTS %s;", tbl.getSimpleName().toLowerCase());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());

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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        tm.validate(entity, ignoreNull);
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
        String sql = String.format("UPDATE %s SET rm=true WHERE id=?", tm.getTable());
        return jdbcHelper.update(sql, new Object[]{id});
    }

    public <E> int removeByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        String stub = OUtils.genStub("?", ",", ids.size());
        String sql = String.format("UPDATE %s SET rm=true WHERE rm = false AND id IN (%s)", tm.getTable(), stub);
        LOGGER.debug(sql);
        return jdbcHelper.update(sql, ids.toArray());
    }

    public <E extends OEntity> int remove(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        List<Object> sqlArgs = new ArrayList<>();
        String whereCnd = cnd.whereSql(sqlArgs);
        String sql = String.format("UPDATE %s SET rm=true WHERE %s", tm.getTable(), whereCnd);
        return jdbcHelper.update(sql, sqlArgs.toArray());
    }

    public <E extends OEntity> int recovery(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        String sql = String.format("DELETE FROM %s WHERE id=?", tm.getTable());
        return jdbcHelper.update(sql, new Object[]{id});
    }

    public <E> int deleteByIds(Class<E> tbl, List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        String stub = OUtils.genStub("?", ",", ids.size());
        String sql = String.format("DELETE FROM %s WHERE id IN (%s) ", tm.getTable(), stub);
        return jdbcHelper.update(sql, ids.toArray());
    }

    public <E extends OEntity> int delete(Class<E> tbl, Cnd<E> cnd) {
        if (cnd == null)
            return 0;
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
        List<Object> sqlArgs = new ArrayList<>();
        String sql = cnd.countSql(tm, tpl, sqlArgs);
        LOGGER.debug(sql);
        return (Long) jdbcHelper.queryForObject(sql, sqlArgs.toArray(new Object[0]));
    }

    public <E extends OEntity> Page<E> find(Class<E> tbl, Cnd<E> cnd) {
        return findByTpl(tbl, null, cnd);
    }

    public <E extends OEntity> Page<E> findByTpl(Class<E> tbl, SelectTpl<E> tpl, Cnd<E> cnd) {
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
        OAssert.fatal(tm != null, "无法找到表：%s", tbl.getSimpleName());
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
        TableMeta tm = tableToTableMeta.get(tbl.getSimpleName().toLowerCase());
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
