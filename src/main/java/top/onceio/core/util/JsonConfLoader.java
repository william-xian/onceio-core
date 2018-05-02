package top.onceio.core.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import top.onceio.OnceIO;

public class JsonConfLoader {

	private final static Logger LOGGER = Logger.getLogger(JsonConfLoader.class);

	private JsonObject conf = new JsonObject();
	private JsonObject beans = new JsonObject();

	public JsonObject getConf() {
		return conf;
	}

	public JsonObject getBeans() {
		return beans;
	}
	
	public static JsonConfLoader loadConf(String dir) {
		JsonConfLoader conf = new JsonConfLoader();
		conf.load(dir);
		return conf;
	}
	
	public void load(String dir)  {
		try {
			URL url = OnceIO.getClassLoader().getResource(dir);
			if(url != null) {
				File file = new File(url.getFile());
				if(file.exists()) {
					Files.walk(file.toPath(), FileVisitOption.FOLLOW_LINKS).forEach(path -> {
						File cnf = path.toFile();
						if(cnf.getName().endsWith(".json")) {
							try {
								JsonReader reader = OUtils.gson.newJsonReader(new FileReader(cnf));
								JsonObject jn = OUtils.gson.fromJson(reader, JsonObject.class);
								jn.entrySet().forEach(new Consumer<Entry<String, JsonElement>>() {
									@Override
									public void accept(Entry<String, JsonElement> arg) {
										if ("beans".equals(arg.getKey())) {
											arg.getValue().getAsJsonObject().entrySet().forEach(new Consumer<Entry<String, JsonElement>>() {
												@Override
												public void accept(Entry<String, JsonElement> bean) {
													beans.add(bean.getKey(), bean.getValue());
												}
											});
										} else {
											conf.add(arg.getKey(), arg.getValue());
										}
									}
								});
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								
							}
			
						}
						
					}
					);
				}
			}

		} catch (IOException e) {
			LOGGER.warn(e.getMessage());
		}
	}

	public Map<String,Object> resovleBeans() {
		Map<String,Object> name2Bean = new HashMap<>();		

		/** 初始分配内存 */
		beans.entrySet().forEach(new Consumer<Entry<String, JsonElement>>() {
			public void accept(Entry<String, JsonElement> t) {
				JsonObject clsFields = t.getValue().getAsJsonObject();
				JsonElement type = clsFields.get("@TYPE");
				String clsName = (type != null) ? type.getAsString() : t.getKey();
				try {
					Class<?> cls = OnceIO.getClassLoader().loadClass(clsName);
					Object bean = cls.newInstance();
					name2Bean.put(t.getKey(), bean);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					LOGGER.warn(e.getMessage());
				}
			}
		});
		beans.entrySet().forEach(new Consumer<Entry<String, JsonElement>>() {
			public void accept(Entry<String, JsonElement> t) {
				JsonObject clsFields = t.getValue().getAsJsonObject();
				Object bean = name2Bean.get(t.getKey());
				Class<?> cls = bean.getClass();
				clsFields.entrySet().forEach(new Consumer<Entry<String, JsonElement>>() {
					@Override
					public void accept(Entry<String, JsonElement> t) {
						if(t.getKey().equals("@TYPE")) {
							return;
						}
						try {
							Method method = OReflectUtil.getSetMethod(cls, t.getKey());
							if (method != null) {
								String strV = t.getValue().getAsString();
								if (strV != null) {
									if (strV.startsWith("@")) {
										method.invoke(bean, name2Bean.get(strV.substring(1)));
									} else {
										method.invoke(bean,
												OReflectUtil.strToBaseType(method.getParameterTypes()[0], strV));
									}
								} else {
									method.invoke(bean,
											OReflectUtil.strToBaseType(method.getParameterTypes()[0], strV));
								}
							} else {
								OLog.warn("not exist : " + t.getKey());
							}
						} catch (IllegalArgumentException | IllegalAccessException | SecurityException
								| InvocationTargetException e) {
							e.printStackTrace();
						}
					}

				});
				OLog.debug(t.getKey() + " -> " + bean);
			}
		});
		return name2Bean;
	}
	
}
