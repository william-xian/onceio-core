package top.onceio.core.db.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.onceio.core.util.OAssert;
import top.onceio.core.util.Tuple3;

/**
 * 数据推倒引擎
 *
 * @author Administrator
 */
public class DDEngine {

    /**
     * 命名路径，实表
     */
    public Map<String, DDMeta> pathToMeta = new HashMap<>();

    /**
     * 虚表名，实表
     */
    public Map<String, DDMeta> aliasToMeta = new HashMap<>();
    /**
     * 虚表字段，实表
     */
    public Map<String, DDMeta> columnToMeta = new HashMap<>();
    /**
     * <表，<关联表，关联关系>>
     */
    public Map<String, Map<String, String>> relMap = new HashMap<>();

    /**
     * 添加推导关系和结果集 <b>此处的关系是一一对应的</b>
     *
     * @param resultSet A {id aid,name AName,bid}; A.bid-B {name BName,CId};
     *                  A.bid-B.cid-C {name CName}
     * @return
     */
    public DDEngine append(String resultSet) {
        String[] sets = resultSet.split(";");
        for (String set : sets) {
            String[] tbl_columns = set.split("\\{|\\}");
            if (tbl_columns.length >= 2) {
                String path = tbl_columns[0].trim();
                String[] columns = tbl_columns[1].split(",");
                DDMeta meta = new DDMeta();
                meta.setPath(path);
                meta.setColumnMapping(Arrays.asList(columns));
                pathToMeta.put(path, meta);
            } else {
                OAssert.warnning("%s -> %s 不合法", resultSet, set);
            }
        }
        return this;
    }

    private String tranAlias(int id) {
        return "T" + id;
    }

    public void build() {
        supplyDDMeta();

        List<String> paths = new ArrayList<>(pathToMeta.keySet());
        Collections.sort(paths);
        int id = 0;
        for (String path : paths) {
            DDMeta meta = pathToMeta.get(path);
            meta.setName(tranAlias(id));
            id++;
        }
        for (String path : paths) {
            DDMeta meta = pathToMeta.get(path);
            resoveDDMeta(meta);
        }

        /** 生成对应关系 */
        for (DDMeta meta : pathToMeta.values()) {
            aliasToMeta.put(meta.getName(), meta);
            for (String col : meta.getColumns()) {
                columnToMeta.put(col, meta);
            }
        }
    }

    /**
     * 补充中间过程表
     */
    private void supplyDDMeta() {
        List<String> keys = new ArrayList<>(pathToMeta.keySet());
        Collections.sort(keys);
        for (String path : keys) {
            String[] pathArr = path.split("-");
            String lastTbl = null;
            for (int i = 0; i < pathArr.length; i++) {
                String curTbl = pathArr[i];
                String tbl = curTbl.replaceAll("\\..*", "");
                if (tbl.equals(lastTbl)) {
                    List<String> plist = new ArrayList<>();
                    for (int t = 0; t < i; t++) {
                        plist.add(pathArr[t]);
                    }
                    plist.add(tbl);
                    String pkName = pathArr[i - 1].replaceAll(".*\\.", "");
                    String npath = String.join("-", plist);
                    if (!pathToMeta.containsKey(npath)) {
                        DDMeta nmeta = new DDMeta();
                        nmeta.setPath(npath);
                        nmeta.setPkName(pkName);
                        pathToMeta.put(npath, nmeta);
                    }
                }
                lastTbl = tbl;
            }
        }
    }

    private Tuple3<String, String, String> splitPath(String path) {
        Tuple3<String, String, String> result = new Tuple3<String, String, String>();
        int sep = path.lastIndexOf('-');
        if (sep < 0) {
            result.a = path;
        } else {
            String rel = path.substring(0, sep);
            int rSep = rel.lastIndexOf('.');
            result.a = path.substring(0, rSep);
            result.b = path.substring(sep + 1);
            result.c = path.substring(rSep + 1, sep);
        }
        return result;
    }

    private void resoveDDMeta(DDMeta meta) {
        Tuple3<String, String, String> tbls = splitPath(meta.getPath());
        if (tbls.a != null && tbls.b == null) {
            meta.setTable(tbls.a);
            return;
        } else {
            meta.setTable(tbls.b.replaceAll("\\..*", ""));
        }
        DDMeta metaA = pathToMeta.get(tbls.a);
        if (meta.getPkName() != null) {
            /** 是中间表 */
            tbls = splitPath(tbls.a);
            metaA = pathToMeta.get(tbls.a);
        }
        if (metaA != null) {
            String rel = String.format("%s.%s = %s.%s", metaA.getName(), tbls.c, meta.getName(),
                    meta.getPkName() == null ? "id" : meta.getPkName());
            saveRelation(rel, meta, metaA);
        } else {
            OAssert.fatal("不可能,%s 对到错误", meta.getPath());
        }
    }

    private void saveRelation(String relation, DDMeta a, DDMeta b) {
        Map<String, String> mappingA = relMap.get(a.getName());
        if (mappingA == null) {
            mappingA = new HashMap<>();
            relMap.put(a.getName(), mappingA);
        }
        mappingA.put(b.getName(), relation);
        Map<String, String> mappingB = relMap.get(b.getName());
        if (mappingB == null) {
            mappingB = new HashMap<>();
            relMap.put(b.getName(), mappingB);
        }
        mappingB.put(a.getName(), relation);
    }

    /**
     * 寻找足迹
     */
    public boolean search(Set<DDMeta> spoor, List<DDMeta> path, DDMeta src, DDMeta dest) {
        if (spoor.contains(src)) {
            return false;
        }
        Map<String, String> map = relMap.get(src.getName());
        if (map == null) {
            return false;
        }
        path.add(src);
        spoor.add(src);
        if (map.containsKey(dest.getName()) || src.getName().equals(dest.getName())) {
            path.add(dest);
            return true;
        } else {
            for (String subSrc : map.keySet()) {
                DDMeta subMeta = aliasToMeta.get(subSrc);
                if (search(spoor, path, subMeta, dest)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据主表和相关参数 推导出依赖的相关表
     *
     * @param mainPath
     * @param params
     * @return
     */
    public String genericJoinSqlByParams(String mainPath, Set<String> select, Set<String> params) {
        DDMeta mainMeta = pathToMeta.get(mainPath);
        if (mainMeta == null) {
            OAssert.warnning("%s 不存在", mainPath);
        }
        Set<DDMeta> set = new HashSet<>();
        if (params != null && !params.isEmpty()) {
            for (String column : params) {
                DDMeta meta = columnToMeta.get(column);
                if (meta != null) {
                    set.add(meta);
                } else {
                    OAssert.warnning("列 %s 不存在", column);
                }
            }
        }
        if (select != null && !select.isEmpty()) {
            for (String column : select) {
                DDMeta meta = columnToMeta.get(column);
                if (meta != null) {
                    set.add(meta);
                } else {
                    OAssert.warnning("列 %s 不存在", column);
                }
            }
        }
        Set<DDMeta> depends = new HashSet<>();
        List<String> dependNamePaths = new ArrayList<>();
        for (DDMeta meta : set) {
            Set<DDMeta> spoor = new HashSet<>();
            List<DDMeta> path = new ArrayList<>();
            if (search(spoor, path, meta, mainMeta)) {
                depends.addAll(path);
                StringBuffer namepath = new StringBuffer();
                for (int i = path.size() - 1; i >= 0; i--) {
                    namepath.append(path.get(i).getName() + "-");
                }
                dependNamePaths.add(namepath.toString());
            } else {
                OAssert.warnning("关系一定有错误，无法推导 %s -> %s", meta, mainPath);
            }
        }
        Collections.sort(dependNamePaths);
        depends.add(mainMeta);
        StringBuffer sql = new StringBuffer("");
        sql.append(String.format("%s %s", mainMeta.getTable(), mainMeta.getName()));
        if (dependNamePaths != null && !dependNamePaths.isEmpty()) {
            Set<String> spoor = new HashSet<>();
            spoor.add(mainMeta.getName());
            for (String dnp : dependNamePaths) {
                String[] deps = dnp.split("-");
                String depend = deps[0];
                for (int i = 1; i < deps.length; i++) {
                    DDMeta meta = aliasToMeta.get(deps[i]);
                    /** 如果存在中间表去除重复 */
                    if (spoor.contains(meta.getName()))
                        continue;
                    spoor.add(meta.getName());
                    DDMeta dependMeta = aliasToMeta.get(depend);
                    String rel = relMap.get(meta.getName()).get(dependMeta.getName());
                    sql.append(String.format("\nLEFT JOIN %s %s on %s", meta.getTable(), meta.getName(), rel));
                    depend = deps[i];
                }
            }
        }
        return sql.toString();
    }

    public Map<String, String> getColumnToOrigin() {
        Map<String, String> tokens = new HashMap<>();
        for (String col : columnToMeta.keySet()) {
            DDMeta meta = columnToMeta.get(col);
            tokens.put(col, meta.getName() + "." + meta.getColumnToOrigin().get(col));
        }
        return tokens;
    }

}
