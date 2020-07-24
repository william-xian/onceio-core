package cn.xian.app.holder;

import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.model.BaseTable;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.mvc.annocations.Param;

@AutoApi(UserInfo.class)
@Cacheable
public class UserHolder extends DaoHolder<UserInfo,UserInfo.Meta> {
    @Cacheable
    @Api
    public UserInfo fetchByName(@Param("name") String name) {
        return super.fetch(UserInfo.Meta.meta().name.eq(name));
    }

    @Override
    public int updateBy(BaseTable<UserInfo.Meta> cnd) {
        return 0;
    }
}
