package top.onceio.core.processor;


import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.model.BaseCol;
import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.db.model.StringCol;
import top.onceio.core.util.OReflectUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({"top.onceio.core.db.annotation.Model"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ModelProcessor extends AbstractProcessor {
    private static final String META_CLASS_NAME = "Meta";
    private static final String META_METHOD_NAME = "meta";
    private static final String CLASS_INIT_METHOD_NAME = "<init>";
    private static final String META_BIND_METHOD_NAME = "super.bind";

    private Messager messager;
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;
    private ClassReader classReader;
    private Elements elementsUtils;

    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        messager = processingEnvironment.getMessager();
        super.init(processingEnvironment);
        elementsUtils = processingEnvironment.getElementUtils();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        classReader = ClassReader.instance(context);
        classReader.loadClass(names.fromString(BaseMeta.class.getName()));
        classReader.loadClass(names.fromString(BaseCol.class.getName()));
        classReader.loadClass(names.fromString(StringCol.class.getName()));
        classReader.loadClass(names.fromString(OReflectUtil.class.getName()));
    }

    class EntityTranslator extends TreeTranslator {

        private boolean hasMetaClass = false;
        private JCTree.JCAnnotation tbl;
        private JCTree.JCClassDecl jcClass;
        private Set<String> methodNames = new HashSet<>();
        private java.util.List<JCTree.JCVariableDecl> variables = new ArrayList<>();

        @Override
        public void visitClassDef(JCTree.JCClassDecl jcClass) {
            super.visitClassDef(jcClass);
            this.jcClass = jcClass;

            if (jcClass.name.toString().equals(META_CLASS_NAME)) {
                hasMetaClass = true;
            }

        }

        @Override
        public void visitAnnotation(JCTree.JCAnnotation var1) {
            var1.annotationType = this.translate(var1.annotationType);
            var1.args = this.translate(var1.args);
            if (treeMaker.Type(var1.type).toString().equals(Model.class.getName())) {
                this.tbl = var1;
            }
            this.result = var1;
        }

        @Override
        public void visitMethodDef(JCTree.JCMethodDecl var1) {
            super.visitMethodDef(var1);
            methodNames.add(var1.name.toString());
        }

        @Override
        public void visitVarDef(JCTree.JCVariableDecl var1) {
            super.visitVarDef(var1);
            variables.add(var1);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotation = roundEnv.getElementsAnnotatedWith(Model.class);

        java.util.List<JCTree> entities = annotation.stream().map(element -> trees.getTree(element)).collect(Collectors.toList());

        for (JCTree tree : entities) {
            try {
                EntityTranslator entityTranslator = new EntityTranslator();
                tree.accept(entityTranslator);
                if (entityTranslator.hasMetaClass) continue;
                JCTree.JCClassDecl jcClass = entityTranslator.jcClass;
                Set<String> methodNames = entityTranslator.methodNames;

                java.util.List<? extends Element> fields = elementsUtils.getAllMembers(jcClass.sym).stream().filter(m -> m.getKind().isField() && m.getAnnotation(Col.class) != null)
                        .collect(Collectors.toList());

                Map<String, TypeMirror> fieldToType = new HashMap<>();
                for (Element e : fields) {
                    String fieldName = e.getSimpleName().toString();
                    fieldToType.put(fieldName, e.asType());
                }

                for (JCTree.JCVariableDecl var : entityTranslator.variables) {
                    String fieldName = var.name.toString();
                    if (fieldName.length() > 1) {
                        fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    } else {
                        fieldName = fieldName.toUpperCase();
                    }
                    if (!methodNames.contains("get" + fieldName) && !methodNames.contains("is" + fieldName)) {
                        JCTree.JCMethodDecl get = generateGetterMethod(var);
                        jcClass.defs = jcClass.defs.append(get);
                    }
                    if (!methodNames.contains("set" + fieldName)) {
                        JCTree.JCMethodDecl set = generateSetterMethod(var, jcClass);
                        jcClass.defs = jcClass.defs.append(set);
                    }
                }

                JCTree.JCClassDecl metaClass = generateMetaClass(entityTranslator.tbl, jcClass, fieldToType);
                jcClass.defs = jcClass.defs.append(metaClass);
                JCTree.JCMethodDecl methodDecl = generateMetaMethod(metaClass);
                jcClass.defs = jcClass.defs.append(methodDecl);
                //messager.printMessage(Diagnostic.Kind.NOTE, jcClass.toString());


            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    private JCTree.JCMethodDecl generateGetterMethod(JCTree.JCVariableDecl jcVariable) {

        JCTree.JCModifiers jcModifiers = treeMaker.Modifiers(Flags.PUBLIC);

        Name methodName = handleMethodSignature(jcVariable.getName(), "get");

        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        jcStatements.append(
                treeMaker.Return(treeMaker.Select(treeMaker.Ident(getNameFromString("this")), jcVariable.getName())));
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());

        JCTree.JCExpression returnType = jcVariable.vartype;

        List<JCTree.JCTypeParameter> typeParameters = List.nil();

        List<JCTree.JCVariableDecl> parameters = List.nil();

        List<JCTree.JCExpression> throwsClauses = List.nil();
        return treeMaker
                .MethodDef(jcModifiers, methodName, returnType, typeParameters, parameters, throwsClauses, jcBlock, null);
    }

    private JCTree.JCMethodDecl generateSetterMethod(JCTree.JCVariableDecl jcVariable, JCTree.JCClassDecl jcClass) throws ReflectiveOperationException {

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);

        Name variableName = jcVariable.getName();
        Name methodName = handleMethodSignature(variableName, "set");

        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        jcStatements.append(treeMaker.Exec(treeMaker
                .Assign(treeMaker.Select(treeMaker.Ident(getNameFromString("this")), variableName),
                        treeMaker.Ident(variableName))));


        jcStatements.append(treeMaker.Return(treeMaker.Ident(getNameFromString("this"))));

        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());

//        JCTree.JCExpression returnType = treeMaker.Type((Type) (Class.forName("com.sun.tools.javac.code.Type$JCVoidType").newInstance()));


        JCTree.JCExpression returnType = treeMaker.Type(jcClass.sym.type);

        List<JCTree.JCTypeParameter> typeParameters = List.nil();


        JCTree.JCVariableDecl variableDecl = treeMaker
                .VarDef(treeMaker.Modifiers(Flags.PARAMETER, List.nil()), jcVariable.name, jcVariable.vartype, null);
        List<JCTree.JCVariableDecl> parameters = List.of(variableDecl);

        List<JCTree.JCExpression> throwsClauses = List.nil();
        return treeMaker
                .MethodDef(modifiers, methodName, returnType, typeParameters, parameters, throwsClauses, jcBlock, null);

    }

    /**
     * @param metaClassName
     * @return public top.onceio.core.db.model.BaseCol<Meta> age = new top.onceio.core.db.model.BaseCol(this, OReflectUtil.getField(User.class, "age"));
     */
    private JCTree.JCVariableDecl generateColField(JCTree.JCFieldAccess entityClass, Name metaClassName, String fieldName, TypeMirror fieldType) {

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);

        Name variableName = names.fromString(fieldName);
        JCTree.JCTypeApply typeApply;
        if (fieldType.getKind().equals(TypeKind.BOOLEAN) || fieldType.getKind().equals(TypeKind.BYTE)
                || fieldType.getKind().equals(TypeKind.SHORT) || fieldType.getKind().equals(TypeKind.INT) || fieldType.getKind().equals(TypeKind.LONG)
                || fieldType.getKind().equals(TypeKind.FLOAT) || fieldType.getKind().equals(TypeKind.DOUBLE)) {
            typeApply = treeMaker.TypeApply(memberAccess(BaseCol.class.getName()), List.of(treeMaker.Ident(metaClassName)));
        } else {
            typeApply = treeMaker.TypeApply(memberAccess(StringCol.class.getName()), List.of(treeMaker.Ident(metaClassName)));
        }

        JCTree.JCExpression fn = memberAccess(OReflectUtil.class.getName() + ".getField");
        JCTree.JCMethodInvocation m = treeMaker.Apply(List.nil(), fn, List.of(entityClass, treeMaker.Literal(variableName.toString())));


        JCTree.JCNewClass metaVal = treeMaker.NewClass(
                null,
                List.nil(),
                typeApply,
                List.of(treeMaker.Ident(getNameFromString("this")), m),
                null
        );

        return treeMaker.VarDef(modifiers, variableName, typeApply, metaVal);

    }

    /**
     * public static class Meta extends top.onceio.core.db.model.BaseTable<Meta>  {
     * public top.onceio.core.db.model.BaseCol<Meta> age = new top.onceio.core.db.model.BaseCol(this, OReflectUtil.getField(User.class, "age"));
     * public top.onceio.core.db.model.StringCol<Meta> name = new top.onceio.core.db.model.StringCol(this, OReflectUtil.getField(User.class, "name"));
     * public Meta() {
     * super.bind("user",this, User.class);
     * }
     * }
     *
     * @return
     */
    private JCTree.JCClassDecl generateMetaClass(JCTree.JCAnnotation tblAnn, JCTree.JCClassDecl jcEntityClass, Map<String, TypeMirror> fieldToType) {
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.STATIC | Flags.PUBLIC);
        JCTree.JCFieldAccess entityClass = treeMaker.Select(treeMaker.Ident(jcEntityClass.name), names.fromString("class"));

        Name metaClassName = getNameFromString(META_CLASS_NAME);
        List<JCTree.JCTypeParameter> typeParameters = List.nil();
        JCTree.JCExpression extending = treeMaker.TypeApply(memberAccess(BaseMeta.class.getName()), List.of(treeMaker.Ident(metaClassName)));
        List<JCTree.JCExpression> implementing = List.nil();
        final ArrayList<JCTree> defs = new ArrayList<>();

        JCTree.JCMethodDecl m = generateMetaConstructionMethod(tblAnn, jcEntityClass, entityClass, metaClassName);
        defs.add(m);
        fieldToType.forEach((fieldName, fieldType) -> {
            defs.add(generateColField(entityClass, metaClassName, fieldName, fieldType));
        });
        JCTree.JCClassDecl metaClassDecl = treeMaker.ClassDef(modifiers, metaClassName, typeParameters, extending, implementing, List.from(defs));

        return metaClassDecl;
    }

    private JCTree.JCMethodDecl generateMetaConstructionMethod(JCTree.JCAnnotation tblAnn, JCTree.JCClassDecl jcEntityClass, JCTree.JCFieldAccess entityClass, Name methodName) {
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);

        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();

        JCTree.JCExpression fn = memberAccess(META_BIND_METHOD_NAME);

        String tableName = jcEntityClass.name.toString().replaceAll("([A-Z])", "_$1").toLowerCase();
        if (tableName.startsWith("_")) {
            tableName = tableName.substring(1);
        }
        for (JCTree.JCExpression arg : tblAnn.getArguments()) {
            String[] fv = arg.toString().split("=");
            if (fv[0].trim().equals("name")) {
                String v = fv[1].trim();
                tableName = v.substring(1, v.length() - 1);
            }
        }


        JCTree.JCMethodInvocation m = treeMaker.Apply(List.nil(), fn, List.of(treeMaker.Literal(tableName), memberAccess("this"), entityClass));

        jcStatements.append(treeMaker.Exec(m));
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());
        JCTree.JCExpression returnType = null;
        List<JCTree.JCTypeParameter> typeParameters = List.nil();
        List<JCTree.JCVariableDecl> parameters = List.nil();
        List<JCTree.JCExpression> throwsClauses = List.nil();

        return treeMaker
                .MethodDef(modifiers, names.fromString(CLASS_INIT_METHOD_NAME), null, typeParameters, parameters, throwsClauses, jcBlock, null);
    }

    private JCTree.JCMethodDecl generateMetaMethod(JCTree.JCClassDecl metaClass) {

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.STATIC | Flags.PUBLIC);

        Name methodName = getNameFromString(META_METHOD_NAME);

        JCTree.JCNewClass metaVal = treeMaker.NewClass(
                null,
                com.sun.tools.javac.util.List.nil(),
                treeMaker.Ident(metaClass.name),
                com.sun.tools.javac.util.List.nil(),
                null
        );
        JCTree.JCVariableDecl metaValDecl = treeMaker.VarDef(
                treeMaker.Modifiers(Flags.PARAMETER),
                names.fromString("metaVal"),
                treeMaker.Ident(metaClass.name),
                metaVal
        );

        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        jcStatements.append(treeMaker.Return(metaValDecl.getInitializer()));
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());

        JCTree.JCExpression returnType = treeMaker.Ident(metaClass.name);

        List<JCTree.JCTypeParameter> typeParameters = List.nil();

        List<JCTree.JCVariableDecl> parameters = List.nil();

        List<JCTree.JCExpression> throwsClauses = List.nil();

        return treeMaker
                .MethodDef(modifiers, methodName, returnType, typeParameters, parameters, throwsClauses, jcBlock, null);
    }


    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }

    private Name handleMethodSignature(Name name, String prefix) {
        return names.fromString(prefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.toString()));
    }

    private Name getNameFromString(String s) {
        return names.fromString(s);
    }
}