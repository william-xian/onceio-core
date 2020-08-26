package top.onceio.core.db.model;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.util.OUtils;

import java.io.Serializable;

/**
 * @author Administrator
 */
public abstract class BaseModel<ID extends Serializable> {
    @Col(nullable = false)
    protected ID id;

    public BaseModel() {
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
