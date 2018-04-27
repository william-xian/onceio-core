package top.onceio.core.aop;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodProxy;

public abstract class ProxyAction {
	ProxyAction next = null;
	public ProxyAction next() {
		return next;
	}
	public abstract Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable;
}
