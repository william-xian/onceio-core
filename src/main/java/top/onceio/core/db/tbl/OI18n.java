package top.onceio.core.db.tbl;

import top.onceio.core.annotation.I18nCfg;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.util.OUtils;
import top.onceio.core.db.model.*;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class OI18n extends BaseEntity {
    @Col(size = 64, nullable = false)
    private String oid;
    @Col(size = 255, nullable = false)
    private String name;
    @Col(size = 32, nullable = true)
    private String val;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String toString() {
        return OUtils.toJson(this);
    }

    public static String msgId(String lang, String format) {
        return "msg/" + lang + "_" + OUtils.encodeMD5(format);
    }

    public static String constId(String lang, Class<?> clazz, String fieldName) {
        I18nCfg group = clazz.getAnnotation(I18nCfg.class);
        return "const/" + group.value() + "_" + clazz.getSimpleName() + "_" + fieldName;
    }
    
    public static class Meta extends BaseEntity.Meta<Meta>  {
        public BaseCol<Meta> oid = new BaseCol(this, OReflectUtil.getField(OI18n.class, "oid"));
        public BaseCol<Meta> name = new BaseCol(this, OReflectUtil.getField(OI18n.class, "name"));
        public BaseCol<Meta> val = new BaseCol(this, OReflectUtil.getField(OI18n.class, "val"));
        public Meta() {
            super("public.OI18n");
            super.bind(this, OI18n.class);
        }
        public static Meta meta() {
            return new Meta();
        }
    }

}
