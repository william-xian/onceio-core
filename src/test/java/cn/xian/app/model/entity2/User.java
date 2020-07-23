package cn.xian.app.model.entity2;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.OEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class User extends OEntity {
    @Col(size = 32)
    protected String name;
    @Col
    protected int age;

    public static class Meta extends BaseTable<User.Meta> {
        public BaseCol<User.Meta> id = new BaseCol(this, OReflectUtil.getField(User.class,"id"));
        public StringCol<User.Meta> name = new StringCol(this, OReflectUtil.getField(User.class,"name"));
        public BaseCol<User.Meta> age = new BaseCol(this, OReflectUtil.getField(User.class,"age"));

        public Meta() {
            super("user");
            super.bind(this);
        }

        public Meta(String alias) {
            super("user", alias);
            super.bind(this);
        }

        public static Meta meta() {
            return new Meta();
        }

        public static Meta meta(String alias) {
            return new Meta(alias);
        }
    }


}
