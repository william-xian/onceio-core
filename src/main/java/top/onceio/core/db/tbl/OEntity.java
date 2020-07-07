package top.onceio.core.db.tbl;

import java.util.HashMap;
import java.util.Map;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.util.OUtils;

/**
 * @author Administrator
 */
public abstract class OEntity {
    @Col(nullable = false)
    private Long id;
    @Col(nullable = false)
    private transient Boolean rm;
    /**
     * 用户存储额外数据，如 聚合函数
     */
    protected Map<String, Object> extra;

    public OEntity() {
    }

    public void init() {
    }

    public void initId() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getRm() {
        return rm;
    }

    public void setRm(Boolean rm) {
        this.rm = rm;
    }

    public Map<String, Object> put(String key, Object val) {
        if (extra == null) {
            extra = new HashMap<String, Object>();
        }
        extra.put(key, val);
        return extra;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return OUtils.toJson(this);
    }
}
