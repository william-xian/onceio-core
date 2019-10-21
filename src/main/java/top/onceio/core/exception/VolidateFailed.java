package top.onceio.core.exception;

import java.util.HashMap;
import java.util.Map;

public class VolidateFailed extends Failed {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Map<String, Object> data = new HashMap<>();

    protected VolidateFailed(int level, String format, Object[] args) {
        super(level, format, args);
        super.setData(data);
    }

    public void throwSelf() {
        throw this;
    }

    public VolidateFailed put(String key, String value) {
        data.put(key, value);
        return this;
    }

    public static VolidateFailed createError(String format, Object... args) {
        VolidateFailed vf = new VolidateFailed(Failed.ERROR, format, args);
        return vf;
    }
}
