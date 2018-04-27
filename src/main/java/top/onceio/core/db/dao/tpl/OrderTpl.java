package top.onceio.core.db.dao.tpl;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class OrderTpl<E> extends Tpl {
	public static final Object ORDER_BY_ASC = null;
	public static final Object ORDER_BY_DESC = new Object();
	public static final byte ORDER_BY_ASC_B = 'A';
	public static final byte ORDER_BY_DESC_B = 'D';
	public static final char ORDER_BY_ASC_C = 'A';
	public static final char ORDER_BY_DESC_C = 'D';
	public static final int ORDER_BY_ASC_INT = 0;
	public static final int ORDER_BY_DESC_INT = 1;
	public static final long ORDER_BY_ASC_LONG = 0L;
	public static final long ORDER_BY_DESC_LONG = 1L;
	public static final String ORDER_BY_ASC_S = "A";
	public static final String ORDER_BY_DESC_S = "D";
	public static final double ORDER_BY_ASC_DOUBLE = 0;
	public static final double ORDER_BY_DESC_DOUBLE = 1d;
	public static final float ORDER_BY_ASC_FLOAT = 0;
	public static final float ORDER_BY_DESC_FLOAT = 1f;
	public static final BigDecimal ORDER_BY_ASC_DECIMAL = new BigDecimal(0);
	public static final BigDecimal ORDER_BY_DESC_DECIMAL = new BigDecimal(1);
	private List<String> order = new ArrayList<>();
	private String orderMethod = "ASC";
	private E tpl;

	@SuppressWarnings("unchecked")
	public OrderTpl(Class<E> tplClass) {
		OrderSetterProxy cglibProxy = new OrderSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (E) enhancer.create();
	}

	public E desc() {
		orderMethod = "DESC";
		return tpl;
	}

	public E asc() {
		orderMethod = "ASC";
		return tpl;
	}

	public String getOrder() {
		return String.join(",", order);
	}

	class OrderSetterProxy implements MethodInterceptor {
		@Override
		public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			if (method.getName().startsWith("set") && args.length == 1) {
				if (method.getName().length() > 3) {
					String fieldName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
					order.add(fieldName + " " + orderMethod);
				}
			}
			return o;
		}
	}
}