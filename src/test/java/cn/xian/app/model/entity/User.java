package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class User extends BaseEntity {
    @Col(size = 32)
    protected String name;
    @Col
    protected int age;

    public static class Meta extends BaseEntity.Meta<Meta> {
        public StringCol<User.Meta> name = new StringCol(this, OReflectUtil.getField(User.class, "name"));
        public BaseCol<User.Meta> age = new BaseCol(this, OReflectUtil.getField(User.class, "age"));

        public Meta() {
            super("user");
            super.bind(this, User.class);
        }

        public static Meta meta() {
            return new Meta();
        }
    }


}
