package top.onceio.core.db.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import top.onceio.core.exception.Failed;

public class JdbcHelper {

    private static final Logger LOGGER = Logger.getLogger(JdbcHelper.class);

    private static ThreadLocal<Connection> trans = new ThreadLocal<Connection>();

    private DataSource dataSource;


    public DataSource getDataSource() {
        return dataSource;
    }

    public DBType getDBType() {
        return DBType.POSTGRESQL;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Attempts to change the transaction isolation level for this
     * <code>Connection</code> object to the one given. The constants defined in
     * the interface <code>Connection</code> are the possible transaction
     * isolation levels.
     * <p>
     * <B>Note:</B> If this method is called during a transaction, the result is
     * implementation-defined.
     *
     * @param level one of the following <code>Connection</code> constants:
     *              <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     *              <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     *              <code>Connection.TRANSACTION_REPEATABLE_READ</code>, or
     *              <code>Connection.TRANSACTION_SERIALIZABLE</code>. (Note that
     *              <code>Connection.TRANSACTION_NONE</code> cannot be used
     *              because it specifies that transactions are not supported.)
     * @param readOnly 是否是只读
     * @return isCreated
     * @see DatabaseMetaData#supportsTransactionIsolationLevel
     */
    public boolean beginTransaction(int level, boolean readOnly) {
        boolean created = false;
        try {
            Connection conn = trans.get();
            if (conn == null) {
                created = true;
                conn = dataSource.getConnection();
                conn.setReadOnly(readOnly);
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(level);
                trans.set(conn);
            } else {
                if (conn.getTransactionIsolation() < level) {
                    conn.setTransactionIsolation(level);
                }
            }
        } catch (SQLException e) {
            Failed.throwError(e.getMessage());
        }
        return created;
    }

    public Savepoint setSavepoint() {
        Savepoint sp = null;
        Connection conn = trans.get();
        try {
            sp = conn.setSavepoint();
        } catch (SQLException e) {
            Failed.throwError(e.getMessage());
        }
        return sp;
    }

    public void rollback(Savepoint sp) {
        Connection conn = trans.get();
        if (conn != null) {
            try {
                conn.rollback(sp);
                conn.releaseSavepoint(sp);
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            }
        }
    }

    public void rollback() {
        Connection conn = trans.get();
        if (conn != null) {
            try {
                conn.rollback();
                trans.remove();
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            }
        }
    }

    public void commit() {
        Connection conn = trans.get();
        if (conn != null) {
            try {
                conn.commit();
                trans.remove();
            } catch (SQLException e) {
                Failed.throwError(e.getMessage());
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 返回数据中list[0] 是字段名，list[1-n]是字段所对应的数据
     * @param sql 使用替代符的SQL语句
     * @param args SQL参数列表
     * @return list[n]:row data list[0] is the columnNames,list[1] is the first row data of thus columns.
     */
    public List<Object[]> call(String sql, Object[] args) {
        List<Object[]> result = new LinkedList<>();
        Connection conn = trans.get();
        PreparedStatement stat = null;
        ResultSet rs = null;
        boolean usingTrans = false;
        if (conn == null) {
            if (dataSource != null) {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        } else {
            usingTrans = true;
        }
        if (conn != null) {
            try {
                stat = conn.prepareCall(sql, ResultSet.FETCH_UNKNOWN, ResultSet.CONCUR_UPDATABLE);
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        stat.setObject(i + 1, args[i]);
                    }
                }
                rs = stat.executeQuery();
                ResultSetMetaData md = rs.getMetaData();
                Object[] rowNames = new Object[md.getColumnCount()];
                for (int cc = 1; cc <= md.getColumnCount(); cc++) {
                    rowNames[cc - 1] = md.getColumnName(cc);
                }
                result.add(rowNames);
                while (rs.next()) {
                    Object[] row = new Object[md.getColumnCount()];
                    for (int cc = 1; cc <= md.getColumnCount(); cc++) {
                        row[cc - 1] = rs.getObject(cc);
                    }
                    result.add(row);
                }
                rs.close();
            } catch (SQLException e) {
                Failed.throwMsg(e.getMessage());
            } finally {
                if (stat != null) {
                    try {
                        stat.close();
                    } catch (SQLException e) {
                        Failed.throwMsg(e.getMessage());
                    }
                }
                if (conn != null && !usingTrans) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Failed.throwMsg(e.getMessage());
                    }
                }
            }
        }
        return result;
    }

    public int[] batchExec(String... sqls) {
        Connection conn = trans.get();
        Statement stat = null;
        int[] result = null;
        boolean usingTrans = false;
        if (conn == null) {
            if (dataSource != null) {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        } else {
            usingTrans = true;
        }
        try {
            stat = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            for (String sql : sqls) {
                stat.addBatch(sql);
            }
            stat.setMaxRows(sqls.length);
            result = stat.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            Failed.throwMsg(e.getMessage());
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    Failed.throwMsg(e.getMessage());
                }
            }
            if (conn != null && !usingTrans) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Failed.throwMsg(e.getMessage());
                }
            }
        }
        return result;
    }

    private int[] batchExec(String sql, List<Object[]> args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        Connection conn = trans.get();
        PreparedStatement stat = null;
        int[] result = null;
        boolean usingTrans = false;
        if (conn == null) {
            if (dataSource != null) {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        } else {
            usingTrans = true;
        }
        try {
            stat = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            for (Object[] arr : args) {
                for (int i = 0; i < arr.length; i++) {
                    stat.setObject(i + 1, arr[i]);
                }
                stat.addBatch();
            }
            stat.setMaxRows(args.size());
            result = stat.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            Failed.throwMsg(e.getMessage());
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    Failed.throwMsg(e.getMessage());
                }
            }
            if (conn != null && !usingTrans) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Failed.throwMsg(e.getMessage());
                }
            }
        }
        return result;
    }

    public void query(String sql, Object[] args, Consumer<ResultSet> consumer) {
        Connection conn = trans.get();
        PreparedStatement stat = null;
        ResultSet rs = null;
        boolean usingTrans = false;
        if (conn == null) {
            if (dataSource != null) {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        } else {
            usingTrans = true;
        }
        try {
            stat = conn.prepareStatement(sql, ResultSet.FETCH_UNKNOWN, ResultSet.CONCUR_READ_ONLY);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    stat.setObject(i + 1, args[i]);
                }
            }
            rs = stat.executeQuery();
            while (rs.next()) {
                consumer.accept(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Failed.throwMsg(e.getMessage());
        } finally {

            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    Failed.throwMsg(e.getMessage());
                }
            }

            if (conn != null && !usingTrans) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Failed.throwMsg(e.getMessage());
                }
            }
        }
    }

    public Object queryForObject(String sql) {
        return queryForObject(sql, null);
    }

    public Object queryForObject(String sql, Object[] args) {
        List<Object[]> list = call(sql, args);
        if (list.size() == 2) {
            return list.get(1)[0];
        }
        return null;
    }

    public int[] batchUpdate(String sql) {
        return batchUpdate(sql, null);
    }

    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        return batchExec(sql, batchArgs);
    }

    public int update(String sql, Object[] args) {
        int cnt = 0;
        Connection conn = trans.get();
        PreparedStatement stat = null;
        boolean usingTrans = false;
        if (conn == null) {
            if (dataSource != null) {
                try {
                    conn = dataSource.getConnection();
                } catch (SQLException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        } else {
            usingTrans = true;
        }
        try {
            stat = conn.prepareStatement(sql, ResultSet.FETCH_UNKNOWN, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            LOGGER.debug(sql);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    stat.setObject(i + 1, args[i]);
                }
            }
            cnt = stat.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            Failed.throwMsg(e.getMessage());
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    Failed.throwMsg(e.getMessage());
                }
            }
            if (conn != null && !usingTrans) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Failed.throwMsg(e.getMessage());
                }
            }
        }
        return cnt;
    }

}
