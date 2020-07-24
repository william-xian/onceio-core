package cn.xian.app.model.view;

import cn.xian.app.model.entity.Bill;
import cn.xian.app.model.entity.User;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.TblView;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseView;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;

@TblView()
public class UserBillView extends BaseEntity implements BaseView {

    @Col(size = 32)
    protected String name;
    @Col
    protected int age;
    @Col
    protected Integer amount;

    @Override
    public String def() {
        User.Meta u = User.Meta.meta().alias("u");
        Bill.Meta b = Bill.Meta.meta().alias("b");
        u.select(u.name, u.age, b.amount)
                .from()
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18);
        u.groupBy(u.name, b.amount)
                .orderBy(u.age);
        return u.toString();
    }

    public static class Meta extends BaseEntity.Meta<Meta> {
        public StringCol<UserBillView.Meta> name = new StringCol(this, OReflectUtil.getField(UserBillView.class, "name"));
        public BaseCol<UserBillView.Meta> age = new BaseCol(this, OReflectUtil.getField(UserBillView.class, "age"));
        public BaseCol<UserBillView.Meta> amount = new BaseCol(this, OReflectUtil.getField(UserBillView.class, "amount"));

        public Meta() {
            super("user_bill_view");
            super.bind(this, UserBillView.class);
        }

        public static Meta meta() {
            return new Meta();
        }
    }


}
