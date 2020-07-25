package cn.xian.app.model.entity;

import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.tbl.BaseEntity;
import top.onceio.core.db.model.*;
import top.onceio.core.util.OReflectUtil;

@Tbl
public class UserInfo extends BaseEntity {
    @Col(nullable = false, size = 32, unique = true)
    private String name;
    @Col(nullable = true, size = 64)
    private String passwd;
    @Col(nullable = true, size = 255)
    private String avatar;
    @Col(nullable = true, size = 255)
    private Integer genre;
    @Col
    protected int age;

    public String getName() {
        return name;
    }

    public UserInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getPasswd() {
        return passwd;
    }

    public UserInfo setPasswd(String passwd) {
        this.passwd = passwd;
        return this;
    }

    public String getAvatar() {
        return avatar;
    }

    public UserInfo setAvatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    public Integer getGenre() {
        return genre;
    }

    public UserInfo setGenre(Integer genre) {
        this.genre = genre;
        return this;
    }


    public static class Meta extends BaseEntity.Meta<Meta>  {
        public StringCol<Meta> name = new StringCol(this, OReflectUtil.getField(UserInfo.class, "name"));
        public StringCol<Meta> passwd = new StringCol(this, OReflectUtil.getField(UserInfo.class, "passwd"));
        public StringCol<Meta> avatar = new StringCol(this, OReflectUtil.getField(UserInfo.class, "avatar"));
        public BaseCol<Meta> genre = new BaseCol(this, OReflectUtil.getField(UserInfo.class, "genre"));
        public BaseCol<Meta> age = new BaseCol(this, OReflectUtil.getField(UserInfo.class, "age"));
        public Meta() {
            super("public.user_info");
            super.bind(this, UserInfo.class);
        }
    }
    public static Meta meta() {
        return new Meta();
    }

}
