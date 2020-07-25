package cn.xian.app.model.view;

import cn.xian.app.model.entity.Bill;
import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.annotation.TblType;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.DefView;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl(type = TblType.WITH)
public class UserBillView extends BaseEntity implements DefView {

    @Col(size = 32)
    protected String name;
    @Col
    protected int age;
    @Col
    protected Integer amount;

    @Override
    public BaseTable def() {
        UserInfo.Meta u = UserInfo.meta().alias("u");
        Bill.Meta b = Bill.meta().alias("b");
        u.select(u.name, u.age, b.amount)
                .from()
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18)
                .groupBy(u.name, b.amount)
                .orderBy(u.age);
        return u;
    }

    public static class Meta extends BaseEntity.Meta<Meta>  {
        public StringCol<Meta> name = new StringCol(this, OReflectUtil.getField(UserBillView.class, "name"));
        public BaseCol<Meta> age = new BaseCol(this, OReflectUtil.getField(UserBillView.class, "age"));
        public BaseCol<Meta> amount = new BaseCol(this, OReflectUtil.getField(UserBillView.class, "amount"));
        public Meta() {
            super("public.user_bill_view");
            super.bind(this, UserBillView.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
