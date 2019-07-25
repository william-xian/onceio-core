package top.onceio.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import top.onceio.core.util.FieldPathPicker;

public class FieldPathPickerTest {

	@Test
	public void test_array() {
		int[] arrInt = new int[]{1,2,3,4};
		FieldPathPicker fpp = new FieldPathPicker(int[].class,"[2]");

		Assert.assertEquals(3, (int)(fpp.getField(arrInt)));
	}

	@Test
	public void test_List() {
		List<Integer> arryList = new ArrayList<>();
		arryList.add(1);
		arryList.add(2);
		arryList.add(3);
		arryList.add(4);
		FieldPathPicker fpp = new FieldPathPicker(List.class,"[2]");
		Assert.assertEquals(3, (int)(fpp.getField(arryList)));
	}
	
	@Test
	public void test_Map() {
		Map<String,Object> map = new HashMap<>();
		Map<String,Object> a = new TreeMap<>();
		a.put("age", 15);
		map.put("a", a);
		FieldPathPicker fpp = new FieldPathPicker(Map.class,"a.age");
		Assert.assertEquals(15, (int)(fpp.getField(map)));
	}
	@Test
	public void test_Object() {
		A a = new A();
		B b = new B();
		a.setA(a);
		a.setB(b);
		a.setName("name-a");
		b.setV(new int[]{1,2,3});
		FieldPathPicker fppName = new FieldPathPicker(A.class,"name");
		Assert.assertEquals("name-a", (String)(fppName.getField(a)));
		FieldPathPicker fppAname = new FieldPathPicker(A.class,"a.name");
		Assert.assertEquals("name-a", (String)(fppAname.getField(a)));
		FieldPathPicker fppAAname = new FieldPathPicker(A.class,"a.a.name");
		Assert.assertEquals("name-a", (String)(fppAAname.getField(a)));
		FieldPathPicker fppBV1 = new FieldPathPicker(A.class,"b.v[1]");
		Assert.assertEquals(2, (int)(fppBV1.getField(a)));
	}
	class A {
		private String name;
		private A a;
		private B b;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public A getA() {
			return a;
		}
		public void setA(A a) {
			this.a = a;
		}
		public B getB() {
			return b;
		}
		public void setB(B b) {
			this.b = b;
		}
		
	}
	class B {
		private int[] v;

		public int[] getV() {
			return v;
		}
		
		public void setV(int[] v) {
			this.v = v;
		}
		
	}
	
}
