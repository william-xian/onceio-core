package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Index;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.tbl.BaseEntity;

@Tbl(indexes = {@Index(columns = {"buyer_id", "receiver_id"})})
public class GoodsShipping extends BaseEntity {
    @Col(ref = GoodsOrder.class, nullable = false)
    private long goodsOrderId;
    @Col(ref = UserInfo.class, nullable = false)
    private long buyerId;
    @Col(ref = UserInfo.class, nullable = false)
    private long receiverId;
    @Col(size = 255)
    private String address;

    public long getGoodsOrderId() {
        return goodsOrderId;
    }

    public void setGoodsOrderId(long goodsOrderId) {
        this.goodsOrderId = goodsOrderId;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(long buyerId) {
        this.buyerId = buyerId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String addr) {
        this.address = addr;
    }
}
