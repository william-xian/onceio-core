package top.onceio.core.exception;

import java.util.HashMap;
import java.util.Map;

public class ValidateFailed extends Failed {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Map<String, Object> data = new HashMap<>();

    protected ValidateFailed(int code, String format, Object[] args) {
        super(code, format, args);
        super.setData(data);
    }

    public void throwSelf() {
        throw this;
    }

    public ValidateFailed put(String key, String value) {
        data.put(key, value);
        return this;
    }

    public static ValidateFailed create(String format, Object... args) {
        ValidateFailed vf = new ValidateFailed(400, format, args);
        return vf;
    }
}
