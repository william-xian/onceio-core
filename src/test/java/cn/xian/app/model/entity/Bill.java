package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Index;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl(indexes = {@Index(columns = {"user_id", "merchant_id"})})
public class Bill extends BaseEntity {
    @Col
    protected Long userId;
    @Col
    protected Long merchantId;
    @Col
    protected Integer amount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public static class Meta extends BaseEntity.Meta<Meta>  {
        public BaseCol<Meta> userId = new BaseCol(this, OReflectUtil.getField(Bill.class, "userId"));
        public BaseCol<Meta> amount = new BaseCol(this, OReflectUtil.getField(Bill.class, "amount"));
        public Meta() {
            super("public.bill");
            super.bind(this, Bill.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
