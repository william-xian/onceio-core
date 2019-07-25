package top.onceio.db;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import top.onceio.core.db.meta.DDEngine;
import top.onceio.core.util.OUtils;

public class DDEngineTest {

	/**
	User {id,name}
	UserAddr{id,addr}
	UserFriend {id,userId,friendId}
	Goods {id,name}
	GoodsDesc {id,saled,content}
	GoodsOrder {id,goodsId,userId,amount,money,ctime}
	GoodsShipping {id,userId,goodsOrderId,receiverId,addr }
	1.哪些用户在情人节给他的好友购买销量小于1000，价值大于1000元戒指？
	"select u.id userId,u.name username,ua.addr addr, g.name goodsName,go.ctime ctime
	from GoodsOrder go
	left join User u on go.userId = u.id
	left join UserAddr ua on go.userId = ua.id
	left join Goods g on g.id = go.goodsId
	left join GoodsDesc gd on go.goodsId = gd.id
	left join GoodsShipping gs on go.goodsOrderId = gs.id
	"*/
	@Test
	public void testJoin() {
		DDEngine dde = new DDEngine();
		dde.append("GoodsOrder {ctime};")
		.append("GoodsOrder.userId-User {id userId,name username};")
		.append("GoodsOrder.userId-UserAddr {addr};")
		.append("GoodsOrder.goodsId-Goods {name goodsName};")
		.append("GoodsOrder.goodsId-GoodsDesc {saled,content};")
		.append("GoodsOrder.goodsOrderId-GoodsShipping {goodsOrderId,receiverId,addr receiverAddr};")
			.build();
		System.out.println(OUtils.toJson(dde));
		Set<String> params = new HashSet<String>();
		params.add("content");
		params.add("receiverAddr");
		String sql = dde.genericJoinSqlByParams("GoodsOrder.goodsOrderId-GoodsShipping", params,null);
		System.out.println(sql);
	}
	/**
	2.在情人节，收到两位以上好友的礼物用户，好友是谁？
	"select gs.userId, u.name user, g.name goodsName,group_concat(uf.name)friends
	from GoodsShipping gs
	left join User u on gs.userId = u.id
	left join UserFriend uf on gs.receiverId = uf.id
	left join GoodsOrder go on gs.goodsOrderId = go.id
	left join Goods g on g.id = go.goodsId
	"
	*/
	@Test
	public void testJoin1() {
		DDEngine dde = new DDEngine();
		dde.append("O {uid, gid};")
		.append("O.uid-U {name uame, age};")
		.append("O.gid-G {name gname};")
		.append("O.uid-R.uid-R.fid-U {name fname,age fage};")
			.build();
		System.out.println(OUtils.toJson(dde));
		Set<String> params = new HashSet<String>();
		params.add("fname");
		String sql = dde.genericJoinSqlByParams("O", params,null);
		System.out.println(sql);
	}
	
}
