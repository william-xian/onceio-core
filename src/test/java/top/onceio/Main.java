package top.onceio;


import cn.xian.app.model.entity2.Bill;
import cn.xian.app.model.entity2.User;

public class Main {

    public static void main(String[] args) {
        User u = User.meta("u");
        Bill b = Bill.meta("b");

        u.select(u.name, u.age, b.amount)
                .from()
                .join(b).on(u.id, b.userId)
                .where().name.like("%a").and().age.gt(18);

        u.groupBy(u.name, b.amount)
                .orderBy(u.age)
                .limit(3, 5);

        System.out.println(u);
    }

}
