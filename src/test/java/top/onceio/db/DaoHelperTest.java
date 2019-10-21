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
import top.onceio.core.db.dao.tpl.Cnd;
import top.onceio.core.db.dao.tpl.SelectTpl;
import top.onceio.core.db.dao.tpl.Tpl;
import top.onceio.core.db.dao.tpl.UpdateTpl;
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
        Cnd<UserInfo> cnd = new Cnd<>(UserInfo.class);
        daoHelper.remove(UserInfo.class, cnd);
        daoHelper.delete(UserInfo.class, cnd);

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
        int removed1 = daoHelper.removeById(UserInfo.class, uc.getId());
        Assert.assertEquals(1, removed1);
        int deleteRemoved1 = daoHelper.deleteById(UserInfo.class, uc.getId());
        Assert.assertEquals(1, deleteRemoved1);
        Assert.assertEquals(10, daoHelper.count(UserInfo.class));

        int deleted10 = daoHelper.deleteByIds(UserInfo.class, ids);
        Assert.assertEquals(0, deleted10);

        int deleteRemoved10 = daoHelper.removeByIds(UserInfo.class, ids);
        Assert.assertEquals(10, deleteRemoved10);
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
        uc3.setRm(false);
        Assert.assertEquals(uc3.toString(), db3.toString());
        daoHelper.removeByIds(UserInfo.class, ids);
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
        UpdateTpl<UserInfo> tpl = new UpdateTpl<>(UserInfo.class);
        tpl.set().setId(uc1.getId());
        tpl.add().setGenre(1);
        daoHelper.updateByTpl(UserInfo.class, tpl);
        UserInfo db1 = daoHelper.get(UserInfo.class, tpl.getId());
        Assert.assertEquals(1, db1.getGenre().intValue());
        UserInfo db2 = daoHelper.get(UserInfo.class, uc2.getId());
        uc2.setRm(false);
        Assert.assertEquals(uc2.toString(), db2.toString());
        daoHelper.removeByIds(UserInfo.class, ids);
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
        Cnd<UserInfo> cnd1 = new Cnd<>(UserInfo.class);
        cnd1.eq().setGenre(2);
        cnd1.or().ne().setGenre(3);
        Cnd<UserInfo> cnd3 = new Cnd<>(UserInfo.class);
        cnd3.like().setAvatar("avatar%00");
        Assert.assertEquals(2, daoHelper.count(UserInfo.class, cnd3));
        Cnd<UserInfo> cnd4 = new Cnd<>(UserInfo.class);
        /** (genre=2 or genre != 3) and not (avatar like 'avatar%00')*/
        cnd4.and(cnd1).not(cnd3);
        Assert.assertEquals(6, daoHelper.count(UserInfo.class, cnd4));

        cnd4.setPage(-2);
        cnd4.setPagesize(4);
        Page<UserInfo> page1 = daoHelper.find(UserInfo.class, cnd4);
        Assert.assertEquals(2, page1.getData().size());
        Assert.assertEquals(6, page1.getTotal().longValue());
        Cnd<UserInfo> rm = new Cnd<>(UserInfo.class);
        rm.in(ids.toArray()).setId(Tpl.USING_LONG);
        int cnt = daoHelper.remove(UserInfo.class, rm);
        System.out.println("removed - " + cnt);
        cnt = daoHelper.deleteByIds(UserInfo.class, ids);
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

        Cnd<UserInfo> rm = new Cnd<>(UserInfo.class);
        rm.in(ids.toArray()).setId(Tpl.USING_LONG);
        int cnt = 0;

        cnt = daoHelper.removeByIds(UserInfo.class, ids);
        System.out.println("removed ids - " + cnt);
        cnt = daoHelper.remove(UserInfo.class, rm);
        System.out.println("removed - " + cnt);
        Cnd<UserInfo> cnd = new Cnd<>(UserInfo.class);

        cnt = (int) daoHelper.remove(UserInfo.class, cnd);
        System.out.println("removed - " + cnt);

        cnt = daoHelper.delete(UserInfo.class, cnd);
        System.out.println("delete - " + cnt);
    }

    @Test
    public void having_group_orderby() {
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

        Cnd<UserInfo> cnd = new Cnd<UserInfo>(UserInfo.class);
        cnd.groupBy().use().setGenre(Tpl.USING_INT);

        SelectTpl<UserInfo> distinct = new SelectTpl<UserInfo>(UserInfo.class);
        distinct.using().setGenre(SelectTpl.USING_INT);
        Page<UserInfo> page = daoHelper.findByTpl(UserInfo.class, distinct, cnd);
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

        daoHelper.removeByIds(UserInfo.class, ids);
        daoHelper.deleteByIds(UserInfo.class, ids);
    }
}