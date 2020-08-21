package top.onceio.core.db.tbl;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.util.OUtils;

/**
 * @author Administrator
 */
public abstract class BaseEntity {
    @Col(nullable = false)
    protected Long id;

    public BaseEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return OUtils.toJson(this);
    }


}
