package top.onceio;

import cn.xian.app.model.entity.Bill;
import cn.xian.app.model.entity.UserInfo;
import cn.xian.app.model.view.UserBillView;

public class Main {

    public static void main(String[] args) {
        /**增 单个添加，批量添加, 不重复添加*/
        /**删 主键删除，条件删除*/
        /**改 主键更改，非null更改，表达式更改，条件批量更改*/
        /**查 外连接，内连接，子查询，视图（子查询，With，视图，物化视图，union）*/
        leftJoin();
        subQuery();
        view();
    }

    public static void leftJoin() {
        UserInfo.Meta u = UserInfo.meta().as("u");
        Bill.Meta b = Bill.meta().as("b");
        u.select(u.name, u.age, b.amount)
                .from(u)
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18)
                .groupBy(u.name, b.amount)
                .orderBy(u.age)
                .limit(3, 5);
        System.out.println(u.toSql());
    }

    public static void subQuery() {
        UserInfo.Meta u = UserInfo.meta().as("u");
        Bill.Meta b = Bill.meta().as("b");
        b.select(b.userId).from().where().amount.gt(1);
        u.select(u.name, u.age)
                .from()
                .where().id.in(b);

        System.out.println(u.toSql());
    }

    public static void view() {
        UserBillView.Meta v = UserBillView.meta();
        v.select(v.name, v.age, v.amount).from().where().name.like("%a");
        System.out.println(v.toSql());
    }
}
