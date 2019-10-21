package cn.xian.app.model.view;

import cn.xian.app.model.entity.GoodsOrder;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.TblView;

@TblView
public class GoodsOrderView extends GoodsOrder {
    //@Col
    //private Long rowNum;
    @Col(refBy = "userId-UserInfo.name")
    private String username;
    @Col(refBy = "goodsId-Goods.name")
    private String goodsName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

}
