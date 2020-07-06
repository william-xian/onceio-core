package cn.xian.app.model.entity2;

import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.StringCol;

public class User extends BaseTable<User> {

    public StringCol<User> name = new StringCol(this, "name");
    public BaseCol<User> age = new BaseCol(this, "age");


    public User() {
        super("user");
        super.bind(this);
    }

    public User(String alias) {
        super("user", alias);
        super.bind(this);
    }

    public static User meta() {
        return new User();
    }

    public static User meta(String alias) {
        return new User(alias);
    }
}
