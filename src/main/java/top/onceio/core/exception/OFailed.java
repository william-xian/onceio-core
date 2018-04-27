package top.onceio.core.exception;

public class OFailed extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String format;
	private Object[] args;
	private Object data;

	public OFailed(String format) {
		super();
		this.format = format;
	}

	public OFailed(String format, Object[] args) {
		super();
		this.format = format;
		this.args = args;
	}

	public OFailed(String format, Object[] args, Object data) {
		super();
		this.format = format;
		this.args = args;
		this.data = data;
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

	public static OFailed wrap(Throwable e) {
		OFailed oe = new OFailed(e.getMessage());
		return oe;
	}
}
