package top.onceio.core.cache.impl;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import top.onceio.core.cache.Cache;

public class FIFOMemoryCache implements Cache {

	private Map<Object, SoftReference<Object>> objs;

	private LinkedList<Object> keys;

	private String name;

	private int size;

	private int length;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public FIFOMemoryCache(int size) {
		this.size = size;
		this.length = 0;
		if (objs == null) {
			objs = new HashMap<>(size);
		}
		if (keys == null) {
			keys = new LinkedList<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Class<T> type) {
		SoftReference<Object> sf = objs.get(key);
		if (sf == null) {
			return null;
		}
		Object obj = sf.get();
		return (T) obj;
	}

	@Override
	public void put(Object key, Object value) {
		if (!objs.containsKey(key)) {
			this.length++;
			keys.add(key);
			if (this.length >= size) {
				Object oldKey = keys.removeFirst();
				objs.remove(oldKey);
			}
		} else {
			keys.remove(key);
			keys.add(key);
		}
		objs.put(key, new SoftReference<Object>(value));
	}

	@Override
	public void evict(Object key) {
		this.length--;
		objs.remove(key);
		keys.add(key);
	}

	@Override
	public void clear() {
		this.length = 0;
		objs.clear();
		keys.clear();
	}

}
