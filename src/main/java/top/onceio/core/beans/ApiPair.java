package top.onceio.core.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import top.onceio.core.mvc.annocations.Attr;
import top.onceio.core.mvc.annocations.Cookie;
import top.onceio.core.mvc.annocations.Header;
import top.onceio.core.mvc.annocations.Param;

public class ApiPair {
	private ApiMethod apiMethod;
	private String api;
	private Object bean;
	private Method method;
	
	private Map<String, Integer> nameVarIndex;
	private Map<String, Class<?>> nameType;
	private Map<Integer, String> paramNameArgIndex;
	private Map<Integer, String> attrNameArgIndex;
	private Map<Integer, String> cookieNameArgIndex;
	private Map<Integer, String> headerNameArgIndex;
	private Map<Class<?>,Integer> typeIndex;

	public ApiMethod getApiMethod() {
		return apiMethod;
	}

	public void setApiMethod(ApiMethod apiMethod) {
		this.apiMethod = apiMethod;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public Object getBean() {
		return bean;
	}

	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Map<String, Integer> getNameVarIndex() {
		return nameVarIndex;
	}

	public void setNameVarIndex(Map<String, Integer> nameVarIndex) {
		this.nameVarIndex = nameVarIndex;
	}

	public Map<String, Class<?>> getNameType() {
		return nameType;
	}

	public void setNameType(Map<String, Class<?>> nameType) {
		this.nameType = nameType;
	}

	public Map<Integer, String> getParamNameArgIndex() {
		return paramNameArgIndex;
	}

	public void setParamNameArgIndex(Map<Integer, String> paramNameArgIndex) {
		this.paramNameArgIndex = paramNameArgIndex;
	}

	public Map<Integer, String> getAttrNameArgIndex() {
		return attrNameArgIndex;
	}

	public void setAttrNameArgIndex(Map<Integer, String> attrNameArgIndex) {
		this.attrNameArgIndex = attrNameArgIndex;
	}

	public Map<Class<?>, Integer> getTypeIndex() {
		return typeIndex;
	}

	public void setTypeIndex(Map<Class<?>, Integer> typeIndex) {
		this.typeIndex = typeIndex;
	}

	public Map<Integer, String> getCookieNameArgIndex() {
		return cookieNameArgIndex;
	}

	public void setCookieNameArgIndex(Map<Integer, String> cookieNameArgIndex) {
		this.cookieNameArgIndex = cookieNameArgIndex;
	}

	public Map<Integer, String> getHeaderNameArgIndex() {
		return headerNameArgIndex;
	}

	public void setHeaderNameArgIndex(Map<Integer, String> headerNameArgIndex) {
		this.headerNameArgIndex = headerNameArgIndex;
	}

	public ApiPair(ApiMethod apiMethod, String api, Object bean, Method method) {
		super();
		this.apiMethod = apiMethod;
		this.api = api;
		this.bean = bean;
		this.method = method;
		String[] names = api.split("/");
		nameVarIndex = new HashMap<>(names.length);
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (!name.isEmpty()) {
				int end = name.length() - 1;
				if (name.charAt(0) == '{' && name.charAt(end) == '}') {
					nameVarIndex.put(name.substring(1, end), i);
				}
			}
		}
		if (method.getParameterCount() > 0) {
			nameType = new HashMap<>(method.getParameterCount());
			paramNameArgIndex = new HashMap<>(method.getParameterCount());
			attrNameArgIndex = new HashMap<>(method.getParameterCount());
			cookieNameArgIndex = new HashMap<>(method.getParameterCount());
			headerNameArgIndex = new HashMap<>(method.getParameterCount());
			typeIndex = new HashMap<>(method.getParameterCount());
			Parameter[] params = method.getParameters();
			for (int i = 0; i < params.length; i++) {
				Parameter param = method.getParameters()[i];
				Param paramAnn = param.getAnnotation(Param.class);
				Attr attrAnn = param.getAnnotation(Attr.class);
				Cookie cookieAnn = param.getAnnotation(Cookie.class);
				Header headerAnn = param.getAnnotation(Header.class);
				if (paramAnn != null) {
					paramNameArgIndex.put(i, paramAnn.value());
					nameType.put(paramAnn.value(), param.getType());
				} else if (attrAnn != null) {
					attrNameArgIndex.put(i, attrAnn.value());
					nameType.put(attrAnn.value(), param.getType());
				}else if (cookieAnn != null) {
					cookieNameArgIndex.put(i, cookieAnn.value());
					nameType.put(cookieAnn.value(), param.getType());
				}else if (headerAnn != null) {
					headerNameArgIndex.put(i, headerAnn.value());
					nameType.put(headerAnn.value(), param.getType());
				} else {
					typeIndex.put(param.getType(), i);
				}
			}
		}
	}

}
