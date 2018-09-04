package top.onceio.core.util;

import java.util.HashMap;
import java.util.Map;

public class PairMap<A, B> {
	private Map<A, B> a2b = null;
	private Map<B, A> b2a = null;

	public PairMap() {
		a2b = new HashMap<>();
		b2a = new HashMap<>();
	}

	public PairMap(int initialCapacity) {
		a2b = new HashMap<>(initialCapacity);
		b2a = new HashMap<>(initialCapacity);
	}

	public A getA(B b) {
		return b2a.get(b);
	}

	public B getB(A a) {
		return a2b.get(a);
	}

	public void put(A a, B b) {
		a2b.put(a, b);
		b2a.put(b, a);

	}

	public B removeByA(A a) {
		B b = a2b.remove(a);
		if (b != null) {
			b2a.remove(b);
		}
		return b;
	}

	public A removeByB(B b) {
		A a = b2a.remove(b);
		if (a != null) {
			a2b.remove(a);
		}
		return a;
	}

	public void clear() {
		a2b.clear();
		b2a.clear();
	}
}
