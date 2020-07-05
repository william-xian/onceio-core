package cn.xian.app.model.view;

import cn.xian.app.model.entity.GoodsOrder;
import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.TblView;

import java.util.function.Function;

@TblView(def ="select go.*,g.name goodsName,g.genre genre,ui.name username \n" +
        "from goodsorder go\n" +
        "left join  userinfo ui on ui.id = go.userid\n" +
        "left join goods g on g.id = go.goodsid")
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
