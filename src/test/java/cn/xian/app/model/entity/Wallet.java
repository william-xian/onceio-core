package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.db.model.*;
import top.onceio.core.util.OReflectUtil;


@Tbl(autoCreate = true)
public class Wallet extends BaseEntity {
    @Col(nullable = false, ref = UserInfo.class)
    private Long id;
    @Col(nullable = true)
    private int balance;
    @Col(nullable = true)
    private int expenditure;
    @Col(nullable = true)
    private int income;

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getExpenditure() {
        return expenditure;
    }

    public void setExpenditure(int expenditure) {
        this.expenditure = expenditure;
    }

    public int getIncome() {
        return income;
    }

    public void setIncome(int income) {
        this.income = income;
    }
    
    
    

    public static class Meta extends BaseEntity.Meta<Meta>  {
        public BaseCol<Meta> id = new BaseCol(this, OReflectUtil.getField(Wallet.class, "id"));
        public BaseCol<Meta> balance = new BaseCol(this, OReflectUtil.getField(Wallet.class, "balance"));
        public BaseCol<Meta> expenditure = new BaseCol(this, OReflectUtil.getField(Wallet.class, "expenditure"));
        public BaseCol<Meta> income = new BaseCol(this, OReflectUtil.getField(Wallet.class, "income"));
        public Meta() {
            super("public.wallet");
            super.bind(this, Wallet.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
