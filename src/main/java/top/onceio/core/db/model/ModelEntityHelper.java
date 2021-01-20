package top.onceio.core.db.model;

import top.onceio.core.db.meta.ColumnMeta;
import top.onceio.core.db.meta.TableMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class ModelEntityHelper {

    public static String toJavaClassName(String simpleName) {
        int index = simpleName.indexOf('.');
        if (index >= 0) {
            simpleName = simpleName.substring(index);
        }
        StringBuilder className = new StringBuilder();
        boolean last = true;
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
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

    public static String toJavaFieldName(String simpleName) {
        int index = simpleName.indexOf('.');
        if (index >= 0) {
            simpleName = simpleName.substring(index);
        }
        StringBuilder className = new StringBuilder();
        boolean last = false;
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
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

    public static String genericJavaFileContent(String packageName, TableMeta tm) {
        String classFormat = "package %s;\n" +
                "\n" +
                "import top.onceio.core.db.annotation.Col;\n" +
                "import top.onceio.core.db.annotation.Model;\n" +
                "import top.onceio.core.db.model.BaseModel;\n" +
                "\n" +
                "@Model%s\n" +
                "public class %s extends BaseModel<%s> {\n";
        String fieldFormat = "\n    @Col%s\n" +
                "    protected %s %s;\n";
        String idType = "Long";
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
            } else {
                javaType = "String";
                col.append(String.format("type = \"%s\", ", cm.getType()));
            }
            if (cm.isPrimaryKey()) {
                idType = javaType;
            }
            if (!cm.isPrimaryKey() && cm.isUnique()) {
                col.append(String.format("unique = %s, ", cm.isUnique()));
            }
            if (cm.isNullable()) {
                col.append(String.format("nullable = %s, ", cm.isNullable()));
            }
            if (!cm.getUsing().equals("") && !cm.getUsing().equals("BTREE")) {
                col.append(String.format("using = \"%s\", ", cm.getUsing()));
            }
            if (!cm.getComment().equals("")) {
                col.append(String.format("comment = \"%s\", ", cm.getComment()));
            }
            if (!cm.getDefaultValue().equals("")) {
                col.append(String.format("defaultValue = \"%s\", ", cm.getDefaultValue()));
            }
            if (col.length() > 0) {
                col.delete(col.length() - 2, col.length());
                col.insert(0, '(');
                col.append(')');
            }
            sb.append(String.format(fieldFormat, col.toString(), javaType, name));
        }

        String model = "";
        if (tm.getTable().contains(".")) {
            model = tm.getTable();
        }
        String className = toJavaClassName(tm.getTable());
        sb.insert(0, String.format(classFormat, packageName, model, className, idType));
        sb.append("}");

        return sb.toString();
    }

    public static Map<String, TableMeta> findTableMeta(DaoHelper daoHelper, Collection<String> schemaTables) {
        return daoHelper.findTableMeta(schemaTables);
    }

    public static void genericJavaFile(DaoHelper daoHelper, String dir, String packageName, Collection<String> schemaTables) {
        Map<String, TableMeta> nameToTableMeta = daoHelper.findTableMeta(schemaTables);
        for (String tableName : schemaTables) {
            TableMeta tm = nameToTableMeta.get(tableName);
            if (tm == null) continue;
            FileOutputStream fos = null;
            try {
                File file = new File(dir + "/" + packageName.replace(".", "/") + "/" + toJavaClassName(tm.getTable()) + ".java");
                File p = file.getParentFile();
                if (!p.exists()) {
                    p.mkdirs();
                }
                fos = new FileOutputStream(file);
                String content = genericJavaFileContent(packageName, tm);
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
