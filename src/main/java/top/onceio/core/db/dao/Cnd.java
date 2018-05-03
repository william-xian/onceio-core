package top.onceio.core.db.dao;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.db.dao.tpl.GroupTpl;
import top.onceio.core.db.dao.tpl.HavingTpl;
import top.onceio.core.db.dao.tpl.OrderTpl;
import top.onceio.core.db.dao.tpl.SelectTpl;
import top.onceio.core.db.dao.tpl.Tpl;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.DDEngine;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

public class Cnd<E> extends Tpl {
	private static final Logger LOGGER = Logger.getLogger(Cnd.class);
	private Integer page;
	private Integer pagesize;
	private E pageArg;
	private Boolean isNext;
	private HavingTpl<E> having;
	private GroupTpl<E> group;
	private OrderTpl<E> order;
	private List<Object> args;
	private String opt = null;
	private Object[] inVals;
	private String logic = null;
	private List<String> extLogics;
	private List<Cnd<E>> extCnds;
	private StringBuffer selfSql;
	private Class<E> tplClass;
	private E tpl;
	private boolean usingRm = false;

	@SuppressWarnings("unchecked")
	public Cnd() {
		Type t = DaoHolder.class.getTypeParameters()[0];
		tplClass = (Class<E>) OReflectUtil.searchGenType(DaoHolder.class, this.getClass(), t);
		init();
	}
	@SuppressWarnings("unchecked")
	public Cnd(Class<E> tplClass) {
		this.tplClass = tplClass;
		CndSetterProxy cglibProxy = new CndSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (E) enhancer.create();
		init();
	}
	//TODO 条件表达式未设计
	@SuppressWarnings("unchecked")
	public Cnd(Class<E> tplClass,String cnd) {
		this.tplClass = tplClass;
		CndSetterProxy cglibProxy = new CndSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (E) enhancer.create();
		init();
	}
	public void init() {
		if(args == null) {
			args = new ArrayList<>();
		}
		if(selfSql == null) {
			selfSql = new StringBuffer();
		}
		if(extLogics == null) {
			extLogics = new ArrayList<>();
		}
		if(extCnds == null) {
			extCnds = new ArrayList<>();
		}
	}
	public Integer getPagesize() {
		return pagesize;
	}

	public void setPagesize(Integer pagesize) {
		this.pagesize = pagesize;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPage() {
		return page;
	}

	public E getPageArg() {
		return pageArg;
	}

	public void setPageArg(E pageArg) {
		this.pageArg = pageArg;
	}

	public Boolean getIsNext() {
		return isNext;
	}

	public void setIsNext(Boolean isNext) {
		this.isNext = isNext;
	}

	public E eq() {
		opt = "=";
		return tpl;
	}

	public E ne() {
		opt = "!=";
		return tpl;
	}

	public E lt() {
		opt = "<";
		return tpl;
	}

	public E le() {
		opt = "<=";
		return tpl;
	}

	public E gt() {
		opt = ">";
		return tpl;
	}

	public E ge() {
		opt = ">=";
		return tpl;
	}

	public E is_null() {
		opt = "IS NULL";
		return tpl;
	}

	public E not_null(E e) {
		opt = "IS NOT NULL";
		return tpl;
	}

	/**
	 * 对于需要查找null值字段，传递vals为null值，
	 */
	public E in(Object[] vals) {
		opt = "IN";
		inVals = vals;
		return tpl;
	}

	public E like() {
		opt = "LIKE";
		return tpl;
	}

	public E pattern() {
		opt = "~*";
		return tpl;
	}

	public Cnd<E> and() {
		if (opt != null) {
			logic = "AND";
		}
		return this;
	}

	public Cnd<E> or() {
		if (opt != null) {
			logic = "OR";
		}
		return this;
	}

	public Cnd<E> not() {
		if (opt != null) {
			logic = "NOT";
		}
		return this;
	}

	public Cnd<E> and(Cnd<E> extCnd) {
		extLogics.add("AND");
		extCnds.add(extCnd);
		return this;
	}

	public Cnd<E> or(Cnd<E> extCnd) {
		extLogics.add("OR");
		extCnds.add(extCnd);
		return this;
	}

	public Cnd<E> not(Cnd<E> extCnd) {
		extLogics.add("NOT");
		extCnds.add(extCnd);
		return this;
	}

	public String whereSql(List<Object> sqlArgs) {
		StringBuffer self = new StringBuffer();
		if (selfSql.length() > 0) {
			self.append("(" + selfSql);
			if(!usingRm) {
				self.append(" AND rm = false");
			}
			self.append(")");
		} else { 
			self.append("(rm = false)");
		}
		sqlArgs.addAll(args);
		for (int i = 0; i < this.extLogics.size(); i++) {
			String sl = this.extLogics.get(i);
			Cnd<E> c = extCnds.get(i);
			String other = c.whereSql(sqlArgs);
			if (!other.equals("")) {
				switch (sl) {
				case "AND":
					if (self.length() > 0) {
						self.append(" AND");
					}
					self.append(other);
					break;
				case "OR":
					if (self.length() > 0) {
						self.append(" OR");
					}
					self.append(other);
					break;
				case "NOT":
					if (self.length() > 0) {
						self.append(" AND NOT");
					} else {
						self.append(" NOT");
					}
					self.append(other);
					break;
				default:
				}
			} else {
				LOGGER.warn("查询条件是空的");
			}
		}
		return self.toString();
	}

	public String afterWhere(TableMeta tm, List<Object> sqlArgs) {
		StringBuffer afterWhere = new StringBuffer();
		Map<String, String> tokens = null;
		
		if (tm.getEngine() != null) {
			DDEngine dde = tm.getEngine();
			tokens = dde.getColumnToOrigin();
		}

		String whereCnd = whereSql(sqlArgs);
		if (!whereCnd.equals("")) {
			if (tokens == null) {
				afterWhere.append(String.format(" WHERE (%s)", whereCnd));
			} else {
				afterWhere.append(String.format(" WHERE (%s)", OUtils.replaceWord(whereCnd, tokens)));
			}
		}
		String group = group();
		if (group != null && !group.isEmpty()) {
			if (tokens == null) {
				afterWhere.append(String.format(" GROUP BY %s", group));
			} else {
				afterWhere.append(String.format(" GROUP BY %s", OUtils.replaceWord(group, tokens)));
			}
		}
		String having = getHaving(sqlArgs);
		if (having != null && !having.isEmpty()) {
			if (tokens == null) {
				afterWhere.append(String.format(" HAVING %s", having));
			} else {
				afterWhere.append(String.format(" HAVING %s", OUtils.replaceWord(having, tokens)));
			}
		}
		String order = getOrder();
		if (!order.isEmpty()) {
			if (tokens == null) {
				afterWhere.append(String.format(" ORDER BY %s", order));
			} else {
				afterWhere.append(String.format(" ORDER BY %s", OUtils.replaceWord(order, tokens)));
			}
		}
		return afterWhere.toString();
	}

	public StringBuffer selectSql(TableMeta tm, SelectTpl<E> tpl) {
		StringBuffer sqlSelect = new StringBuffer();
		sqlSelect.append("SELECT ");
		if (tm.getEngine() == null) {
			if (tpl != null && tpl.sql() != null && !tpl.sql().isEmpty()) {
				sqlSelect.append(tpl.sql());
			} else {
				for (ColumnMeta cm : tm.getColumnMetas()) {
					sqlSelect.append(cm.getName() + ",");
				}
				sqlSelect.delete(sqlSelect.length() - 1, sqlSelect.length());
			}
			sqlSelect.append(String.format(" FROM %s", tm.getTable()));
		} else {
			DDEngine dde = tm.getEngine();
			Set<String> params = new HashSet<>();
			Map<String, String> colToOrigin = dde.getColumnToOrigin();
			if (tpl != null) {
				params.addAll(tpl.getArgNames());
				sqlSelect.append(tpl.sql(colToOrigin));
			} else {
				for (ColumnMeta cm : tm.getColumnMetas()) {
					params.add(cm.getName());
					sqlSelect.append(String.format("%s %s,", colToOrigin.get(cm.getName()), cm.getName()));
				}
				sqlSelect.delete(sqlSelect.length() - 1, sqlSelect.length());
			}
			String mainPath = tm.getEntity().getSuperclass().getSimpleName().toLowerCase();
			String joinTables = dde.genericJoinSqlByParams(mainPath, params, null);
			sqlSelect.append(String.format(" FROM %s", joinTables));
			System.err.println(sqlSelect);
		}
		return sqlSelect;
	}

	public StringBuffer wholeSql(TableMeta tm, SelectTpl<E> tpl, List<Object> sqlArgs) {
		StringBuffer sql = new StringBuffer();
		sql.append(selectSql(tm, tpl));
		sql.append(afterWhere(tm, sqlArgs));
		return sql;
	}

	// TODO 根据上一条数据 和order语句计算相临的两页数据
	public String pageSql(TableMeta tm, SelectTpl<E> tpl, List<Object> sqlArgs) {
		StringBuffer s = wholeSql(tm, tpl, sqlArgs);
		s.append(" LIMIT ? OFFSET ?");
		sqlArgs.addAll(Arrays.asList(getPagesize(), (getPage() - 1) * getPagesize()));
		return s.toString();
	}

	public String countSql(TableMeta tm, SelectTpl<E> tpl, List<Object> sqlArgs) {
		String group = group();
		if (tm.getEngine() == null) {
			if (group != null && !group.isEmpty()) {
				return String.format("SELECT COUNT(1) FROM (SELECT 1 FROM %s %s) t", tm.getTable(),
						afterWhere(tm, sqlArgs));
			} else {
				return String.format("SELECT COUNT(1) FROM %s %s", tm.getTable(), afterWhere(tm, sqlArgs));
			}
		} else {
			StringBuffer select = selectSql(tm, tpl);
			int fromIndex = select.indexOf("FROM");
			return String.format("SELECT COUNT(1) FROM (SELECT 1 %s %s) t", select.substring(fromIndex),
					afterWhere(tm, sqlArgs));
		}
	}

	public HavingTpl<E> having() {
		if (having == null) {
			having = new HavingTpl<E>(tplClass);
		}
		return having;
	}

	public String getHaving(List<Object> sqlArgs) {
		if (having != null) {
			return having.sql(sqlArgs);
		} else {
			return null;
		}
	}

	public OrderTpl<E> orderBy() {
		if (order == null) {
			order = new OrderTpl<E>(tplClass);
		}
		return order;
	}

	public String getOrder() {
		if (order != null) {
			return order.getOrder();
		} else {
			return "";
		}
	}

	public GroupTpl<E> groupBy() {
		if (group == null) {
			group = new GroupTpl<E>(tplClass);
		}
		return group;
	}

	public String group() {
		if (group != null) {
			return group.getGroup();
		} else {
			return null;
		}
	}

	class CndSetterProxy implements MethodInterceptor {
		@Override
		public Object intercept(Object o, Method method, Object[] argsx, MethodProxy methodProxy) throws Throwable {
			if (method.getName().startsWith("set") && argsx.length == 1) {
				if (method.getName().length() > 3) {
					String fieldName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
					String strLogic = "";
					if(fieldName.equals("rm")) {
						if(!usingRm) {
							usingRm = true;		
						}else {
							return o;
						}
					}
					if (logic != null) {
						strLogic = logic.toString() + " ";
					}
					if (opt.equals("IN") && inVals != null && inVals.length > 1) {
						String stub = OUtils.genStub("?", ",", inVals.length);
						selfSql.append(String.format("%s%s %s (%s)", strLogic, fieldName, opt, stub));
						for (Object v : inVals) {
							args.add(v);
						}
					} else if (opt.equals("IS NULL")) {
						selfSql.append(String.format("%s%s IS NULL", strLogic, fieldName));
					} else if (opt.equals("IS NOT NULL")) {
						selfSql.append(String.format("%s%s IS NOT NULL", strLogic, fieldName));
					} else {
						selfSql.append(String.format("%s%s %s ?", strLogic, fieldName, opt));
						args.add(argsx[0]);

					}
				}
			}
			return o;
		}
	}

}
