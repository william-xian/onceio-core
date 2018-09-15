package top.onceio.core.db.dao.tpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.DDEngine;
import top.onceio.core.db.meta.TableMeta;
import top.onceio.core.util.OLog;
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
	public Cnd(Class<E> tplClass) {
		init(tplClass);
	}
	public Cnd(Class<E> tplClass,boolean ignoreRm) {
		init(tplClass);
		this.usingRm = !ignoreRm;
	}
	//TODO 条件表达式未设计c
	/**
	 * 
	 * @param tplClass
	 * @param cnd 其格式为cnd=cnd=c1&cnd=c2&page=&pagesize=&orderby=
	 */
	public Cnd(Class<E> tplClass,String cnd) {
		init(tplClass);
		initCnd(cnd);
	}
	@SuppressWarnings("unchecked")
	public void init(Class<E> tplClass) {
		this.tplClass = tplClass;
		CndSetterProxy cglibProxy = new CndSetterProxy();
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(tplClass);
		enhancer.setCallback(cglibProxy);
		tpl = (E) enhancer.create();
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

	public void initCnd(String cnd) {
		Map<String,List<String>> param = OUtils.parseURLParam(cnd);
		List<String> vals = param.get("page");
		if(vals != null && !vals.isEmpty()) {
			String sPage = 	vals.get(0);
			try {
				this.setPage(Integer.parseInt(sPage));
			}catch(Exception e) {
			}
		}
		vals = param.get("pagesize");
		if(vals != null && !vals.isEmpty()) {
			String sVal = 	vals.get(0);
			try {
				this.setPagesize(Integer.parseInt(sVal));
			}catch(Exception e) {
			}
		}
		TableMeta tm = TableMeta.tableCache.get(tplClass);
		if(tm != null) {
			vals = param.get("orderby");
			if(vals != null && !vals.isEmpty()) {
				for(String sOrder:vals) {
					String[] cos = sOrder.split(",");
					for(String co:cos) {
						String[] c_o = co.split(" ");
						if(c_o.length > 0) {
							ColumnMeta cm = tm.getColumnMetaByName(c_o[0]);
							if(cm != null) {
								if(c_o.length == 2 && (c_o[1].equalsIgnoreCase("ASC") || c_o[1].equalsIgnoreCase("DESC") || c_o[1].equals(""))) {
									this.order.order.add(c_o[0] + " " + c_o[1]);
								}else {
									this.order.order.add(c_o[0]);
								}
							}
						}
					}
				}
			}
			vals = param.get("cnd");
			resovle(tm,vals);
		}else {
			OLog.error("表%s不纯在", tplClass);
		}
	}
	
	private static Pattern LOGIC = Pattern.compile("_and_|_or_|_not_");
	private static Pattern CMP = Pattern.compile(">=|<=|!=|~\\*|>|<|=|_in_|_like_");
	private void resovle(TableMeta tm, List<String> vals) {
		Cnd<E> preCnd = null;
		boolean usingSelf = true;
		if(vals != null && !vals.isEmpty()) {
			for(String valArr:vals) {
				for(String val:valArr.split(";")) {
					Cnd<E> curCnd = null;
					if(usingSelf) {
						curCnd = this;
						usingSelf =false;
					}else {
						curCnd = new Cnd<>(tplClass);
					}

					Matcher lMatcher = LOGIC.matcher(val);
					int sp = 0;
					String lopt = null;
					while(lMatcher.find()) {
						String exp = val.substring(sp,lMatcher.start());
						sp = lMatcher.end();
						initCnd(curCnd,tm,exp,lopt);
						lopt = val.substring(lMatcher.start(),lMatcher.end());
					}
					String exp = val.substring(sp);
					if(!exp.isEmpty()) {
						initCnd(curCnd,tm,exp,lopt);	
					}
					
					if(preCnd != null) {
						preCnd.and(curCnd);
					}else {
						preCnd = curCnd;
					}
				}
			}
		}
	}
	//TODO
	private Cnd<E> initCnd(Cnd<E> cnd,TableMeta tm,String exp,String lopt) {
		Matcher cMatcher= CMP.matcher(exp);
		if(cMatcher.find()) {
			String fieldName= exp.substring(0,cMatcher.start());
			ColumnMeta cm = tm.getColumnMetaByName(fieldName);
			if(cm != null) {
				if(lopt != null) {
					cnd.selfSql.append(lopt.replace("_", " ").toUpperCase());
				}
				String cOpt = exp.substring(cMatcher.start(),cMatcher.end());
				String cVal = exp.substring(cMatcher.end());
				if(cVal!= null && !cVal.isEmpty()) {
					if(cm.getName().equals("rm")) {
						cnd.usingRm=true;
					}
					if(cOpt.equals("_in_")){
						String[] items = cVal.split(",");
						for(String item:items) {
							cnd.args.add(OReflectUtil.strToBaseType(cm.getJavaBaseType(), item));	
						}
						cnd.selfSql.append(cm.getName()+" in ("+OUtils.genStub("?", ",", items.length) + ")");
					}else {
						cnd.args.add(OReflectUtil.strToBaseType(cm.getJavaBaseType(), cVal));	
						cnd.selfSql.append(cm.getName()+" " + cOpt.replaceAll("_", "").toUpperCase() +" ?");
					}
				}else {
					cnd.args.add(null);
					cnd.selfSql.append(cm.getName()+" " + cOpt.replaceAll("_", "").toUpperCase() +" ?");
				}
			}
		}
		return cnd;
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
					if (opt.equals("IN")) {
						if(inVals != null && inVals.length > 0) {
							String stub = OUtils.genStub("?", ",", inVals.length);
							selfSql.append(String.format("%s%s %s (%s)", strLogic, fieldName, opt, stub));
							for (Object v : inVals) {
								args.add(v);
							}	
						} else {
							selfSql.append(String.format("%s1 = 0", strLogic ));
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
