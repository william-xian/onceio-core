package cn.xian.app.model.view;

import cn.xian.app.model.entity.Bill;
import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.annotation.ModelType;
import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.db.model.BaseModel;
import top.onceio.core.db.model.DefView;
import top.onceio.core.db.model.Func;

@Model(type = ModelType.VIEW)
public class UserBillView extends BaseModel<Long> implements DefView {

    @Col(size = 32)
    protected String name;
    @Col
    protected int age;
    @Col
    protected Integer amount;

    @Override
    public BaseMeta def() {
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
