package cn.xian.app.model.view;

import cn.xian.app.model.entity.Bill;
import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.annotation.TblType;
import top.onceio.core.db.model.*;
import top.onceio.core.db.tbl.BaseEntity;

@Tbl(type = TblType.VIEW)
public class UserBillView extends BaseEntity<Long> implements DefView {

    @Col(size = 32)
    protected String name;
    @Col
    protected int age;
    @Col
    protected Integer amount;

    @Override
    public BaseTable def() {
        UserInfo.Meta u = UserInfo.meta().as("u");
        Bill.Meta b = Bill.meta().as("b");
        u.select(u.name, u.age, Func.sum(b.amount))
                .from()
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18)
                .groupBy(u.name, u.age)
                .orderBy(u.age);
        return u;
    }

}
