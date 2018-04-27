package top.onceio.core.aop.proxies;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.aop.ProxyAction;
import top.onceio.core.aop.annotation.Aop;
import top.onceio.core.aop.annotation.Transactional;
import top.onceio.core.beans.BeansEden;
import top.onceio.core.db.jdbc.JdbcHelper;

@Aop(order="tran")
public class TransactionalProxy extends ProxyAction {
	
	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		Object result = null;
		Transactional trans = method.getAnnotation(Transactional.class);
		if (trans != null) {
			JdbcHelper jdbcHelper = BeansEden.get().load(JdbcHelper.class);
			boolean created = jdbcHelper.beginTransaction(trans.isolation(), trans.readOnly());
			try {
				result = proxy.invokeSuper(obj, args);
				if (created) {
					jdbcHelper.commit();
				}
			} catch (Exception e) {
				if (created) {
					jdbcHelper.rollback();
				}
				throw e;
			}
		} else {
			result = proxy.invokeSuper(obj, args);
		}
		return result;
	}
}
