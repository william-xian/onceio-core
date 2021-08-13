package cn.xian.app.model.entity;

import cn.xian.app.model.Gender;
import org.checkerframework.checker.units.qual.C;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.model.BaseModel;

@Model
public class UserInfo extends BaseModel<Long> {
    @Col(nullable = false, size = 32, unique = true)
    private String name;
    @Col(nullable = true, size = 64)
    private String passwd;
    @Col(nullable = true, size = 255)
    private String avatar;
    @Col(nullable = true, size = 255)
    private Gender genre;
    @Col
    private int age;
    @Col(defaultValue = "false")
    private boolean removed;

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

    public Gender getGenre() {
        return genre;
    }

    public UserInfo setGenre(Gender genre) {
        this.genre = genre;
        return this;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
