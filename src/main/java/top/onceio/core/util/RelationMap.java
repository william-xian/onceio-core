package top.onceio.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 关系图，动态建立单一映射关系群
 *
 * @author xian
 */
public class RelationMap {
    private Map<String, PairMap<Object, Object>> nameToRelationMap = new HashMap<>();

    public void make(String relation, Object a, Object b) {
        PairMap<Object, Object> pm = nameToRelationMap.get(relation);
        if (pm == null) {
            pm = new PairMap<>();
            nameToRelationMap.put(relation, pm);
        }
        pm.put(a, b);
    }
}
