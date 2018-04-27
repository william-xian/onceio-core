package top.onceio.core.exception;

public class Failed extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int MSG = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;
	private int level;
	private String format;
	private Object[] args;
	private Object data;

	protected Failed(int level, String format, Object[] args) {
		super();
		this.level = level;
		this.format = format;
		this.args = args;
	}

	protected Failed(int level, String format, Object[] args, Object data) {
		super();
		this.level = level;
		this.format = format;
		this.args = args;
		this.data = data;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public static void throwMsg(String format, Object... args) {
		throw new Failed(MSG, format, args);
	}

	public static void throwWarring(String format, Object... args) {
		throw new Failed(WARN, format, args);
	}

	public static void throwError(String format, Object... args) {
		throw new Failed(ERROR, format, args);
	}

	public static void throwMsgData(Object data, String format, Object... args) {
		throw new Failed(MSG, format, args, data);
	}

	public static void throwWarringData(Object data, String format, Object... args) {
		throw new Failed(WARN, format, args, data);
	}

	public static void throwErrorData(Object data, String format, Object... args) {
		throw new Failed(ERROR, format, args, data);
	}

}
