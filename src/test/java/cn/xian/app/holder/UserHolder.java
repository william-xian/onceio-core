package cn.xian.app.holder;

import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.dao.tpl.Cnd;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.mvc.annocations.Param;

@AutoApi(UserInfo.class)
@Cacheable
public class UserHolder extends DaoHolder<UserInfo> {
	@Cacheable
	@Api
	public UserInfo fetchByName(@Param("name") String name) {
		Cnd<UserInfo> cnd = new Cnd<>(UserInfo.class);
		cnd.eq().setName(name);
		return super.fetch(null,cnd);
	}
}
