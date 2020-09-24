package cn.xian.app.model;

import top.onceio.core.annotation.I18nCfgBrief;

public enum Gender {
    @I18nCfgBrief("男")
    MALE(1),
    @I18nCfgBrief("女")
    FEMALE(0);
    public int val;
    Gender(int val) {
        this.val = val;
    }

}
