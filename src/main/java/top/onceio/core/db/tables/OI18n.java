package top.onceio.core.db.tables;

import top.onceio.core.annotation.I18nCfg;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.db.model.BaseModel;
import top.onceio.core.util.OUtils;

@Model
public class OI18n extends BaseModel<String> {
    @Col(size = 64, nullable = false)
    protected String id;
    @Col(size = 255, nullable = false)
    private String name;
    @Col(size = 32, nullable = true)
    private String val;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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



    public static class Meta extends BaseMeta<Meta> {
        public top.onceio.core.db.model.StringCol<Meta> id = new top.onceio.core.db.model.StringCol(this, top.onceio.core.util.OReflectUtil.getField(OI18n.class, "id"));
        public top.onceio.core.db.model.StringCol<Meta> name = new top.onceio.core.db.model.StringCol(this, top.onceio.core.util.OReflectUtil.getField(OI18n.class, "name"));
        public top.onceio.core.db.model.StringCol<Meta> val = new top.onceio.core.db.model.StringCol(this, top.onceio.core.util.OReflectUtil.getField(OI18n.class, "val"));
        public Meta() {
            super.bind("o_i18n",this, OI18n.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
