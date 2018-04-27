package top.onceio.core.aop.proxies;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.aop.ProxyAction;
import top.onceio.core.aop.annotation.Aop;
import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.beans.BeansEden;
import top.onceio.core.cache.Cache;

@Aop(order = "cache-3-cacheable")
public class CacheableProxy extends ProxyAction {

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		Object result = null;
		Cacheable cacheable = method.getAnnotation(Cacheable.class);
		if (cacheable != null) {
			Cache cache = BeansEden.get().load(Cache.class, cacheable.cacheName());
			if (cache != null) {
				String argkey = CacheKeyResovler.extractKey(method, cacheable.key(), args);
				String key = cacheable.cacheName() + argkey;
				result = cache.get(key, method.getReturnType());
				if (result == null) {
					result = proxy.invokeSuper(obj, args);
					cache.put(key, result);
				}
			} else {
				result = proxy.invokeSuper(obj, args);
			}
		} else {
			result = proxy.invokeSuper(obj, args);
		}
		return result;
	}

}