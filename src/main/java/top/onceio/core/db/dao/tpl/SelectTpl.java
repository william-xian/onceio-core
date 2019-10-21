package top.onceio.core.db.dao.tpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 遇到需要使用distinct时，请使用group by性能更好
 */
public class SelectTpl<E> extends FuncTpl<E> {
    public SelectTpl(Class<E> tplClass) {
        super(tplClass);
    }

    public SelectTpl(Class<E> tplClass, String tpl) {
        super(tplClass);
        if (funcs == null) {
            funcs = new ArrayList<>();
        }
        if (argNames == null) {
            argNames = new ArrayList<>();
        }
        if (tpl != null && !tpl.equals("")) {
            for (String t : tpl.split(",")) {
                funcs.add("");
                argNames.add(t);
            }
        }
    }

    public E using() {
        funcs.add("");
        return tpl;
    }

    List<String> columns() {
        List<String> cols = new ArrayList<>();
        for (int i = 0; i < funcs.size(); i++) {
            String func = funcs.get(i);
            String argName = argNames.get(i);
            if (func.equals("")) {
                cols.add(argName);
            } else {
                if (argName.startsWith("as ") || argName.startsWith(" ")) {
                    String col = argName.replaceAll(".* (.*)]", "$1");
                    cols.add("_" + col);
                } else {
                    cols.add(String.format("%s_%s", func, argName));
                }
            }
        }
        return cols;
    }

    String sql() {
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
            } else if (func.endsWith(")")) {
                sb.append(String.format("%s %s,", func, argName));
            } else {
                sb.append(String.format("%s(%s) %s_%s,", func, argName, func, argName));
            }
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    String sql(Map<String, String> colToOrigin) {
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
            } else if (func.endsWith(")")) {
                sb.append(String.format("%s %s,", func, argName));
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
