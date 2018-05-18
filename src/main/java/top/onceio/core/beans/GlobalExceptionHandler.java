package top.onceio.core.beans;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import top.onceio.core.db.dao.Dao;
import top.onceio.core.db.dao.tpl.Cnd;
import top.onceio.core.db.tbl.OI18n;
import top.onceio.core.exception.Failed;
import top.onceio.core.util.OUtils;

public class GlobalExceptionHandler {
	private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class);
	Dao<OI18n> dao;

	public Map<String, Object> failedHandler(Locale locale, Failed failed) throws Exception {
		String defaultFromat = failed.getFormat();
		String lang = locale == null ? null : locale.getLanguage();
		if (lang != null && !lang.equals(Locale.getDefault().getLanguage())) {
			String key = "msg/" + lang + "_" + OUtils.encodeMD5(failed.getFormat());
			Cnd<OI18n> cnd = new Cnd<>(OI18n.class);
			cnd.eq().setOid(key);
			OI18n i18n = dao.fetch(null, cnd);
			if (i18n != null) {
				defaultFromat = i18n.getName();
			}
		}
		String msg = String.format(defaultFromat, failed.getArgs());
		LOGGER.error(String.format("ERROR: %s", msg));
		Map<String, Object> result = new HashMap<>();
		if (failed.getData() != null) {
			result.put("data", failed.getData());
		}
		switch (failed.getLevel()) {
		case Failed.ERROR:
			result.put("error", msg);
			break;
		case Failed.WARN:
			result.put("warnning", msg);
			break;
		case Failed.MSG:
			result.put("msg", msg);
			break;
		}
		return result;
	}

	public Map<String, String> defaultErrorHandler(Exception e) throws Exception {
		LOGGER.error(String.format("ERROR: %s", e.getMessage()));
		Map<String, String> result = new HashMap<>();
		result.put("error", e.getMessage());
		return result;
	}
}