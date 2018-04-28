package top.onceio.core.beans;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.cglib.proxy.Enhancer;
import top.onceio.OnceIO;
import top.onceio.core.annotation.Config;
import top.onceio.core.annotation.Def;
import top.onceio.core.annotation.Definer;
import top.onceio.core.annotation.I18nCfg;
import top.onceio.core.annotation.I18nCfgBrief;
import top.onceio.core.annotation.I18nMsg;
import top.onceio.core.annotation.OnCreate;
import top.onceio.core.annotation.OnDestroy;
import top.onceio.core.annotation.Using;
import top.onceio.core.aop.AopProxy;
import top.onceio.core.aop.ProxyAction;
import top.onceio.core.aop.ProxyChain;
import top.onceio.core.aop.annotation.Aop;
import top.onceio.core.aop.annotation.CacheEvict;
import top.onceio.core.aop.annotation.CachePut;
import top.onceio.core.aop.annotation.Cacheable;
import top.onceio.core.aop.annotation.Transactional;
import top.onceio.core.aop.proxies.CacheEvictProxy;
import top.onceio.core.aop.proxies.CachePutProxy;
import top.onceio.core.aop.proxies.CacheableProxy;
import top.onceio.core.aop.proxies.TransactionalProxy;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.db.annotation.TblView;
import top.onceio.core.db.dao.Cnd;
import top.onceio.core.db.dao.DaoHolder;
import top.onceio.core.db.dao.IdGenerator;
import top.onceio.core.db.dao.impl.DaoHelper;
import top.onceio.core.db.jdbc.JdbcHelper;
import top.onceio.core.db.tbl.OEntity;
import top.onceio.core.db.tbl.OI18n;
import top.onceio.core.exception.Failed;
import top.onceio.core.mvc.annocations.Api;
import top.onceio.core.mvc.annocations.AutoApi;
import top.onceio.core.util.AnnotationScanner;
import top.onceio.core.util.IDGenerator;
import top.onceio.core.util.OAssert;
import top.onceio.core.util.OReflectUtil;
import top.onceio.core.util.OUtils;

public class BeansEden {
	private final static Logger LOGGER = Logger.getLogger(BeansEden.class);
	private Map<String, Object> nameToBean = new ConcurrentHashMap<>();
	private ApiResover apiResover = new ApiResover();
	ObjectNode conf = new ObjectNode(JsonNodeFactory.instance);
	ObjectNode beans = new ObjectNode(JsonNodeFactory.instance);
	private AnnotationScanner scanner = new AnnotationScanner(Api.class, AutoApi.class, Definer.class, Def.class,
			Using.class, Tbl.class, TblView.class, I18nMsg.class, I18nCfg.class, Aop.class);
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

	private void loadConf() {
		try {
			Enumeration<URL> iter = OnceIO.getClassLoader().getResources("conf");
			while(iter.hasMoreElements()) {
				URL url = iter.nextElement();
				if(url.getPath().endsWith(".json")) {
					InputStream in = url.openStream();
					JsonNode jn = OUtils.mapper.reader().readTree(in);
					in.close();
					jn.fields().forEachRemaining(new Consumer<Entry<String,JsonNode>>(){
						@Override
						public void accept(Entry<String, JsonNode> arg) {
							if("beans".equals(arg.getKey())) {
								arg.getValue().fields().forEachRemaining(new Consumer<Entry<String,JsonNode>>(){
									@Override
									public void accept(Entry<String, JsonNode> bean) {
										beans.set(bean.getKey(), bean.getValue());
									}
								});
							}else {
								conf.set(arg.getKey(), arg.getValue());
							}
						}
					});
				}
			}
			
			beans.fields().forEachRemaining(new Consumer<Entry<String,JsonNode>>() {
				public void accept(Entry<String, JsonNode> t) {
					JsonNode clsFields = t.getValue();
					String clsName = clsFields.get("type") != null?clsFields.get("type").textValue():t.getKey();
					try {
						Class<?> cls =  OnceIO.getClassLoader().loadClass(clsName);
						Object bean = cls.newInstance();
						if(cls.toString().equals(t.getKey())) {
							store(cls,"",bean);
						}else {
							store(cls,t.getKey(),bean);	
						}
						clsFields.fields().forEachRemaining(new Consumer<Entry<String,JsonNode>>() {
							@Override
							public void accept(Entry<String, JsonNode> t) {
								try {
									Field field = cls.getField(t.getKey());
									field.set(bean, OReflectUtil.toBaseType(t.getValue(),field.getType()));
								} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
						});
					
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		} catch (IOException e) {
			LOGGER.warn(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Class<? extends OEntity>> matchTblTblView() {
		List<Class<? extends OEntity>> entities = new LinkedList<>();
		for (Class<?> clazz : scanner.getClasses(Tbl.class)) {
			if (OEntity.class.isAssignableFrom(clazz)) {
				entities.add((Class<? extends OEntity>) clazz);
			}
		}
		for (Class<?> clazz : scanner.getClasses(TblView.class)) {
			if (OEntity.class.isAssignableFrom(clazz)) {
				entities.add((Class<? extends OEntity>) clazz);
			}
		}
		return entities;
	}

	private IdGenerator createIdGenerator() {
		return new IdGenerator() {
			@Override
			public Long next(Class<?> entityClass) {
				return IDGenerator.randomID();
			}
		};
	}

	private JdbcHelper createJdbcHelper(DataSource ds) {
		JdbcHelper jdbcHelper = new JdbcHelper();
		jdbcHelper.setDataSource(ds);
		return jdbcHelper;
	}

	private DaoHelper createDaoHelper(JdbcHelper jdbcHelper, IdGenerator idGenerator,
			List<Class<? extends OEntity>> entities) {
		DaoHelper daoHelper = new DaoHelper();
		daoHelper.init(jdbcHelper, idGenerator, entities);
		return daoHelper;
	}

	private void loadConfig(Class<?> clazz, Object bean, Field field) {
		Config cnfAnn = field.getAnnotation(Config.class);
		if (cnfAnn != null) {
			Class<?> fieldType = field.getType();
			String val = conf.get(cnfAnn.value()).textValue();
			if (val != null) {
				try {
					if (OReflectUtil.isBaseType(fieldType)) {
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
				AopProxy cglibProxy = new AopProxy();
				Enhancer enhancer = new Enhancer();
				enhancer.setSuperclass(defClazz);
				enhancer.setCallback(cglibProxy);
				Object bean = enhancer.create();

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
			Class<?> beanClass = bean.getClass();
			List<Class<?>> classes = new ArrayList<>();
			for (Class<?> clazz = beanClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
				classes.add(0, clazz);
			}
			for (Class<?> clazz : classes) {
				for (Field field : clazz.getDeclaredFields()) {
					loadConfig(clazz, bean, field);
					Using usingAnn = field.getAnnotation(Using.class);
					if (usingAnn != null) {
						Class<?> fieldType = field.getType();
						field.setAccessible(true);
						Object fieldBean = load(fieldType, usingAnn.value());
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
			}
		}
	}

	private void executeOnCreate(Object bean, Method method) {
		OnCreate onCreateAnn = method.getAnnotation(OnCreate.class);
		if (onCreateAnn != null) {
			if (method.getParameterCount() == 0) {
				try {
					method.invoke(bean);
				} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					LOGGER.error(e.getMessage(), e);
				}
			} else {
				LOGGER.error(String.format("初始化函数%s,不应该有参数", method.getName()));
			}
		}
	}

	private void checkOnDestroy(Object bean, Method method) {
		OnDestroy onDestroyAnn = method.getAnnotation(OnDestroy.class);
		if (onDestroyAnn != null) {
			if (method.getParameterCount() == 0) {
			} else {
				LOGGER.error(String.format("初始化函数%s,不应该有参数", method.getName()));
			}
		}
	}

	private void resoveApi(Class<?> clazz, Api fatherApi, Api methodApi, Object bean, Method method) {
		String api = fatherApi.value() + methodApi.value();
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

	private void resoveAutoApi(Class<?> clazz, AutoApi autoApi, Api methodApi, Object bean, Method method,
			String methodName) {
		String api = autoApi.value().getSimpleName().toLowerCase();
		if (methodName != null) {
			api = api + "/" + methodName;
		}
		for (ApiMethod apiMethod : methodApi.method()) {
			apiResover.push(apiMethod, api, bean, method);
		}
	}

	private void resoveBeanMethod() {
		Iterator<Object> beans = new HashSet<>(nameToBean.values()).iterator();
		while (beans.hasNext()) {
			Object bean = beans.next();
			Class<?> clazz = bean.getClass();
			if (clazz.getName().contains("$$EnhancerByCGLIB$$")) {
				clazz = clazz.getSuperclass();
			}
			Api fatherApi = clazz.getAnnotation(Api.class);
			AutoApi autoApi = clazz.getAnnotation(AutoApi.class);
			Set<String> ignoreMethods = new HashSet<>();
			for (Method method : clazz.getDeclaredMethods()) {
				executeOnCreate(bean, method);
				checkOnDestroy(bean, method);
				Api methodApi = method.getAnnotation(Api.class);
				if (fatherApi != null && methodApi != null) {
					resoveApi(clazz, fatherApi, methodApi, bean, method);
				}
				if (autoApi != null && methodApi != null) {
					ignoreMethods.add(method.getName() + method.getParameterTypes().hashCode());
					if (!methodApi.value().equals("")) {
						resoveAutoApi(clazz, autoApi, methodApi, bean, method, methodApi.value());
					} else {
						resoveAutoApi(clazz, autoApi, methodApi, bean, method, method.getName());
					}
				}

				resovleMethodAop(clazz, method);
			}
			if (autoApi != null) {
				if (DaoHolder.class.isAssignableFrom(clazz)) {
					for (Method method : DaoHolder.class.getDeclaredMethods()) {
						Api methodApi = method.getAnnotation(Api.class);
						if (methodApi != null
								&& !ignoreMethods.contains(method.getName() + method.getParameterTypes().hashCode())) {
							// TODO
							resoveAutoApi(clazz, autoApi, methodApi, bean, method, method.getName());
						}
					}
				}
			}
		}

		apiResover.build();
	}

	private Map<String, List<Class<?>>> patternToAopClass = new HashMap<>();

	private void resovleAop() {
		for (Class<?> clazz : scanner.getClasses(Aop.class)) {
			Aop aop = clazz.getAnnotation(Aop.class);
			String[] patterns = aop.pattern();
			if(patterns.length == 0) {
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

	private void resovleMethodAop(Class<?> clazz, Method method) {
		String func = clazz.getName() + "." + method.getName();

		Set<Class<?>> aopClazz = new HashSet<>();
		for (Map.Entry<String, List<Class<?>>> entry : patternToAopClass.entrySet()) {
			if (func.matches(entry.getKey())) {
				aopClazz.addAll(entry.getValue());
			}
		}
		Cacheable cacheable = method.getAnnotation(Cacheable.class);
		if(cacheable != null) {
			aopClazz.add(CacheableProxy.class);
		}
		
		CachePut cachePut = method.getAnnotation(CachePut.class);
		if(cachePut != null) {
			aopClazz.add(CachePutProxy.class);
		}
		CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
		if(cacheEvict != null) {
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

	public void resovle(String... packages) {
		loadConf();
		scanner.scanPackages(packages);
		scanner.putClass(Tbl.class, OI18n.class);
		scanner.putClass(AutoApi.class, OI18nHolder.class);

		resovleAop();

		loadDefiner();

		DataSource ds = load(DataSource.class, null);
		OAssert.err(ds != null, "dataSource cannot be null");
		IdGenerator idGenerator = load(IdGenerator.class, null);
		if (idGenerator == null) {
			idGenerator = createIdGenerator();
			store(IdGenerator.class, null, idGenerator);
		}
		JdbcHelper jdbcHelper = load(JdbcHelper.class, null);
		if (jdbcHelper == null) {
			jdbcHelper = createJdbcHelper(ds);
			store(JdbcHelper.class, null, jdbcHelper);
		}
		DaoHelper daoHelper = load(DaoHelper.class, null);
		if (daoHelper == null) {
			daoHelper = createDaoHelper(jdbcHelper, idGenerator, matchTblTblView());
			store(DaoHelper.class, null, daoHelper);
		}else {
			if(daoHelper.getEntities() == null) {
				daoHelper.init(jdbcHelper, idGenerator, matchTblTblView());
			}
		}
		loadDefined();

		loadApiAutoApi();

		linkBeans();

		resoveBeanMethod();

		init();

	}

	public <T> void store(Class<T> clazz, String beanName, Object bean) {
		if (beanName == null) {
			beanName = "";
		}
		OAssert.err(bean != null, "%s:%s can not be null!", clazz.getName(), beanName);
		LOGGER.debug("bean name=" + clazz.getName() + ":" + beanName);
		nameToBean.put(clazz.getName() + ":" + beanName, bean);
		Def def = clazz.getAnnotation(Def.class);
		if (def != null && def.nameByInterface()) {
			for (Class<?> iter : clazz.getInterfaces()) {
				nameToBean.put(iter.getName() + ":" + beanName, bean);
				LOGGER.debug("beanName=" + iter.getName() + ":" + beanName);
			}
		}

	}

	public <T> T load(Class<T> clazz) {
		return load(clazz, null);
	}

	@SuppressWarnings("unchecked")
	public <T> T load(Class<T> clazz, String beanName) {
		if (beanName == null) {
			beanName = "";
		}
		Object v = nameToBean.get(clazz.getName() + ":" + beanName);
		if (v != null) {
			return (T) v;
		}
		return null;
	}

	public <T> void erase(Class<T> clazz, String beanName) {
		if (beanName == null) {
			beanName = "";
		}
		Object bean = load(clazz, beanName);
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

	public void init() {
		annlysisI18nMsg();
		annlysisConst();
	}

	private void annlysisI18nMsg() {
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
					Cnd<OI18n> cnd = new Cnd<>(OI18n.class);
					cnd.eq().setKey(key);
					OI18n i18n = dao.fetch(null, cnd);
					if (i18n == null) {
						i18n = new OI18n();
						i18n.setKey(key);
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

	private void annlysisConst() {
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
					String fieldname = field.getName();
					String val = field.get(null).toString();
					String key = "const/" + group.value() + "_" + clazz.getSimpleName() + "_" + fieldname;
					String name = cons.value();
					Cnd<OI18n> cnd = new Cnd<>(OI18n.class);
					cnd.eq().setKey(key);
					OI18n i18n = dao.fetch(null, cnd);

					if (i18n == null) {
						i18n = new OI18n();
						i18n.setKey(key);
						i18n.setName(name);
						i18n.setVal(val);
						LOGGER.debug("add: " + i18n);
						i18ns.add(i18n);
					} else {
						/** The val depend on database */
						if (!val.equals(i18n.getVal())) {
							field.set(null, OReflectUtil.strToBaseType(field.getType(), i18n.getVal()));
							LOGGER.debug("reload: " + i18n);
						}
						if (!i18n.getName().equals(name)) {
							i18n.setName(name);
							dao.insert(i18n);
							LOGGER.debug("update: " + i18n);
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

	public ApiResover getApiResover() {
		return apiResover;
	}
}
