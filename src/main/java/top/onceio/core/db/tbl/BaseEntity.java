package top.onceio.core.db.tbl;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.util.OReflectUtil;
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

    public static class Meta<M> extends BaseTable<M> {
        public BaseCol<?> id;

        public Meta(String table) {
            super(table);
            id = new BaseCol(this, OReflectUtil.getField(BaseEntity.class, "id"));
        }

        public static Meta meta(String table) {
            return new Meta(table);
        }
    }
}
