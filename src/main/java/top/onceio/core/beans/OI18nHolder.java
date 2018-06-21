package top.onceio.core.beans;

import java.util.Locale;

import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.dao.tpl.Cnd;
import top.onceio.core.db.tbl.OI18n;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.mvc.annocations.Param;
import top.onceio.core.util.OUtils;

@AutoApi(OI18n.class)
@Cacheable
public class OI18nHolder extends DaoHolder<OI18n> {
	
	@Api("/translate")
	public String translate(@Param("msg")String msg, @Param("lang")String lang) throws Exception {
		if (lang != null && !lang.equals(Locale.getDefault().getLanguage())) {
			String key = "msg/" + lang + "_" + OUtils.encodeMD5(msg);
			Cnd<OI18n> cnd = new Cnd<>(OI18n.class);
			cnd.eq().setOid(key);
			OI18n i18n = fetch(null, cnd);
			if (i18n != null) {
				msg = i18n.getName();
			}
		}
		return msg;
	}

}
