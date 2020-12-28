package top.onceio.core.beans;

import top.onceio.core.annotation.OnCreate;
import top.onceio.core.annotation.Validate;
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
    public final static String API = "api";
    public final static String MODEL = "model";
    private Map<String, Object> model = new HashMap<>();
    private Map<String, ApiGroupModel> api = new HashMap<>();

    public static class ApiGroupModel {
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
        public List<String> httpMethods;
        public List<TypeModel> params;
        public TypeModel returnType;
    }

    public static class TypeModel {
        public String type;
        public String name;
        public boolean nullable;
        public String pattern;
        public String ref;
        public String source;
    }

    @OnCreate
    public void init() {
        model.put(Object.class.getName(), Object.class.getName());
        model.put(Class.class.getName(), Class.class.getName());
        model.put(Type.class.getName(), Type.class.getName());
        model.put(String.class.getName(), String.class.getName());
        model.put(Long.class.getName(), Long.class.getName());
        model.put(Integer.class.getName(), Integer.class.getName());
        model.put(Short.class.getName(), Short.class.getName());
        model.put(Double.class.getName(), Double.class.getName());
        model.put(Float.class.getName(), Float.class.getName());
        model.put(Boolean.class.getName(), Boolean.class.getName());
        model.put(Byte.class.getName(), Byte.class.getName());
        model.put(Character.class.getName(), Character.class.getName());
        model.put(Void.class.getName(), Void.class.getName());
        model.put(void.class.getName(), void.class.getName());
        model.put(long.class.getName(), long.class.getName());
        model.put(int.class.getName(), int.class.getName());
        model.put(short.class.getName(), short.class.getName());
        model.put(double.class.getName(), double.class.getName());
        model.put(float.class.getName(), float.class.getName());
        model.put(boolean.class.getName(), boolean.class.getName());
        model.put(byte.class.getName(), byte.class.getName());
        model.put(char.class.getName(), char.class.getName());
        model.put(Date.class.getName(), Date.class.getName());
        model.put(Timestamp.class.getName(), Timestamp.class.getName());
        model.put(List.class.getName(), List.class.getName());
        model.put(Set.class.getName(), Set.class.getName());
        model.put(Map.class.getName(), Map.class.getName());
        model.put(Collection.class.getName(), Collection.class.getName());
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
            ApiGroupModel parent = this.api.get(name);
            if (parent == null) {
                parent = new ApiGroupModel();
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
                subApi.returnType = resolveType(bean, method);

                List<String> methodNames = new ArrayList<>();
                for (ApiMethod am : apiAnn.method()) {
                    methodNames.add(am.name());
                }
                subApi.httpMethods = methodNames;
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


    private TypeModel resolveType(Object bean, Method method) {
        Class<?> genType = null;
        Type t = null;
        if (DaoHolder.class.isAssignableFrom(bean.getClass())) {
            t = DaoHolder.class.getTypeParameters()[0];
            genType = OReflectUtil.searchGenType(DaoHolder.class, bean.getClass(), t);
        }
        TypeModel params = new TypeModel();
        if (method.getGenericReturnType().equals(t)) {
            resolveClass(bean, params, genType, method.getGenericReturnType());
        } else {
            resolveClass(bean, params, method.getReturnType(), method.getGenericReturnType());
        }
        return params;
    }

    private List<TypeModel> resolveParams(Object bean, Method method) {
        List<TypeModel> params = new ArrayList<>();
        for (int i = 0; i < method.getParameterCount(); i++) {
            TypeModel paramInfo = new TypeModel();
            Parameter param = method.getParameters()[i];
            Validate validate = param.getAnnotation(Validate.class);
            Class<?> paramType = method.getParameterTypes()[i];
            Type genericType = method.getGenericParameterTypes()[i];
            paramInfo.type = paramType.getName();
            String pName = null;
            String pSrc = null;
            do {
                Param pAnn = param.getAnnotation(Param.class);
                if (pAnn != null) {
                    pName = pAnn.value();
                    pSrc = "Param";
                    break;
                }
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
            } while (false);

            if (pName != null) {
                if (!pName.equals("")) {
                    paramInfo.name = pName;
                    paramInfo.source = pSrc;
                    resolveValidator(paramInfo, pName, validate, null);
                }
                resolveClass(bean, paramInfo, paramType, genericType);
            } else {
                paramInfo.name = param.getName();
            }
            params.add(paramInfo);
        }
        return params;
    }

    private void resolveValidator(TypeModel colModel, String name, Validate validate, Col col) {
        if (col != null && validate == null) {
            if (col.nullable() == false) {
                colModel.nullable = col.nullable();
            }
            if (!col.pattern().equals("")) {
                colModel.pattern = col.pattern();
            }
            if (!col.ref().equals(void.class)) {
                colModel.ref = col.ref().getName();
            }
        }
        if (validate != null) {
            if (col.nullable() == false) {
                colModel.nullable = validate.nullable();
            }
            if (!col.pattern().equals("")) {
                colModel.pattern = validate.pattern();
            }
            if (!col.ref().equals(void.class)) {
                colModel.ref = validate.ref().getName();
            }
        }
    }

    public void resolveClass(Object bean, TypeModel result, Class<?> type, Type genericType) {
        if (!type.equals(genericType) && bean != null && DaoHolder.class.isAssignableFrom(bean.getClass())) {
            Type t = DaoHolder.class.getTypeParameters()[0];
            Class<?> genType = OReflectUtil.searchGenType(DaoHolder.class, bean.getClass(), t);
            if (genericType.getTypeName().equals("E")) {
                result.type = genType.getName();
            } else {
                result.type = genericType.getTypeName().replace("<E>", "<" + genType.getName() + ">");
            }
            resolveModel(genType.getTypeName(), genType);
        } else {
            result.type = genericType.getTypeName();
        }
        resolveModel(genericType.getTypeName(), type);
    }

    public void resolveModel(String name, Class<?> type) {
        if (model.containsKey(name)) {
            return;
        }
        if (type.getName().startsWith("java")) {
            model.put(name, type.getName());
        } else {
            List<TypeModel> result = new ArrayList<>();
            Set<String> fieldNames = new HashSet<>();
            model.put(name, result);
            for (Class<?> clazz = type; clazz != null
                    && !OReflectUtil.isBaseType(clazz); clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || fieldNames.contains(field.getName())) {
                        continue;
                    }
                    fieldNames.add(field.getName());
                    TypeModel typeModel = new TypeModel();
                    typeModel.name = field.getName();
                    typeModel.type = field.getGenericType().getTypeName();
                    resolveModel(field.getGenericType().getTypeName(), field.getType());
                    Validate validate = field.getAnnotation(Validate.class);
                    Col col = field.getAnnotation(Col.class);
                    resolveValidator(typeModel, field.getName(), validate, col);
                    result.add(typeModel);
                }
            }
        }
    }

    @Api(value = "/apis")
    public Map<String, Object> apis() {
        Map<String, Object> result = new HashMap<>();
        List<ApiGroupModel> apiList = new ArrayList<>(api.values());
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
        result.put(API, apiList);
        result.put(MODEL, model);
        return result;
    }
}