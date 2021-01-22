package top.onceio.core.db.model;

import top.onceio.core.db.annotation.IndexType;
import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.IndexMeta;
import top.onceio.core.db.meta.TableMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ModelEntityHelper {

    public static String toJavaClassName(String tableName, String prefix) {
        int index = tableName.indexOf('.');
        if (index >= 0) {
            tableName = tableName.substring(index + 1);
        }
        if (prefix != null && tableName.startsWith(prefix)) {
            tableName = tableName.substring(prefix.length());
        }
        StringBuilder className = new StringBuilder();
        boolean last = true;
        for (int i = 0; i < tableName.length(); i++) {
            char c = tableName.charAt(i);
            if (c == '_') {
                last = true;
            } else {
                if (last) {
                    className.append(Character.toUpperCase(c));
                } else {
                    className.append(c);
                }
                last = false;
            }
        }
        return className.toString();
    }

    public static String toJavaFieldName(String columnName) {
        StringBuilder fieldName = new StringBuilder();
        boolean last = false;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                last = true;
            } else {
                if (last) {
                    fieldName.append(Character.toUpperCase(c));
                } else {
                    fieldName.append(c);
                }
                last = false;
            }
        }
        return fieldName.toString();
    }

    public static String genericJavaFileContent(String packageName, String prefix, TableMeta tm) {
        String classFormat = "package %s;\n" +
                "\n" +
                "import top.onceio.core.db.annotation.Col;\n" +
                "import top.onceio.core.db.annotation.Model;\n" +
                "import top.onceio.core.db.model.BaseModel;\n" +
                "%s\n\n" +
                "@Model%s\n" +
                "public class %s extends BaseModel<%s> {\n";
        String fieldFormat = "\n    @Col%s\n" +
                "    protected %s %s;\n";
        String idType = "Long";
        Set<String> imports = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (ColumnMeta cm : tm.getColumnMetas()) {
            final StringBuilder col = new StringBuilder();
            final String javaType;
            String name = toJavaFieldName(cm.getName());
            if (cm.getType().equals("bool")) {
                javaType = "Boolean";
            } else if (cm.getType().equals("int2")) {
                javaType = "Short";
            } else if (cm.getType().equals("int4")) {
                javaType = "Integer";
            } else if (cm.getType().equals("int8")) {
                javaType = "Long";
            } else if (cm.getType().startsWith("varchar")) {
                javaType = "String";
                col.append(String.format("size = %s, ", cm.getType().replaceAll("varchar\\(([0-9]+)\\)", "$1")));
            } else if (cm.getType().startsWith("char")) {
                javaType = "String";
                col.append(String.format("size = %s, ", cm.getType().replaceAll("char\\(([0-9]+)\\)", "$1")));
                col.append("type = \"char\", ");
            } else if (cm.getType().startsWith("timestamp")) {
                javaType = "Timestamp";
                imports.add("import java.sql.Timestamp;");
            } else {
                javaType = "String";
                col.append(String.format("type = \"%s\", ", cm.getType()));
            }
            if (cm.isPrimaryKey()) {
                idType = javaType;
                continue;
            }
            if (!cm.isPrimaryKey() && cm.isUnique()) {
                col.append(String.format("unique = %s, ", cm.isUnique()));
            }
            if (cm.isNullable()) {
                col.append(String.format("nullable = %s, ", cm.isNullable()));
            }
            if (!cm.getUsing().equals("") && !cm.getUsing().equalsIgnoreCase("btree")) {
                col.append(String.format("using = \"%s\", ", cm.getUsing()));
            }
            if (!cm.getDefaultValue().equals("")) {
                if (cm.getDefaultValue().equalsIgnoreCase("0") && cm.getType().startsWith("int")) {

                } else if (cm.getDefaultValue().equalsIgnoreCase("false") && cm.getType().equalsIgnoreCase("bool")) {

                } else {
                    col.append(String.format("defaultValue = \"%s\", ", cm.getDefaultValue()));
                }
            }
            if (!cm.getComment().equals("")) {
                col.append(String.format("comment = \"%s\", ", cm.getComment()));
            }
            if (col.length() > 0) {
                col.delete(col.length() - 2, col.length());
                col.insert(0, '(');
                col.append(')');
            }
            sb.append(String.format(fieldFormat, col.toString(), javaType, name));
        }


        StringBuilder indexes = new StringBuilder();
        for (IndexMeta index : tm.getIndexes()) {
            StringBuilder indexBuf = new StringBuilder();
            List<String> columns = new ArrayList<>();
            for (String col : index.getColumns()) {
                columns.add(toJavaFieldName(col));
            }
            if (!index.getUsing().equalsIgnoreCase("btree")) {
                indexBuf.append(String.format("using = \"%s\", ", index.getUsing()));
            }
            if (index.getType().equals(IndexType.UNIQUE_INDEX)) {
                indexBuf.append("unique = true, ");
            }
            if (!columns.isEmpty()) {
                indexBuf.append(String.format("columns = {\"%s\"}, ", String.join("\", \"", columns)));
            }
            if (indexBuf.length() > 0) {
                indexBuf.delete(indexBuf.length() - 2, indexBuf.length());
            }
            indexes.append(String.format("@Index(%s), ", indexBuf.toString()));
        }
        if (indexes.length() > 0) {
            indexes.delete(indexes.length() - 2, indexes.length());
            indexes.insert(0, "indexes = {");
            indexes.append("}");
        }
        final String model;
        if (tm.getTable().contains(".") || (prefix != null && !prefix.equalsIgnoreCase(""))) {
            if (indexes.length() == 0) {
                model = String.format("(name = \"%s\")", tm.getTable());
            } else {
                model = String.format("(name = \"%s\", %s)", tm.getTable(), indexes);
                imports.add("import top.onceio.core.db.annotation.Index;");
            }
        } else {
            if (indexes.length() > 0) {
                model = String.format("(%s)", indexes);
                imports.add("import top.onceio.core.db.annotation.Index;");
            } else {
                model = "";
            }
        }
        String className = toJavaClassName(tm.getTable(), prefix);
        sb.insert(0, String.format(classFormat, packageName, String.join("\n", imports), model, className, idType));
        sb.append("}");

        return sb.toString();
    }

    public static Map<String, TableMeta> findTableMeta(DaoHelper daoHelper, Collection<String> schemaTables) {
        return daoHelper.findTableMeta(schemaTables);
    }

    public static void genericJavaFile(DaoHelper daoHelper, String dir, String packageName, String prefix, Collection<String> schemaTables) {
        Map<String, TableMeta> nameToTableMeta = daoHelper.findTableMeta(schemaTables);
        for (String tableName : schemaTables) {
            TableMeta tm = nameToTableMeta.get(tableName);
            if (tm == null) continue;
            FileOutputStream fos = null;
            try {
                String className = toJavaClassName(tm.getTable(), prefix);
                File file = new File(dir + "/" + packageName.replace(".", "/") + "/" + className + ".java");
                File p = file.getParentFile();
                if (!p.exists()) {
                    p.mkdirs();
                }
                fos = new FileOutputStream(file);
                String content = genericJavaFileContent(packageName, prefix, tm);
                fos.write(content.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
