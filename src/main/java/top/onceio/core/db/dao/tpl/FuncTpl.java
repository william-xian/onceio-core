package top.onceio.core.db.dao.tpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.util.OReflectUtil;

public abstract class FuncTpl<E> extends Tpl {
	protected E tpl;
	protected List<String> funcs = new ArrayList<>();
	protected List<String> argNames = new ArrayList<>();

	public FuncTpl() {
		Type t = FuncTpl.class.getTypeParameters()[0];
		@SuppressWarnings("unchecked")
		Class<E> tplClass = (Class<E>) OReflectUtil.searchGenType(FuncTpl.class, this.getClass(), t);
		init(tplClass);
	}
	public FuncTpl(Class<E> tplClass) {
		init(tplClass);
	}
	@SuppressWarnings("unchecked")
	protected void init(Class<E> tplClass) {
		FuncSetterProxy cglibProxy = new FuncSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (E) enhancer.create();
	}

	public E count() {
		funcs.add("COUNT");
		return tpl;
	}

	public E max() {
		funcs.add("MAX");
		return tpl;
	}

	public E min() {
		funcs.add("MIN");
		return tpl;
	}

	public E sum() {
		funcs.add("SUM");
		return tpl;
	}

	public E avg() {
		funcs.add("AVG");
		return tpl;
	}

	public E distinct() {
		funcs.add("DISTINCT");
		return tpl;
	}
	
	public List<String> getArgNames() {
		return argNames;
	}

	class FuncSetterProxy implements MethodInterceptor {
		@Override
		public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			if (method.getName().startsWith("set") && args.length == 1) {
				if (method.getName().length() > 3) {
					String fieldName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
					argNames.add(fieldName);
				}
			}
			return o;
		}
	}
}
