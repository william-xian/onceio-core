package top.onceio.core.exception;

import java.sql.SQLException;

public class SqlFailed extends OFailed {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SqlFailed(String format) {
		super(format);
	}

	public SqlFailed(String format, Object[] args) {
		super(format, args);
	}

	public SqlFailed(String format, Object[] args, Object data) {
		super(format, args, data);
	}

	public static SqlFailed wrap(SQLException e) {
		SqlFailed f = new SqlFailed(e.getMessage());
		return f;
	}
}
