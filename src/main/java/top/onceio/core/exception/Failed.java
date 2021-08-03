package top.onceio.core.exception;

public class Failed extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private int code;
    private String format;
    private Object[] args;
    private Object data;

    protected Failed(int code, String format, Object[] args) {
        super();
        this.code = code;
        this.format = format;
        this.args = args;
    }

    protected Failed(int code, String format, Object[] args, Object data) {
        super();
        this.code = code;
        this.format = format;
        this.args = args;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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


    public static void fail(String format, Object... args) {
        throw new Failed(500, format, args);
    }

    public static void fail(int code,String format, Object... args) {
        throw new Failed(code, format, args);
    }

    @Override
    public String toString() {
        return "Failed : " + String.format(format, args);
    }
}
