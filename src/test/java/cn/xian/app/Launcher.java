package cn.xian.app;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.onceio.core.annotation.BeansIn;
import top.onceio.core.beans.BeansEden;

import javax.sql.DataSource;
import java.sql.Connection;

@BeansIn("cn.xian.app")
public class Launcher {
	
	public static void main(String[] args) {

		DruidDataSource dds = new DruidDataSource();
		dds.setUrl("jdbc:postgresql://dbserver/testdb");
		dds.setUsername("admin");
		dds.setPassword("zg@0715");
		DataSource ds = dds;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			Connection conn = ds.getConnection();
			System.out.println(gson.toJson(conn.getCatalog()));
			System.out.println(gson.toJson(conn.getClientInfo()));

			System.out.println(gson.toJson(conn.getMetaData().getURL()));
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
