package top.onceio.db;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.dao.Page;
import top.onceio.core.util.IDGenerator;
import top.onceio.core.util.OUtils;

public class DaoHelperTest extends DaoBaseTest {

    @BeforeClass
    public static void init() {
        initDao();
    }

    @AfterClass
    public static void cleanup() {
    }

    @After
    public void clean() {
        daoHelper.delete(UserInfo.class, UserInfo.Meta.meta());

    }

    @Test
    public void insert_get_remove_delete() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd" + i % 3);
            System.out.println(OUtils.toJson(uc));
            ucs.add(uc);
            ids.add(uc.getId());
        }
        int insertcnt = daoHelper.batchInsert(ucs);
        Assert.assertEquals(10, insertcnt);
        Assert.assertEquals(10, daoHelper.count(UserInfo.class));
        UserInfo uc = new UserInfo();
        uc.setId(IDGenerator.randomID());
        uc.setName("name-" + System.currentTimeMillis());
        uc.setGenre(100);
        uc.setAvatar("avatar");
        uc.setPasswd("passwd");
        daoHelper.insert(uc);
        Assert.assertEquals(11, daoHelper.count(UserInfo.class));
        UserInfo db = daoHelper.get(UserInfo.class, uc.getId());

        Assert.assertEquals(db.toString(), uc.toString());
        int deleted1 = daoHelper.deleteById(UserInfo.class, uc.getId());
        Assert.assertEquals(0, deleted1);
        Assert.assertEquals(11, daoHelper.count(UserInfo.class));
        /**
         *
         */
        int deleteRemoved1 = daoHelper.deleteById(UserInfo.class, uc.getId());
        Assert.assertEquals(1, deleteRemoved1);
        Assert.assertEquals(10, daoHelper.count(UserInfo.class));

        int deleted10 = daoHelper.deleteByIds(UserInfo.class, ids);
        Assert.assertEquals(0, deleted10);

        int deletedRemoved10 = daoHelper.deleteByIds(UserInfo.class, ids);
        Assert.assertEquals(10, deletedRemoved10);
        Assert.assertEquals(0, daoHelper.count(UserInfo.class));
    }

    @Test
    public void update_updateIgnoreNull() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);
        UserInfo uc1 = ucs.get(0);
        UserInfo uc2 = ucs.get(1);
        UserInfo uc3 = ucs.get(2);
        uc1.setName("t-name");
        uc1.setAvatar(null);
        daoHelper.update(uc1);
        UserInfo db1 = daoHelper.get(UserInfo.class, uc1.getId());
        Assert.assertEquals("t-name", db1.getName());
        Assert.assertNull(db1.getAvatar());
        UserInfo up2 = new UserInfo();
        up2.setId(uc2.getId());
        up2.setName("t-name-" + System.currentTimeMillis());
        daoHelper.updateIgnoreNull(up2);
        UserInfo db2 = daoHelper.get(UserInfo.class, uc2.getId());
        Assert.assertEquals(up2.getName(), db2.getName());
        Assert.assertNotNull(db2.getName());
        /** 无关数据没有被干扰 */
        UserInfo db3 = daoHelper.get(UserInfo.class, uc3.getId());
        Assert.assertEquals(uc3.toString(), db3.toString());
        daoHelper.deleteByIds(UserInfo.class, ids);
    }

    @Test
    public void updateByTpl() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);
        UserInfo uc1 = ucs.get(0);
        UserInfo uc2 = ucs.get(1);
        UserInfo.Meta tpl = UserInfo.Meta.meta();
        tpl.genre.setExp("+1").id.eq(uc1.getId());
        daoHelper.updateBy(UserInfo.class, tpl);
        UserInfo db1 = daoHelper.get(UserInfo.class, uc1.getId());
        Assert.assertEquals(1, db1.getGenre().intValue());
        UserInfo db2 = daoHelper.get(UserInfo.class, uc2.getId());
        Assert.assertEquals(uc2.toString(), db2.toString());
        daoHelper.deleteByIds(UserInfo.class, ids);
    }

    @Test
    public void find() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);
        System.out.println(OUtils.toJson(ucs));
        UserInfo.Meta cnd1 = UserInfo.Meta.meta();
        cnd1.genre.eq(2).or().genre.ne(3);
        UserInfo.Meta cnd3 = UserInfo.Meta.meta();
        cnd3.avatar.like("avatar%00");
        Assert.assertEquals(2, daoHelper.count(UserInfo.class, cnd3));
        UserInfo.Meta cnd4 = UserInfo.Meta.meta();
        /** (genre=2 or genre != 3) and not (avatar like 'avatar%00')*/
        cnd4.and(cnd1).not(cnd3);
        Assert.assertEquals(6, daoHelper.count(UserInfo.class, cnd4));
        cnd4.limit(-2,4);
        Page<UserInfo> page1 = daoHelper.find(UserInfo.class, cnd4);
        Assert.assertEquals(2, page1.getData().size());
        Assert.assertEquals(6, page1.getTotal().longValue());
        int cnt = daoHelper.deleteByIds(UserInfo.class, ids);
        System.out.println("delete - " + cnt);

    }

    @Test
    public void saveRemove() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
        }
        daoHelper.batchInsert(ucs);
        for (UserInfo uc : ucs) {
            ids.add(uc.getId());
        }
        System.out.println(OUtils.toJson(ucs));
        int cnt = daoHelper.delete(UserInfo.class, UserInfo.Meta.meta());
        System.out.println("delete - " + cnt);
    }

    @Test
    public void having_group_order_by() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setGenre(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);

        /*
        Cnd<UserInfo> cnd = new Cnd<UserInfo>(UserInfo.class);
        cnd.groupBy().use().setGenre(Tpl.USING_INT);

        SelectTpl<UserInfo> distinct = new SelectTpl<UserInfo>(UserInfo.class);
        distinct.using().setGenre(SelectTpl.USING_INT);
        Page<UserInfo> page = daoHelper.find(UserInfo.class, distinct, cnd);
        Assert.assertEquals(page.getTotal(), new Long(4));

        SelectTpl<UserInfo> max = new SelectTpl<UserInfo>(UserInfo.class);

        max.max().setGenre(SelectTpl.USING_INT);
        UserInfo ucMax = daoHelper.fetch(UserInfo.class, max, null);
        Assert.assertEquals(ucMax.getGenre(), new Integer(3));

        SelectTpl<UserInfo> min = new SelectTpl<UserInfo>(UserInfo.class);
        min.min().setGenre(SelectTpl.USING_INT);
        UserInfo ucMin = daoHelper.fetch(UserInfo.class, min, null);
        Assert.assertEquals(ucMin.getGenre(), new Integer(0));


        SelectTpl<UserInfo> sum = new SelectTpl<UserInfo>(UserInfo.class);
        sum.sum().setGenre(SelectTpl.USING_INT);
        UserInfo ucSum = daoHelper.fetch(UserInfo.class, sum, null);
        Assert.assertEquals(ucSum.getExtra().get("sum_genre"), new Long(13));

        SelectTpl<UserInfo> avg = new SelectTpl<UserInfo>(UserInfo.class);
        avg.avg().setGenre(SelectTpl.USING_INT);
        UserInfo ucAvg = daoHelper.fetch(UserInfo.class, avg, null);
        System.out.println(ucAvg);

        */
        daoHelper.deleteByIds(UserInfo.class, ids);
    }
}