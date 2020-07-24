package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class Bill extends BaseEntity {
    @Col
    protected Long userId;
    @Col
    protected Integer amount;


    public static class Meta extends BaseEntity.Meta<Meta>  {
        public BaseCol<Meta> userId = new BaseCol(this, OReflectUtil.getField(Bill.class, "userId"));
        public BaseCol<Meta> amount = new BaseCol(this, OReflectUtil.getField(Bill.class, "amount"));
        public Meta() {
            super("public.Bill");
            super.bind(this, Bill.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
