package top.onceio.core.db.meta;

import java.lang.reflect.Field;
import java.util.Objects;

public class ColumnMeta {
    String name;
    String type;
    boolean nullable;
    boolean unique;
    String refTable;
    boolean useFK;
    String using;
    boolean primaryKey;
    /**
     * 正则表达式 或@JSON,@Email等特定教研
     */
    String pattern;
    String defaultValue;
    String comment;

    transient Field field;
    transient Class<?> javaBaseType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getRefTable() {
        return refTable;
    }

    public void setRefTable(String refTable) {
        this.refTable = refTable;
    }

    public boolean isUseFK() {
        return useFK;
    }

    public void setUseFK(boolean useFK) {
        this.useFK = useFK;
    }

    public String getUsing() {
        return using;
    }

    public void setUsing(String using) {
        this.using = using;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Class<?> getJavaBaseType() {
        return javaBaseType;
    }

    public void setJavaBaseType(Class<?> javaBaseType) {
        this.javaBaseType = javaBaseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnMeta that = (ColumnMeta) o;
        return nullable == that.nullable &&
                unique == that.unique &&
                useFK == that.useFK &&
                primaryKey == that.primaryKey &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(refTable, that.refTable) &&
                Objects.equals(using, that.using) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, unique, refTable, useFK, using, primaryKey, defaultValue, comment);
    }
}