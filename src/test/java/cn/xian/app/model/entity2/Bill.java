package cn.xian.app.model.entity2;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.db.tbl.OEntity;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class Bill extends OEntity {
    @Col
    protected Long userId ;
    @Col
    protected Integer amount;

    public static class Meta extends BaseTable<Bill.Meta> {
        public BaseCol<Bill.Meta> id = new BaseCol(this, OReflectUtil.getField(Bill.class,"id"));
        public BaseCol<Bill.Meta> userId = new BaseCol(this, OReflectUtil.getField(Bill.class,"userId"));
        public BaseCol<Bill.Meta> amount = new BaseCol(this, OReflectUtil.getField(Bill.class,"amount"));

        public Meta() {
            super("bill");
            super.bind(this);
        }

        public Meta(String alias) {
            super("bill", alias);
            super.bind(this);
        }

        public static Meta meta() {
            return new Meta();
        }

        public static Meta meta(String alias) {
            return new Meta(alias);
        }
    }


}
