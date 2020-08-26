package cn.xian.app.model.entity;

import com.oracle.webservices.internal.api.databinding.DatabindingMode;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Index;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.db.model.*;
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

}
