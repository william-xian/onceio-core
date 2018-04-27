package top.onceio.core.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class AopProxy implements MethodInterceptor {

	private static final Map<Method, ProxyChain> methodToAopChain = new HashMap<>();

	public static ProxyChain get(Method method) {
		return methodToAopChain.get(method);
	}
	public static ProxyChain push(Method method, ProxyChain aopChain) {
		return methodToAopChain.put(method, aopChain);
	}
	public static void clear() {
		methodToAopChain.clear();
	}
	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		ProxyChain ac = methodToAopChain.get(method);
		if (ac != null) {
			return ac.run(obj, method, args, proxy);
		} else {
			return proxy.invokeSuper(obj, args);
		}
	}

}
