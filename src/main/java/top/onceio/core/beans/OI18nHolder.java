package top.onceio.core.beans;

import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.tables.OI18n;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.mvc.annocations.Param;
import top.onceio.core.util.OUtils;

import java.util.Locale;

@AutoApi(OI18n.class)
@Cacheable
public class OI18nHolder extends DaoHolder<OI18n, OI18n.Meta> {

    @Api("/translate")
    public String translate(@Param("msg") String msg, @Param("lang") String lang) {
        if (lang != null && !lang.equals(Locale.getDefault().getLanguage())) {
            String key = "msg/" + lang + "_" + OUtils.encodeMD5(msg);
            OI18n i18n = fetch(OI18n.meta().id.eq(key));
            if (i18n != null) {
                msg = i18n.getName();
            }
        }
        return msg;
    }
}
