package top.onceio.core.db.meta;

import top.onceio.core.db.model.AccessHelper;
import top.onceio.core.db.model.BaseMeta;

import java.util.*;

public class SqlPlanBuilder {
    public static int DROP_VIEW = 0;
    public static int DROP = 1;
    public static int CREATE_SCHEMA = 2;
    public static int CREATE_TABLE = 3;
    public static int ALTER = 4;
    public static int CREATE_VIEW = 5;
    public static int COMMENT = 6;

    Map<TableMeta, Collection<String>[]> plan = new HashMap<>();

    private Collection<String>[] createArray() {
        Collection<String>[] arr = new Collection[7];
        for (int i : Arrays.asList(DROP, CREATE_TABLE, ALTER, COMMENT)) {
            arr[i] = new ArrayList<>();
        }
        for (int i : Arrays.asList(DROP_VIEW, CREATE_SCHEMA, CREATE_VIEW)) {
            arr[i] = new HashSet<>();
        }
        return arr;
    }

    private Collection<String>[] getOrCreate(TableMeta meta) {
        Collection<String>[] arr = plan.get(meta);
        if (arr == null) {
            arr = createArray();
            plan.put(meta, arr);
        }
        return arr;
    }

    public SqlPlanBuilder append(int option, TableMeta meta, String sql) {
        Collection<String>[] arr = getOrCreate(meta);
        arr[option].add(sql);
        return this;
    }


    public SqlPlanBuilder append(int option, TableMeta meta, Collection<String> sql) {
        if (!sql.isEmpty()) {
            Collection<String>[] arr = getOrCreate(meta);
            arr[option].addAll(sql);
        }
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


    private void sortedAdd(Map<String, TableMeta> nameToMeta, List<TableMeta> order, BaseMeta def) {
        TableMeta meta = nameToMeta.get(AccessHelper.getName(def));
        if (order.contains(meta)) {
            return;
        }

        List<BaseMeta<?>> refs = AccessHelper.getRefs(def);
        for (BaseMeta ref : refs) {
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
            order.add(meta);
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

        Set<String> createSchema = new HashSet<>();
        Set<String> dropView = new HashSet<>();
        for (TableMeta meta : order) {
            Collection<String>[] v = plan.get(meta);
            if (v != null) {
                for (int i = 0; i < v.length; i++) {
                    if (i == CREATE_SCHEMA) {
                        createSchema.addAll(v[i]);
                    } else if (i == DROP_VIEW) {
                        dropView.addAll(v[i]);
                    } else {
                        sql.addAll(v[i]);
                    }
                }
            }
        }
        sql.addAll(0, dropView);
        sql.addAll(0, createSchema);
        return sql;
    }
}
