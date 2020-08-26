package top.onceio.core.db.meta;

import top.onceio.core.db.model.AccessHelper;
import top.onceio.core.db.model.BaseTable;

import java.util.*;

public class SqlPlanBuilder {
    public static int DROP = 0;
    public static int CREATE_SCHEMA = 1;
    public static int CREATE = 2;
    public static int ALTER = 3;
    public static int COMMENT = 4;

    Map<TableMeta, List<String>[]> plan = new HashMap<>();

    private List<String>[] createArray() {
        List<String>[] arr = new List[5];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new ArrayList<>();
        }
        return arr;
    }

    public SqlPlanBuilder append(int option, TableMeta meta, String sql) {
        List<String>[] arr = plan.get(meta);
        if (arr == null) {
            arr = createArray();
            plan.put(meta, arr);
        }
        arr[option].add(sql);
        return this;
    }

    public SqlPlanBuilder append(int option, TableMeta meta, List<String> sql) {
        List<String>[] arr = plan.get(meta);
        if (arr == null) {
            arr = createArray();
            plan.put(meta, arr);
        }
        arr[option].addAll(sql);
        return this;
    }

    public SqlPlanBuilder append(SqlPlanBuilder otherPlan) {
        otherPlan.plan.forEach((k, v) -> {
            for (int i = 0; i < v.length; i++) {
                append(i, k, v[i]);
            }
        });
        return this;
    }


    private void sortedAdd(Map<String, TableMeta> nameToMeta, List<TableMeta> order, BaseTable def) {
        TableMeta meta = nameToMeta.get(AccessHelper.getName(def));
        if (order.contains(meta)) {
            return;
        }

        List<BaseTable<?>> refs = AccessHelper.getRefs(def);
        for (BaseTable ref : refs) {
            sortedAdd(nameToMeta, order, ref);
        }
        order.add(meta);
    }

    private void sortedAdd(Map<String, TableMeta> nameToMeta, List<TableMeta> order, TableMeta meta) {
        if (order.contains(meta)) {
            return;
        }
        if (meta.getViewDef() != null) {
            sortedAdd(nameToMeta, order, meta.getViewDef());
        } else {
            List<IndexMeta> all = new ArrayList<>();
            all.addAll(meta.getFieldConstraint());
            all.addAll(meta.getIndexes());

            for (IndexMeta indexMeta : all) {
                if (indexMeta.refTable != null) {
                    sortedAdd(nameToMeta, order, nameToMeta.get(indexMeta.refTable));
                }
            }
            order.add(meta);
        }
    }

    public List<String> build(Map<String, TableMeta> nameToMeta) {
        List<String> sql = new ArrayList<>();

        List<TableMeta> order = new ArrayList<>();
        for (TableMeta meta : plan.keySet()) {
            sortedAdd(nameToMeta, order, meta);
        }

        for (TableMeta meta : order) {
            List<String>[] v = plan.get(meta);
            if (v != null) {
                for (int i = 0; i < v.length; i++) {
                    sql.addAll(v[i]);
                }
            }
        }
        return sql;
    }
}
