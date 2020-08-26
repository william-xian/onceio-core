package top.onceio.core.db.tbl;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.util.OUtils;

import java.io.Serializable;

/**
 * @author Administrator
 */
public abstract class BaseEntity<ID extends Serializable> {
    @Col(nullable = false)
    protected ID id;

    public BaseEntity() {
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return OUtils.toJson(this);
    }


}
