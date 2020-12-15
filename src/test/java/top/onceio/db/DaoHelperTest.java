package top.onceio.db;

import java.util.ArrayList;
import java.util.List;

import cn.xian.app.model.Gender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.xian.app.model.entity.UserInfo;
import top.onceio.core.db.dao.Page;
import top.onceio.core.db.model.Func;
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
        daoHelper.delete(UserInfo.class, UserInfo.meta());
    }

    @Test
    public void insert_get_remove_delete() {
        List<UserInfo> ucs = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserInfo uc = new UserInfo();
            uc.setId(IDGenerator.randomID());
            uc.setName("name" + i + "-" + System.currentTimeMillis());
            uc.setAge(i % 4);
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
        uc.setAge(100);
        uc.setAvatar("avatar");
        uc.setPasswd("passwd");
        daoHelper.insert(uc);
        Assert.assertEquals(11, daoHelper.count(UserInfo.class));

        int deleteRemoved1 = daoHelper.deleteById(UserInfo.class, uc.getId());
        Assert.assertEquals(1, deleteRemoved1);
        Assert.assertEquals(10, daoHelper.count(UserInfo.class));

        int deleted10 = daoHelper.deleteByIds(UserInfo.class, ids);
        Assert.assertEquals(10, deleted10);

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
            uc.setAge(i % 4);
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
            uc.setAge(i);
            uc.setAge(i);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);
        UserInfo uc1 = ucs.get(0);
        UserInfo uc2 = ucs.get(1);
        UserInfo.Meta tpl = UserInfo.meta();
        tpl.age.setExp("1").id.eq(uc1.getId());
        daoHelper.updateBy(UserInfo.class, tpl);
        UserInfo db1 = daoHelper.get(UserInfo.class, uc1.getId());
        Assert.assertEquals(1, db1.getAge());
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
            uc.setAge(i % 4);
            if(i%2==0) {
                uc.setGenre(Gender.FEMALE);
            }else {
                uc.setGenre(Gender.MALE);
            }
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);
        System.out.println(OUtils.toJson(ucs));
        UserInfo.Meta cnd1 = UserInfo.meta();
        cnd1.genre.eq(Gender.FEMALE).or().genre.ne(Gender.MALE);
        UserInfo.Meta cnd3 = UserInfo.meta();
        cnd3.select().from().where().avatar.like("avatar%00");
        Assert.assertEquals(2, daoHelper.count(UserInfo.class, cnd3));

        UserInfo.Meta cnd4 = UserInfo.meta();
        cnd4.genre.eq(Gender.MALE).or().genre.ne(Gender.FEMALE);
        UserInfo.Meta cnd5 = UserInfo.meta();
        cnd5.select().from().where().avatar.notLike("avatar%00").and(cnd4);
        /** (genre=2 or genre != 3) and not (avatar like 'avatar%00')*/
        System.out.println(cnd5.toSql());
        Assert.assertEquals(5, daoHelper.count(UserInfo.class, cnd5));

        UserInfo.Meta cnd6 = UserInfo.meta().avatar.notLike("avatar%00").and(cnd4);

        //TODO
        Page<UserInfo> page1 = daoHelper.find(UserInfo.class, cnd6, -2, 4);
        Assert.assertEquals(1, page1.getData().size());
        Assert.assertEquals(5, page1.getTotal().longValue());
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
            uc.setAge(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
        }
        daoHelper.batchInsert(ucs);
        for (UserInfo uc : ucs) {
            ids.add(uc.getId());
        }
        System.out.println(OUtils.toJson(ucs));
        int cnt = daoHelper.delete(UserInfo.class, UserInfo.meta());
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
            uc.setGenre(Gender.MALE);
            uc.setAge(i % 4);
            uc.setAvatar(String.format("avatar%d%d", i % 2, i % 3));
            uc.setPasswd("passwd");
            ucs.add(uc);
            ids.add(uc.getId());
        }
        daoHelper.batchInsert(ucs);

        UserInfo.Meta meta = UserInfo.meta();
        meta.select(meta.age);
        meta.groupBy(meta.age);

        List<UserInfo> data = daoHelper.find(UserInfo.class, meta);
        Assert.assertEquals(4, data.size());

        UserInfo.Meta max = UserInfo.meta();
        max.select(Func.max(max.age));

        UserInfo ucMax = daoHelper.fetch(UserInfo.class, max);
        Assert.assertEquals(3,ucMax.getAge());

        UserInfo.Meta min = UserInfo.meta();
        max.select(Func.min(min.age));
        UserInfo ucMin = daoHelper.fetch(UserInfo.class, min);
        Assert.assertEquals(0, ucMin.getAge());

        UserInfo.Meta sum = UserInfo.meta();
        sum.select(Func.sum(sum.age));
        UserInfo ucSum = daoHelper.fetch(UserInfo.class, sum);
        Assert.assertEquals(13, ucSum.getAge());

        UserInfo.Meta avg = UserInfo.meta();
        avg.select(Func.avg(avg.age));
        UserInfo ucAvg = daoHelper.fetch(UserInfo.class, avg);
        System.out.println(ucAvg);

        daoHelper.deleteByIds(UserInfo.class, ids);
    }
}