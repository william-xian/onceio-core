package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.tbl.BaseEntity;

@Tbl
public class GoodsDesc extends BaseEntity {
    @Col(nullable = false,ref = Goods.class)
    private Long id;
    @Col(size = 255, nullable = true)
    private String content;
    @Col(nullable = false)
    private Integer saled;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSaled() {
        return saled;
    }

    public void setSaled(Integer saled) {
        this.saled = saled;
    }

}
