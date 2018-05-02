package top.onceio.core.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OReflectUtil {

	public static Class<?> searchGenType(Class<?> forefather, Class<?> entity, Type fieldType) {
		List<Class<?>> classes = new ArrayList<>();
		Integer typeIndex = null;
		for (Class<?> clazz = entity; !clazz.equals(forefather); clazz = clazz.getSuperclass()) {
			classes.add(0, clazz);
		}
		classes.add(0, forefather);
		Class<?> father = classes.get(0);
		for (int i = 1; i < classes.size(); i++) {
			Class<?> son = classes.get(i);
			TypeVariable<?>[] param = father.getTypeParameters();
			Type types = son.getGenericSuperclass();
			Type[] genericTypes = null;

			if (types instanceof ParameterizedType) {
				genericTypes = ((ParameterizedType) types).getActualTypeArguments();
			}

			for (int pi = 0; pi < param.length; pi++) {
				if (fieldType.equals(param[pi])) {
					if (genericTypes != null) {
						fieldType = genericTypes[pi];
						typeIndex = pi;
						Class<?> javaBaseType = tranBaseType(fieldType);
						if (javaBaseType != null) {
							return javaBaseType;
						}
					} else {
						typeIndex = pi;
						fieldType = null;
					}
					break;
				}
			}

			if (fieldType != null) {
				for (TypeVariable<?> t : son.getTypeParameters()) {
					if (fieldType != null && fieldType.equals(t)) {
						fieldType = t;
						Class<?> javaBaseType = tranBaseType(fieldType);
						if (javaBaseType != null) {
							return javaBaseType;
						}
						break;
					}
				}
			} else if (typeIndex != null) {
				fieldType = son.getTypeParameters()[typeIndex];
				Class<?> javaBaseType = tranBaseType(fieldType);
				if (javaBaseType != null) {
					return javaBaseType;
				}
			} else {
				return null;
			}

			father = son;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T strToBaseType(Class<T> type, String val) {
		if (val == null) {
			return null;
		} else if (type.equals(String.class)) {
			return (T) val;
		} else if (type.equals(int.class) || type.equals(Integer.class)) {
			return (T) Integer.valueOf(val);
		} else if (type.equals(long.class) || type.equals(Long.class)) {
			return (T) Long.valueOf(val);
		} else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
			return (T) Boolean.valueOf(val);
		} else if (type.equals(byte.class) || type.equals(Byte.class)) {
			return (T) Byte.valueOf(val);
		} else if (type.equals(short.class) || type.equals(Short.class)) {
			return (T) Short.valueOf(val);
		} else if (type.equals(double.class) || type.equals(Double.class)) {
			return (T) Double.valueOf(val);
		} else if (type.equals(float.class) || type.equals(Float.class)) {
			return (T) Float.valueOf(val);
		} else if (type.equals(BigDecimal.class)) {
			return (T) BigDecimal.valueOf(Double.valueOf(val));
		} else if (type.equals(Date.class)) {
			return (T) Date.valueOf(val);
		}
		return null;
	}
	
	public static boolean isBaseType(Type type) {
		if (type == String.class || type == Character.class || type == char.class) {
			return true;
		} else if (type == Integer.class || type == int.class) {
			return true;
		} else if (type == Long.class || type == long.class) {
			return true;
		} else if (type == Boolean.class || type == boolean.class) {
			return true;
		} else if (type == Byte.class || type == byte.class) {
			return true;
		} else if (type == Short.class || type == short.class) {
			return true;
		} else if (type == Float.class || type == float.class) {
			return true;
		} else if (type == Double.class || type == double.class) {
			return true;
		} else if (type == BigDecimal.class) {
			return true;
		} else if (type == Date.class) {
			return true;
		}
		return false;
	}

	public static Class<?> tranBaseType(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		return null;
	}

	public static boolean isNumber(Object obj) {
		if (obj instanceof Number) {
			return true;
		}
		return false;
	}

	public static boolean isCharacter(Object obj) {
		if ((obj instanceof String) || (obj instanceof Character)) {
			return true;
		}
		return false;
	}
	
	private static Map<Class<?>,Map<String,Method>> clsSetter = new HashMap<>();
	
	public static Method getSetMethod(Class<?> cls,String fieldName) {
		Map<String,Method> setter = clsSetter.get(cls);
		if(setter == null) {
			setter = new HashMap<>();
			clsSetter.put(cls, setter);
			for(Method m:cls.getMethods()) {
				if(m.getName().startsWith("set") && m.getParameterCount() == 1) {
					setter.put(m.getName(), m);
				}
			}
		}
		int mask = ~32;
		char c = (char)(fieldName.charAt(0) & mask);
		String methodName = "set"+c+fieldName.substring(1);
		return setter.get(methodName);
	}
}
