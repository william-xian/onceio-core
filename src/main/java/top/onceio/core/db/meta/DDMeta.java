package top.onceio.core.db.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import top.onceio.core.util.OAssert;
import top.onceio.core.util.OUtils;

/**
 * 推倒元数据
 *
 * @author Administrator
 */
public class DDMeta {
    String path;
    String name;
    String table;
    String pkName;
    Map<String, String> columnToOrigin = new HashMap<>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        int sp = path.lastIndexOf('-');
        if (sp > 0) {
            this.table = path.substring(sp);
        } else {
            this.table = path;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getPkName() {
        return pkName;
    }

    public void setPkName(String pkName) {
        this.pkName = pkName;
    }

    public Set<String> getColumns() {
        return columnToOrigin.keySet();
    }

    public void setColumnMapping(Collection<String> columnAlias) {
        for (String column : columnAlias) {
            String[] rel_col = column.trim().split(" +");
            if (rel_col.length == 2) {
                columnToOrigin.put(rel_col[1], rel_col[0]);
            } else if (rel_col.length == 1) {
                columnToOrigin.put(rel_col[0], rel_col[0]);
            } else {
                OAssert.warnning("%s 不符合规范", column);
            }
        }
    }

    public Map<String, String> getColumnToOrigin() {
        return columnToOrigin;
    }

    public void setColumnToOrigin(Map<String, String> columnToOrigin) {
        this.columnToOrigin = columnToOrigin;
    }

    @Override
    public String toString() {
        return OUtils.toJson(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DDMeta other = (DDMeta) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

}
