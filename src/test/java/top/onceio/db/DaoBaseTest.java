package top.onceio.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.internal.util.xml.impl.Input;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.jdbc2.optional.SimpleDataSource;
import top.onceio.core.beans.BeansEden;
import top.onceio.core.db.dao.impl.DaoHelper;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.tbl.OI18n;
import top.onceio.core.util.JsonConfLoader;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class DaoBaseTest {
	protected static JdbcHelper jdbcHelper = new JdbcHelper();

	protected static DaoHelper daoHelper = new DaoHelper();
	
	public static void initDao() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		BeansEden.get().resovle(new String[] {"src/test/java/conf/"},new String[] {"cn.xian.app"});
		jdbcHelper = BeansEden.get().load(JdbcHelper.class);
		daoHelper = BeansEden.get().load(DaoHelper.class);
	}
	
	@Test
	public void createTbl() {
		initDao();
		System.out.println(OI18n.class);
	}
	
}
