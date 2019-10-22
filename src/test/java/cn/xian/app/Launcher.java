package cn.xian.app;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.json.JSONUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.onceio.OnceIO;
import top.onceio.core.annotation.BeansIn;
import top.onceio.core.beans.BeansEden;

import javax.sql.DataSource;
import java.sql.Connection;

@BeansIn("cn.xian.app")
public class Launcher {

    public static void main(String[] args) {
        BeansEden.get().resolve(new String[]{"conf"}, new String[]{"cn.xian.app"});
        System.out.println(JSONUtils.toJSONString(BeansEden.get().load(DataSource.class)));
    }
}
