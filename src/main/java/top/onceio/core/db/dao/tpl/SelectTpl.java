package top.onceio.core.db.dao.tpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 遇到需要使用distinct时，请使用group by性能更好
 */
public class SelectTpl<E> extends FuncTpl<E> {
	public SelectTpl() {
		super();
	}
	public SelectTpl(Class<E> tplClass) {
		super(tplClass);
	}
	//TODO
	public SelectTpl(Class<E> tplClass,String tpl) {
		super(tplClass);
	}

	public E using() {
		funcs.add("");
		return tpl;
	}

	public E usingRowNum() {
		funcs.add("ROW_NUMBER() OVER()");
		argNames.add("id");
		return tpl;
	}

	public List<String> columns() {
		List<String> cols = new ArrayList<>();
		for (int i = 0; i < funcs.size(); i++) {
			String func = funcs.get(i);
			String argName = argNames.get(i);
			if (func.equals("")) {
				cols.add(argName);
			} else if (func.equals("ROW_NUMBER() OVER()")) {
				cols.add("rowNum");
			} else {
				cols.add(String.format("%s_%s", func, argName));
			}
		}
		return cols;
	}

	public String sql() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < funcs.size(); i++) {
			String func = funcs.get(i);
			String argName = argNames.get(i);
			if (func.equals("")) {
				sb.append(argName + ",");
			} else if (func.equals("MAX")) {
				sb.append(String.format("%s(%s) %s,", func, argName, argName));
			} else if (func.equals("MIN")) {
				sb.append(String.format("%s(%s) %s,", func, argName, argName));
			} else if (func.equals("ROW_NUMBER() OVER()")) {
				sb.append(String.format("%s rowNum,", func));
			} else {
				sb.append(String.format("%s(%s) %s_%s,", func, argName, func, argName));
			}
		}
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		return sb.toString();
	}

	public String sql(Map<String, String> colToOrigin) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < funcs.size(); i++) {
			String func = funcs.get(i);
			String argName = argNames.get(i);
			if (func.equals("")) {
				sb.append(String.format("%s %s,", colToOrigin.get(argName), argName));
			} else if (func.equals("MAX")) {
				sb.append(String.format("%s(%s) %s,", func, colToOrigin.get(argName), argName));
			} else if (func.equals("MIN")) {
				sb.append(String.format("%s(%s) %s,", func, colToOrigin.get(argName), argName));
			} else if (func.equals("ROW_NUMBER() OVER()")) {
				sb.append(String.format("%s rowNum,", func));
			} else {
				sb.append(String.format("%s(%s) %s_%s,", func, colToOrigin.get(argName), func, argName));
			}
		}
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		return sb.toString();
	}
}
