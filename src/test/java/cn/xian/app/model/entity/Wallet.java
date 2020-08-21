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

}
