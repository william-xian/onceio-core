package cn.xian.app.model.entity2;

import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;

public class Bill extends BaseTable<Bill> {
    public BaseCol<Bill> userId = new BaseCol(this, "userId");
    public BaseCol<Bill> amount = new BaseCol(this, "amount");

    public Bill() {
        super("bill");
        super.bind(this);
    }

    public Bill(String alias) {
        super("bill", alias);
        super.bind(this);
    }

    public static Bill meta() {
        return new Bill();
    }

    public static Bill meta(String alias) {
        return new Bill(alias);
    }
}
