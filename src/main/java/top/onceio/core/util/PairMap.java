package top.onceio.core.util;

import java.util.HashMap;
import java.util.Map;

public class PairMap<A,B> {
	private Map<A,B> a2b = new HashMap<>();
	private Map<B,A> b2a = new HashMap<>();
	
	public void put(A a, B b) {
		a2b.put(a, b);
		b2a.put(b, a);
	}
	
	public A getByB(B b) {
		return b2a.get(b);
	}
	
	public B getByA(A a) {
		return a2b.get(a);
	}
	
	public B removeByA(A a) {
		B b = a2b.get(a);
		if(b != null) {
			a2b.remove(a);
			b2a.remove(b);
		}
		return b;
	}
	
	public void clear() {
		a2b.clear();
		b2a.clear();
	}
}
