package top.onceio.core.beans;

import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.tbl.OI18n;
import top.onceio.core.mvc.annocations.AutoApi;

@AutoApi(OI18n.class)
@Cacheable
public class OI18nHolder extends DaoHolder<OI18n> {

}
