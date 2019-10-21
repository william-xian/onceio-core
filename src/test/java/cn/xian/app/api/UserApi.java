package cn.xian.app.api;

import java.util.Map;

import cn.xian.app.logic.UserLogic;
import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.annotation.Using;
import top.onceio.core.db.dao.Page;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.Param;

@Api("/user")
public class UserApi {

    @Using
    private UserLogic userService;

    @Api("/signup/{username}")
    public UserInfo signup(@Param("username") String username, @Param("passwd") String passwd) {
        return userService.signup(username, passwd);
    }

    @Api("/signin/{username}")
    public boolean signin(@Param("username") String username, @Param("passwd") String passwd) {
        return userService.signin(username, passwd);
    }

    @Api("/find")
    public Page<UserInfo> find(@Param() UserInfo uc) {
        return userService.find(uc);
    }

    @Api("/transfer")
    public Map<String, Object> transfer(@Param("from") Long from, @Param("to") Long to, @Param("v") Integer v) {
        return userService.transfer(from, to, v);
    }
}
