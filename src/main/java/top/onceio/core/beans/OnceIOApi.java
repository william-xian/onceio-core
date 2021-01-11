package top.onceio.core.beans;

import top.onceio.core.annotation.OnCreate;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.mvc.annocations.*;
import top.onceio.core.util.OReflectUtil;

import java.lang.reflect.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

@Api("/docs")
public class OnceIOApi {
    /**
     * 特殊字段
     */
    private Map<String, TypeModel> model = new HashMap<>();
    private Map<String, ServiceModel> api = new HashMap<>();

    public static class OnceIOApiModel {
        public List<ServiceModel> api;
        public Map<String, TypeModel> model;
    }


    public static class ServiceModel {
        public String name;
        public String api;
        public String brief;
        public String entityClass;
        public List<ApiModel> subApi;
    }

    public static class ApiModel {
        public String name;
        public String api;
        public String brief;
        public String httpMethod;
        public List<FieldModel> params;
        public String returnType;
    }

    public static class TypeModel {
        public String type;
        public List<FieldModel> fields;

        public static TypeModel createBase(String name) {
            TypeModel model = new TypeModel();
            model.type = name;
            return model;
        }
    }

    public static class FieldModel {
        public String type;
        public String name;
        public String comment;
        public boolean nullable;
        public String pattern;
        public String defaultValue;
        public String ref;
        public String source;
    }

    @OnCreate
    public void init() {
        model.put(Object.class.getName(), TypeModel.createBase(Object.class.getName()));
        model.put(Class.class.getName(), TypeModel.createBase(Class.class.getName()));
        model.put(Type.class.getName(), TypeModel.createBase(Type.class.getName()));
        model.put(String.class.getName(), TypeModel.createBase(String.class.getName()));
        model.put(Long.class.getName(), TypeModel.createBase(Long.class.getName()));
        model.put(Integer.class.getName(), TypeModel.createBase(Integer.class.getName()));
        model.put(Short.class.getName(), TypeModel.createBase(Short.class.getName()));
        model.put(Double.class.getName(), TypeModel.createBase(Double.class.getName()));
        model.put(Float.class.getName(), TypeModel.createBase(Float.class.getName()));
        model.put(Boolean.class.getName(), TypeModel.createBase(Boolean.class.getName()));
        model.put(Byte.class.getName(), TypeModel.createBase(Byte.class.getName()));
        model.put(Character.class.getName(), TypeModel.createBase(Character.class.getName()));
        model.put(Void.class.getName(), TypeModel.createBase(Void.class.getName()));
        model.put(void.class.getName(), TypeModel.createBase(void.class.getName()));
        model.put(long.class.getName(), TypeModel.createBase(long.class.getName()));
        model.put(int.class.getName(), TypeModel.createBase(int.class.getName()));
        model.put(short.class.getName(), TypeModel.createBase(short.class.getName()));
        model.put(double.class.getName(), TypeModel.createBase(double.class.getName()));
        model.put(float.class.getName(), TypeModel.createBase(float.class.getName()));
        model.put(boolean.class.getName(), TypeModel.createBase(boolean.class.getName()));
        model.put(byte.class.getName(), TypeModel.createBase(byte.class.getName()));
        model.put(char.class.getName(), TypeModel.createBase(char.class.getName()));
        model.put(Date.class.getName(), TypeModel.createBase(Date.class.getName()));
        model.put(Timestamp.class.getName(), TypeModel.createBase(Timestamp.class.getName()));
        model.put(List.class.getName(), TypeModel.createBase(List.class.getName()));
        model.put(Set.class.getName(), TypeModel.createBase(Set.class.getName()));
        model.put(Map.class.getName(), TypeModel.createBase(Map.class.getName()));
        model.put(Collection.class.getName(), TypeModel.createBase(Collection.class.getName()));
        genericApis();
    }

    private void genericApis() {
        api.clear();
        Map<String, ApiPair> api = BeansEden.get().getApiResolver().getPatternToApi();
        Map<Object, Set<Method>> beanToMethods = new HashMap<>();

        for (Map.Entry<String, ApiPair> entry : api.entrySet()) {
            Method method = entry.getValue().getMethod();
            Object bean = entry.getValue().getBean();
            Set<Method> methods = beanToMethods.get(bean);
            if (methods == null) {
                methods = new HashSet<>();
                beanToMethods.put(bean, methods);
            }
            methods.add(method);
        }
        for (Map.Entry<Object, Set<Method>> entry : beanToMethods.entrySet()) {
            Object bean = entry.getKey();
            Class<?> beanClass = bean.getClass();
            String name = beanClass.getName().replaceAll("\\$\\$.*$", "");
            @SuppressWarnings("unchecked")
            ServiceModel parent = this.api.get(name);
            if (parent == null) {
                parent = new ServiceModel();
                parent.subApi = new ArrayList<>();
                parent.name = name;
                this.api.put(name, parent);

                String prefix = "";
                Api parentApi = beanClass.getAnnotation(Api.class);
                AutoApi parentAutoApi = beanClass.getAnnotation(AutoApi.class);
                if (parentApi != null) {
                    prefix = parentApi.value();
                    parent.brief = parentApi.brief();
                } else if (parentAutoApi != null) {
                    prefix = "/" + parentAutoApi.value().getSimpleName().toLowerCase();
                    parent.brief = parentAutoApi.brief();
                }
                parent.api = prefix;
                if (DaoHolder.class.isAssignableFrom(bean.getClass())) {
                    Class<?> entity = OReflectUtil.searchGenType(DaoHolder.class, bean.getClass(), DaoHolder.class.getTypeParameters()[0]);
                    parent.entityClass = entity.getName();
                }
            }
            for (Method method : entry.getValue()) {
                ApiModel subApi = new ApiModel();
                Api apiAnn = method.getAnnotation(Api.class);
                if (apiAnn == null) {
                    continue;
                }
                subApi.name = method.getName();
                subApi.params = resolveParams(bean, method);
                subApi.returnType = resolveModel(bean.getClass(), method.getGenericReturnType(), method.getReturnType());
                subApi.httpMethod = apiAnn.method().name();
                subApi.brief = apiAnn.brief();

                if (!apiAnn.value().equals("")) {
                    subApi.api = apiAnn.value();
                } else {
                    subApi.api = ("/" + method.getName()).replaceFirst("//", "/");
                }
                parent.subApi.add(subApi);
            }

        }
    }

    private List<FieldModel> resolveParams(Object bean, Method method) {
        List<FieldModel> params = new ArrayList<>();
        for (int i = 0; i < method.getParameterCount(); i++) {
            FieldModel paramInfo = new FieldModel();
            Parameter param = method.getParameters()[i];
            Param pAnn = param.getAnnotation(Param.class);
            Class<?> paramType = method.getParameterTypes()[i];
            Type genericType = method.getGenericParameterTypes()[i];
            paramInfo.type = paramType.getName();
            String pName = null;
            String pSrc = null;
            do {
                Header hAnn = param.getAnnotation(Header.class);
                if (hAnn != null) {
                    pName = hAnn.value();
                    pSrc = "Header";
                    break;
                }
                Cookie cAnn = param.getAnnotation(Cookie.class);

                if (cAnn != null) {
                    pName = cAnn.value();
                    pSrc = "Cookie";
                    break;
                }
                Attr aAnn = param.getAnnotation(Attr.class);
                if (aAnn != null) {
                    pName = aAnn.value();
                    pSrc = "Attr";
                    break;
                }
                if (pAnn != null) {
                    pName = !pAnn.value().equals("") ? pAnn.value() : pAnn.name();
                    pSrc = "Param";
                    break;
                }
            } while (false);

            if (pName != null) {
                if (!pName.equals("")) {
                    paramInfo.name = pName;
                    paramInfo.source = pSrc;
                    resolveValidator(paramInfo, pAnn, null);
                }
                paramInfo.type = resolveModel(bean.getClass(), genericType, paramType);
            } else {
                paramInfo.name = param.getName();
            }
            params.add(paramInfo);
        }
        return params;
    }

    private void resolveValidator(FieldModel colModel, Param param, Col col) {
        if (col != null) {
            if (col.nullable() == false) {
                colModel.nullable = col.nullable();
            }
            if (!col.pattern().equals("")) {
                colModel.pattern = col.pattern();
            }
            if (!col.ref().equals(void.class)) {
                colModel.ref = col.ref().getName();
            }
            if (!col.comment().equals("")) {
                colModel.comment = col.comment();
            }
            if (!col.defaultValue().equals("")) {
                colModel.defaultValue = col.defaultValue();
            }
        }
        if (param != null) {
            if (param.nullable() == false) {
                colModel.nullable = param.nullable();
            }
            if (!param.pattern().equals("")) {
                colModel.pattern = param.pattern();
            }
        }
    }

    public String resolveType(Class<?> beanClass, Type genericType, Class<?> type) {
        String actualType = type.getName();
        List<String> actualTypes = new ArrayList<>();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            for (Type t : parameterizedType.getActualTypeArguments()) {
                if (t instanceof ParameterizedType) {
                    if (t instanceof Class) {
                        resolveModel(beanClass, t, (Class<?>) t);
                    }
                    actualTypes.add(t.getTypeName());
                } else if (t instanceof TypeVariable) {
                    Class<?> fClass = OReflectUtil.searchGenType(Object.class, beanClass, t);
                    if (fClass != null) {
                        actualTypes.add(fClass.getTypeName());
                        resolveModel(beanClass, t, fClass);
                    } else {
                        actualTypes.add(t.getTypeName());
                    }
                } else {
                    if (t instanceof Class) {
                        resolveModel(beanClass, t, (Class<?>) t);
                    }
                    actualTypes.add(t.getTypeName());
                }
            }
            actualType = String.format("%s<%s>", type.getName(), String.join(",", actualTypes));
        } else if (genericType instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) genericType;
            Class<?> fClass = OReflectUtil.searchGenType(Object.class, beanClass, typeVariable);
            if (fClass != null) {
                //TODO循环递归 resolveModel(beanClass, typeVariable, fClass);
                actualType = fClass.getTypeName();
            } else {
            }
        } else {
        }
        return actualType;
    }

    /**
     * @param beanClass
     * @param genericType
     * @param type
     * @return actualType
     */
    public String resolveModel(Class<?> beanClass, Type genericType, Class<?> type) {
        String name = type.getName();
        String actualType = resolveType(beanClass, genericType, type);
        if (model.containsKey(name)) {
            return actualType;
        }
        if (type.getName().startsWith("java")) {
            model.put(name, TypeModel.createBase(type.getName()));
        } else {
            TypeModel typeModel = TypeModel.createBase(type.getName());
            List<FieldModel> result = new ArrayList<>();
            typeModel.fields = result;
            Set<String> fieldNames = new HashSet<>();
            model.put(name, typeModel);


            List<Class<?>> classes = new ArrayList<>();

            for (Class<?> clazz = type; clazz != null
                    && !OReflectUtil.isBaseType(clazz); clazz = clazz.getSuperclass()) {
                classes.add(0, clazz);
            }
            for (Class<?> clazz : classes) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || fieldNames.contains(field.getName())) {
                        continue;
                    }
                    fieldNames.add(field.getName());
                    FieldModel fieldModel = new FieldModel();
                    fieldModel.name = field.getName();
                    fieldModel.type = resolveModel(beanClass, field.getGenericType(), field.getType());
                    Col col = field.getAnnotation(Col.class);
                    Param param = field.getAnnotation(Param.class);
                    resolveValidator(fieldModel, param, col);
                    result.add(fieldModel);
                }
            }
        }
        return actualType;
    }

    @Api(value = "/apis")
    public OnceIOApiModel apis() {
        OnceIOApiModel result = new OnceIOApiModel();
        List<ServiceModel> apiList = new ArrayList<>(api.values());
        Collections.sort(apiList, (a, b) -> {
            int c = Objects.compare(a.api, b.api, String::compareToIgnoreCase);
            if (c != 0) {
                return c;
            }
            c = Objects.compare(a.name, b.name, String::compareToIgnoreCase);
            if (c != 0) {
                return c;
            }
            return 0;
        });
        result.api = apiList;
        result.model = model;
        return result;
    }
}