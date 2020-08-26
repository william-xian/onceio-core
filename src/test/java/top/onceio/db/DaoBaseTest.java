package top.onceio.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import top.onceio.core.beans.BeansEden;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.model.DaoHelper;
import top.onceio.core.db.tbl.OI18n;

public class DaoBaseTest {
    protected static JdbcHelper jdbcHelper = new JdbcHelper();

    protected static DaoHelper daoHelper;

    public static void initDao() {
        BeansEden.get().resolve(new String[]{"src/test/resources/conf/"}, new String[]{"cn.xian.app"});
        jdbcHelper = BeansEden.get().load(JdbcHelper.class);
        daoHelper = BeansEden.get().load(DaoHelper.class);
    }

    @Test
    public void createTbl() {
        initDao();
        System.out.println(OI18n.class);
    }

}
