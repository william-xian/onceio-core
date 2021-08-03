package top.onceio.core.exception;

import java.sql.SQLException;

public class SQLFailed extends Failed {
    SQLException exception;

    public SQLException getSQLException() {
        return exception;
    }

    protected SQLFailed(int code, String format, Object[] args) {
        super(code, format, args);
    }

    protected SQLFailed(int code, String format, Object[] args, Object data) {
        super(code, format, args, data);
    }

    public static SQLFailed fail(SQLException e) {
        SQLFailed failed = new SQLFailed(500, e.getMessage(), new Object[0]);
        failed.exception = e;
        throw failed;
    }
}
