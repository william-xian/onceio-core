package cn.xian.app.model.entity2;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.BaseView;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.OEntity;
import top.onceio.core.util.OReflectUtil;

public class UserBillView extends OEntity implements BaseView {

    @Col(size = 32)
    protected String name;
    @Col
    protected int age;
    @Col
    protected Integer amount;

    @Override
    public String def() {
        User.Meta u = User.Meta.meta("u");
        Bill.Meta b = Bill.Meta.meta("b");
        u.select(u.name, u.age, b.amount)
                .from()
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18);
        u.groupBy(u.name, b.amount)
                .orderBy(u.age);
        return u.toString();
    }

    public static class Meta extends BaseTable<UserBillView.Meta> {
        public BaseCol<UserBillView.Meta> id = new BaseCol(this, OReflectUtil.getField(UserBillView.class,"id"));
        public StringCol<UserBillView.Meta> name = new StringCol(this, OReflectUtil.getField(UserBillView.class,"name"));
        public BaseCol<UserBillView.Meta> age = new BaseCol(this, OReflectUtil.getField(UserBillView.class,"age"));
        public BaseCol<UserBillView.Meta> amount = new BaseCol(this, OReflectUtil.getField(UserBillView.class,"amount"));

        public Meta() {
            super("user_bill_view");
            super.bind(this);
        }
        public Meta(String alias) {
            super("user_bill_view", alias);
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
