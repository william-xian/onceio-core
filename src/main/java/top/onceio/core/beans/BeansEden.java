package top.onceio.core.beans;

import com.google.gson.JsonElement;
import net.sf.cglib.proxy.Enhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.onceio.core.annotation.*;
import top.onceio.core.aop.AopProxy;
import top.onceio.core.aop.ProxyAction;
import top.onceio.core.aop.ProxyChain;
import top.onceio.core.aop.annotation.*;
import top.onceio.core.aop.proxies.CacheEvictProxy;
import top.onceio.core.aop.proxies.CachePutProxy;
import top.onceio.core.aop.proxies.CacheableProxy;
import top.onceio.core.aop.proxies.TransactionalProxy;
import top.onceio.core.db.annotation.DefSQL;
import top.onceio.core.db.annotation.Model;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.dao.IdGenerator;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.model.DaoHelper;
import top.onceio.core.db.tables.OI18n;
import top.onceio.core.exception.Failed;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.util.*;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeansEden {
    private final static Logger LOGGER = LoggerFactory.getLogger(BeansEden.class);
    private final static String CLASS_FROM_CG_LIB = "$$EnhancerByCGLIB$$";
    public final Set<Class<?>> CLASSES = new HashSet<>();

    private ConcurrentHashMap<String, Object> nameToBean = new ConcurrentHashMap<>();
    List<Tuple3<String, Object, Method>> createMethods = new ArrayList<>();
    List<Tuple3<String, Object, Method>> destroyMethods = new ArrayList<>();
    private ApiResover apiResover = new ApiResover();
    private AnnotationScanner scanner = new AnnotationScanner(Api.class, AutoApi.class, Definer.class, Def.class,
            Using.class, Model.class, DefSQL.class, I18nMsg.class, I18nCfg.class, Aop.class);
    private static BeansEden instance = null;

    private BeansEden() {
    }

    public static BeansEden get() {
        synchronized (BeansEden.class) {
            if (instance == null) {
                instance = new BeansEden();
            }
        }
        return instance;
    }

    public void addAnnotation(Class<?>... annotations) {
        scanner.getFilter().addAll(Arrays.asList(annotations));
    }

    public Set<Class<?>> getClassByAnnotation(Class<?> annotation) {
        return scanner.getClasses(annotation);
    }


    private IdGenerator createIdGenerator() {
        return new IdGenerator() {
            @Override
            public Long next(Class<?> entityClass) {
                return IDGenerator.next();
            }
        };
    }

    private void loadConfig(Class<?> clazz, Object bean, Field field) {
        Config cnfAnn = field.getAnnotation(Config.class);
        if (cnfAnn != null) {
            Class<?> fieldType = field.getType();
            String[] names = cnfAnn.value().split("\\.");
            JsonElement je = conf.getConf();
            for (String name : names) {
                if (je != null) {
                    je = je.getAsJsonObject().get(name);
                }
            }
            if (je != null) {
                String val = je.getAsString();
                try {
                    if (OReflectUtil.isBaseType(fieldType)) {
                        field.setAccessible(true);
                        field.set(bean, OReflectUtil.strToBaseType(fieldType, val));
                    } else {
                        LOGGER.error(String.format("属性不支持该类型：%s", fieldType.getName()));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error(String.format("找不到属性：%s", cnfAnn.value()));
            }
        }
    }

    private void loadConfig(Class<?> clazz, Object bean) {
        if (clazz != null && bean != null) {
            for (Field field : clazz.getFields()) {
                loadConfig(clazz, bean, field);
            }
        }
    }

    private void loadDefiner() {
        Set<Class<?>> definers = scanner.getClasses(Definer.class);
        for (Class<?> defClazz : definers) {
            try {
                Object def = defClazz.newInstance();
                loadConfig(defClazz, def);
                for (Method method : defClazz.getMethods()) {
                    Def defAnn = method.getAnnotation(Def.class);
                    if (defAnn != null) {
                        if (method.getParameterTypes().length == 0) {
                            Class<?> beanType = method.getReturnType();
                            if (!beanType.equals(void.class)) {
                                String beanName = defAnn.value();
                                try {
                                    Object bean = method.invoke(def);
                                    store(beanType, beanName, bean);
                                } catch (IllegalArgumentException | InvocationTargetException e) {
                                    LOGGER.warn("Def 生成Bean失败 " + e.getMessage());
                                }
                            } else {
                                LOGGER.warn("Def 作用在返回值上");
                            }
                        } else {
                            LOGGER.warn("Def 不支持带参数的构造函数");
                        }
                    }
                }
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void loadDefined() {
        Set<Class<?>> definers = scanner.getClasses(Def.class);
        for (Class<?> defClazz : definers) {
            try {
                Object bean = defClazz.newInstance();
                Def defAnn = defClazz.getAnnotation(Def.class);
                String beanName = defAnn.value();
                store(defClazz, beanName, bean);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void loadApiAutoApi() {
        Set<Class<?>> definers = scanner.getClasses(Api.class, AutoApi.class);
        for (Class<?> defClazz : definers) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("load Api: " + defClazz.getName());
            }
            try {
                AopProxy cglibProxy = new AopProxy();
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(defClazz);
                enhancer.setCallback(cglibProxy);
                Object bean = enhancer.create();

                store(defClazz, null, bean);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void linkBeans() {
        Iterator<Object> beans = new HashSet<>(nameToBean.values()).iterator();
        while (beans.hasNext()) {
            Object bean = beans.next();
            OReflectUtil.tracebackSuperclass(bean.getClass(), Object.class, clazz -> {
                for (Field field : clazz.getDeclaredFields()) {
                    loadConfig(clazz, bean, field);
                    Using usingAnn = field.getAnnotation(Using.class);
                    if (usingAnn != null) {
                        Object fieldBean = null;
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        if (usingAnn.value() != null && !usingAnn.value().equals("")) {
                            fieldBean = load(fieldType, usingAnn.value());
                        } else {
                            if (Collection.class.isAssignableFrom(fieldType)) {
                                List<?> beanList = loadList(fieldType);
                                fieldBean = beanList;
                            } else if (fieldType.isInterface()) {
                                List<?> beanList = loadList(fieldType);
                                if (beanList != null && beanList.size() == 1) {
                                    fieldBean = beanList.get(0);
                                } else {
                                    if (beanList == null) {
                                        LOGGER.error(String.format("找不到 %s:%s", fieldType.getName(), usingAnn.value()));
                                    } else {
                                        LOGGER.error(String.format("%s接口的Bean实例有多个(%s), 请使用Def(name='xxx')使用唯一的Bean实例", fieldType.getName(), beanList.size()));
                                    }
                                }
                            } else {
                                fieldBean = load(fieldType);
                            }
                        }
                        if (fieldBean != null) {
                            try {
                                field.set(bean, fieldBean);
                            } catch (IllegalArgumentException | IllegalAccessException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        } else {
                            LOGGER.error(String.format("找不到 %s:%s", fieldType.getName(), usingAnn.value()));
                        }
                    }
                }
            });
        }
    }

    private void checkOnCreateAddTo(Object bean, Method method, List<Tuple3<String, Object, Method>> list) {
        OnCreate onCreateAnn = method.getAnnotation(OnCreate.class);
        if (onCreateAnn != null) {
            if (method.getParameterCount() == 0) {
                list.add(new Tuple3(onCreateAnn.order(), bean, method));
            } else {
                LOGGER.error(String.format("初始化函数%s,不应该有参数", method.getName()));
            }
        }
    }

    private void checkOnDestroy(Object bean, Method method, List<Tuple3<String, Object, Method>> list) {
        OnDestroy onDestroyAnn = method.getAnnotation(OnDestroy.class);
        if (onDestroyAnn != null) {
            if (method.getParameterCount() == 0) {
                list.add(new Tuple3(onDestroyAnn.order(), bean, method));
            } else {
                LOGGER.error(String.format("初始化函数%s,不应该有参数", method.getName()));
            }
        }
    }

    private void resolveApi(Class<?> clazz, Api fatherApi, Api methodApi, Object bean, Method method) {
        String api = null;
        if (fatherApi.value().startsWith("/")) {
            api = fatherApi.value() + methodApi.value();
        } else {
            api = "/" + fatherApi.value() + methodApi.value();
        }
        ApiMethod[] apiMethods = methodApi.method();
        if (apiMethods.length == 0) {
            apiMethods = fatherApi.method();
        }
        if (apiMethods.length == 0) {
            LOGGER.error("Api的不能为空");
        }
        for (ApiMethod apiMethod : apiMethods) {
            apiResover.push(apiMethod, api, bean, method);
        }
    }

    private void resolveAutoApi(Class<?> clazz, AutoApi autoApi, Api methodApi, Object bean, Method method, String methodName) {
        String api = "/" + autoApi.value().getSimpleName().toLowerCase();
        if (methodName != null && !methodName.equals("") && !methodName.equals("/")) {
            api = api + methodName;
        }
        for (ApiMethod apiMethod : methodApi.method()) {
            apiResover.push(apiMethod, api, bean, method);
        }
    }

    private void resolveBeanMethod() {
        Iterator<Object> beans = new HashSet<>(nameToBean.values()).iterator();
        while (beans.hasNext()) {
            Object bean = beans.next();
            Class<?> clazz = bean.getClass();
            if (clazz.getName().contains(CLASS_FROM_CG_LIB)) {
                clazz = clazz.getSuperclass();
            }
            Api fatherApi = clazz.getAnnotation(Api.class);
            AutoApi autoApi = clazz.getAnnotation(AutoApi.class);
            Set<String> ignoreMethods = new HashSet<>();
            for (Method method : clazz.getDeclaredMethods()) {
                checkOnCreateAddTo(bean, method, createMethods);
                checkOnDestroy(bean, method, destroyMethods);
                Api methodApi = method.getAnnotation(Api.class);
                if (fatherApi != null && methodApi != null) {
                    resolveApi(clazz, fatherApi, methodApi, bean, method);
                }
                if (autoApi != null && methodApi != null) {
                    ignoreMethods.add(method.getName() + method.getParameterTypes().hashCode());
                    if (!methodApi.value().equals("")) {
                        resolveAutoApi(clazz, autoApi, methodApi, bean, method, methodApi.value());
                    } else {
                        resolveAutoApi(clazz, autoApi, methodApi, bean, method, "/" + method.getName());
                    }
                }
                resolveMethodAop(clazz, method);
            }
            if (autoApi != null) {
                if (DaoHolder.class.isAssignableFrom(clazz)) {
                    for (Method method : DaoHolder.class.getDeclaredMethods()) {
                        Api methodApi = method.getAnnotation(Api.class);
                        if (methodApi != null
                                && !ignoreMethods.contains(method.getName() + method.getParameterTypes().hashCode())) {
                            if (!methodApi.value().equals("")) {
                                resolveAutoApi(clazz, autoApi, methodApi, bean, method, methodApi.value());
                            } else {
                                resolveAutoApi(clazz, autoApi, methodApi, bean, method, "/" + method.getName());
                            }
                        }
                    }
                }
            }
        }

        Comparator<Tuple3<String, Object, Method>> comparator = (o1, o2) -> {
            if (o1 == null || o1.a == null || o1.a.equals("")) {
                return -1;
            } else if (o2 == null || o2.a == null || o2.a.equals("")) {
                return -1;
            } else {
                return o1.a.compareTo(o2.a);
            }
        };
        Collections.sort(createMethods, comparator);
        Collections.sort(destroyMethods, comparator);

        for (Tuple3<String, Object, Method> tuple3 : createMethods) {
            Method method = tuple3.c;
            Object bean = tuple3.b;
            try {
                method.invoke(bean);
            } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        apiResover.build();
    }

    private Map<String, List<Class<?>>> patternToAopClass = new HashMap<>();

    private void resolveAop() {
        for (Class<?> clazz : scanner.getClasses(Aop.class)) {
            Aop aop = clazz.getAnnotation(Aop.class);
            String[] patterns = aop.pattern();
            if (patterns.length == 0) {
                patterns = aop.value();
            }
            for (String pattern : patterns) {
                List<Class<?>> aopClasses = patternToAopClass.get(pattern);
                if (aopClasses == null) {
                    aopClasses = new ArrayList<>();
                    patternToAopClass.put(pattern, aopClasses);
                }
                aopClasses.add(clazz);
            }
        }
    }

    private void resolveMethodAop(Class<?> clazz, Method method) {
        String func = clazz.getName() + "." + method.getName();

        Set<Class<?>> aopClazz = new HashSet<>();
        for (Map.Entry<String, List<Class<?>>> entry : patternToAopClass.entrySet()) {
            if (func.matches(entry.getKey())) {
                aopClazz.addAll(entry.getValue());
            }
        }
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable != null) {
            aopClazz.add(CacheableProxy.class);
        }

        CachePut cachePut = method.getAnnotation(CachePut.class);
        if (cachePut != null) {
            aopClazz.add(CachePutProxy.class);
        }
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        if (cacheEvict != null) {
            aopClazz.add(CacheEvictProxy.class);
        }
        Transactional tran = method.getAnnotation(Transactional.class);
        if (tran != null) {
            aopClazz.add(TransactionalProxy.class);
        }
        if (!aopClazz.isEmpty()) {
            ProxyChain aopChain = AopProxy.get(method);
            if (aopChain == null) {
                aopChain = new ProxyChain();
                AopProxy.push(method, aopChain);
                List<Class<?>> sorted = sortAopClass(aopClazz);
                for (int i = 0; i < sorted.size(); i++) {
                    ProxyAction action;
                    try {
                        action = (ProxyAction) sorted.get(i).newInstance();
                        aopChain.append(action);
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 根据Aop的顺序生成代理链
     */
    private List<Class<?>> sortAopClass(Set<Class<?>> collections) {
        List<Class<?>> list = new ArrayList<>(collections);
        Collections.sort(list, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                Aop aop1 = o1.getAnnotation(Aop.class);
                Aop aop2 = o2.getAnnotation(Aop.class);
                return aop1.order().compareTo(aop2.order());
            }

        });
        return list;
    }

    private JsonConfLoader conf = null;

    private void loadDefaultBeans() {
        DataSource ds = load(DataSource.class, null);
        OAssert.err(ds != null, "dataSource cannot be null");
        IdGenerator idGenerator = load(IdGenerator.class, null);
        if (idGenerator == null) {
            idGenerator = createIdGenerator();
            store(IdGenerator.class, null, idGenerator);
        }
        Set<Class<?>> classes = scanner.getClasses(Model.class, DefSQL.class);
        CLASSES.addAll(classes);
    }

    public void resolve(String[] confDir, String[] packages) {
        conf = JsonConfLoader.loadConf(confDir);
        scanner.scanPackages(packages);
        scanner.putClass(Def.class, JdbcHelper.class);
        scanner.putClass(Def.class, DaoHelper.class);
        scanner.putClass(Model.class, OI18n.class);
        scanner.putClass(AutoApi.class, OI18nHolder.class);

        Map<String, Object> confBeans = conf.resolveBeans();
        confBeans.forEach((name, bean) -> {
            store(bean.getClass(), name, bean);
        });

        resolveAop();

        loadDefiner();

        loadDefaultBeans();

        loadDefined();

        loadApiAutoApi();

        linkBeans();

        resolveBeanMethod();

        init();

    }

    public <T> void store(Class<T> clazz, String beanName, Object bean) {
        OAssert.err(bean != null, "%s:%s can not be null!", clazz.getName(), beanName);
        if (beanName == null || beanName.equals("")) {
            beanName = clazz.getName();
        }
        nameToBean.put(beanName, bean);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("store beanName=" + beanName);
        }

    }

    public <T> T load(Class<T> clazz) {
        return load(clazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> clazz, String beanName) {
        Object v = null;
        if (beanName == null || beanName.equals("")) {
            v = nameToBean.get(clazz.getName());
        } else {
            v = nameToBean.get(beanName);
        }
        if (v != null) {
            return (T) v;
        }
        return null;
    }

    public <T> List<T> loadList(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Object bean : nameToBean.values()) {
            if(clazz.isAssignableFrom(bean.getClass())) {
                result.add((T) bean);
            }
        }
        return result;
    }

    public Set<String> names() {
        return nameToBean.keySet();
    }

    public Object get(String beanName) {
        Object bean = nameToBean.get(beanName);
        return bean;
    }

    public void erase(String beanName) {
        Object bean = nameToBean.remove(beanName);
        Class<?> clazz = bean.getClass();
        if (bean != null) {
            for (Method method : clazz.getMethods()) {
                OnDestroy onDestroyAnn = method.getAnnotation(OnDestroy.class);
                if (onDestroyAnn != null) {
                    if (method.getParameterCount() == 0) {
                        try {
                            method.invoke(bean);
                        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    } else {
                        LOGGER.error(String.format("构造%s,不应该有参数", method.getName()));
                    }
                }
            }

        } else {
            LOGGER.error(String.format("找不到Bean对象：  %s:%s", clazz.getName(), beanName));
        }
    }

    /**
     * 按照顺序销毁Bean对象
     */
    public void destroy() {
        for (Tuple3<String, Object, Method> tuple3 : destroyMethods) {
            Method method = tuple3.c;
            Object bean = tuple3.b;
            try {
                method.invoke(bean);
            } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void init() {
        analysisI18nMsg();
        analysisConst();
    }

    private void analysisI18nMsg() {
        OI18nHolder dao = this.load(OI18nHolder.class);
        Set<Class<?>> classes = scanner.getClasses(I18nMsg.class);
        if (classes == null)
            return;
        List<OI18n> i18ns = new ArrayList<>();
        for (Class<?> clazz : classes) {
            I18nMsg group = clazz.getAnnotation(I18nMsg.class);
            for (Field field : clazz.getFields()) {
                field.setAccessible(true);
                try {
                    String name = field.get(null).toString();
                    String key = "msg/" + group.value() + "_" + OUtils.encodeMD5(name);
                    OI18n i18n = dao.fetch(OI18n.meta().id.eq(key));
                    if (i18n == null) {
                        i18n = new OI18n();
                        i18n.setId(key);
                        i18n.setName(name);
                        i18ns.add(i18n);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    Failed.throwError(e.getMessage());
                }
            }
        }
        dao.batchInsert(i18ns);
    }

    private void analysisConst() {
        OI18nHolder dao = this.load(OI18nHolder.class);
        Set<Class<?>> classes = scanner.getClasses(I18nCfg.class);
        if (classes == null)
            return;
        List<OI18n> i18ns = new ArrayList<>();
        for (Class<?> clazz : classes) {
            I18nCfg group = clazz.getAnnotation(I18nCfg.class);
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                I18nCfgBrief cons = field.getAnnotation(I18nCfgBrief.class);
                try {
                    String fieldName = field.getName();
                    String val = field.get(null).toString();
                    String key = "const/" + group.value() + "_" + clazz.getSimpleName() + "_" + fieldName;
                    String name = null;
                    if (cons != null) {
                        name = cons.value();
                    } else {
                        name = fieldName;
                    }
                    OI18n i18n = dao.fetch(OI18n.meta().id.eq(key));
                    if (i18n == null) {
                        i18n = new OI18n();
                        i18n.setId(key);
                        i18n.setName(name);
                        i18n.setVal(val);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("add: " + i18n);
                        }
                        i18ns.add(i18n);
                    } else {
                        /** The val depend on database */
                        if (!val.equals(i18n.getVal())) {
                            field.set(null, OReflectUtil.strToBaseType(field.getType(), i18n.getVal()));
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("reload: " + i18n);
                            }
                        }
                        if (!i18n.getName().equals(name)) {
                            i18n.setName(name);
                            dao.insert(i18n);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("update: " + i18n);
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                    Failed.throwError(e.getMessage());
                }
            }
        }
        dao.batchInsert(i18ns);
    }

    public ApiResover getApiResolver() {
        return apiResover;
    }
}
