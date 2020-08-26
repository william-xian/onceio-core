package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.model.BaseModel;


@Model(autoCreate = true)
public class Wallet extends BaseModel<Long> {
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
