package top.onceio.core.db.dao.tpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OReflectUtil;

public class UpdateTpl<T> extends Tpl {
	protected static Pattern OPT = Pattern.compile("\\+|-|\\*|/|&|\\||~|#|<<|>>");
	private StringBuffer sql = new StringBuffer();
	private T tpl;
	private String strOpt;
	private Long id;
	private List<Object> args = new ArrayList<>();
	private Class<T> tplClass;

	@SuppressWarnings("unchecked")
	public UpdateTpl() {
		Type t = UpdateTpl.class.getTypeParameters()[0];
		tplClass = (Class<T>) OReflectUtil.searchGenType(UpdateTpl.class, this.getClass(), t);
		init(tplClass);
	}
	public UpdateTpl(Class<T> tplClass) {
		init(tplClass);
	}
	
	public UpdateTpl(Class<T> tplClass,String tpl) {
		init(tplClass);
		initTpl(tpl);
	}
	
	protected void initTpl(String tpl) {
		OAssert.err(tpl != null, "tpl cannot be null");
		String[] fileds = tpl.split(",");
		for(String field : fileds) {
			sql.append(field+",");
		}
	}
	
	@SuppressWarnings("unchecked")	
	protected void init(Class<T> tplClass) {
		this.tplClass = tplClass;
		UpdateSetterProxy cglibProxy = new UpdateSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (T) enhancer.create();
	}

	public Long getId() {
		return id;
	}

	public T set() {
		strOpt = "=";
		return tpl;
	}

	public T add() {
		strOpt = "+";
		return tpl;
	}

	public T sub() {
		strOpt = "-";
		return tpl;
	}

	public T mul() {
		strOpt = "*";
		return tpl;
	}

	public T div() {
		strOpt = "/";
		return tpl;
	}

	public T and() {
		strOpt = "&";
		return tpl;
	}

	public T or() {
		strOpt = "|";
		return tpl;
	}

	public T not() {
		strOpt = "~";
		return tpl;
	}

	public T xor() {
		strOpt = "#";
		return tpl;
	}

	public T left_shift() {
		strOpt = "<<";
		return tpl;
	}

	public T right_shift() {
		strOpt = ">>";
		return tpl;
	}

	public String getSetTpl() {
		if(sql.length() > 0 && sql.charAt(sql.length()-1) == ',') {
			return sql.substring(0, sql.length() - 1);
		}else {
			return sql.toString();
		}
	}

	public List<Object> getArgs() {
		return args;
	}

	class UpdateSetterProxy implements MethodInterceptor {
		@Override
		public Object intercept(Object o, Method method, Object[] argsx, MethodProxy methodProxy) throws Throwable {
			if (method.getName().startsWith("set") && argsx.length == 1) {
				if (method.getName().length() > 3) {
					String fieldName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
					Object arg = argsx[0];
					if (fieldName.equals("id")) {
						id = (Long) arg;
					} else if (fieldName.equals("rm")) {
					} else if (strOpt != null) {
						if (strOpt.equals("=")) {
							args.add(arg);
							sql.append(String.format("%s=(?),", fieldName));
						} else if (strOpt.equals("~")) {
							sql.append(String.format("%s=~(%s),", fieldName, fieldName));
						} else if (arg != null) {
							args.add(arg);
							sql.append(String.format("%s=%s%s(?),", fieldName, fieldName, strOpt));
						}
					}

				}
			}
			return o;
		}
	}
}
