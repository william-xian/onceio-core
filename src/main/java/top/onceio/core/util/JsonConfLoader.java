package top.onceio.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import top.onceio.OnceIO;

public class JsonConfLoader {

	private final static Logger LOGGER = Logger.getLogger(JsonConfLoader.class);

	ObjectNode conf = new ObjectNode(JsonNodeFactory.instance);
	ObjectNode beans = new ObjectNode(JsonNodeFactory.instance);

	public ObjectNode getConf() {
		return conf;
	}

	public ObjectNode getBeans() {
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
							InputStream in;
							try {
								in = new FileInputStream(cnf);
								JsonNode jn = OUtils.mapper.reader().readTree(in);
								in.close();
								jn.fields().forEachRemaining(new Consumer<Entry<String, JsonNode>>() {
									@Override
									public void accept(Entry<String, JsonNode> arg) {
										if ("beans".equals(arg.getKey())) {
											arg.getValue().fields().forEachRemaining(new Consumer<Entry<String, JsonNode>>() {
												@Override
												public void accept(Entry<String, JsonNode> bean) {
													beans.set(bean.getKey(), bean.getValue());
												}
											});
										} else {
											conf.set(arg.getKey(), arg.getValue());
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
		beans.fields().forEachRemaining(new Consumer<Entry<String, JsonNode>>() {
			public void accept(Entry<String, JsonNode> t) {
				JsonNode clsFields = t.getValue();
				String clsName = clsFields.get("type") != null ? clsFields.get("type").textValue() : t.getKey();
				OLog.debug("resovleBeans" + clsName);
				try {
					Class<?> cls = OnceIO.getClassLoader().loadClass(clsName);
					Object bean = cls.newInstance();
					name2Bean.put(t.getKey(), bean);
					clsFields.fields().forEachRemaining(new Consumer<Entry<String, JsonNode>>() {
						@Override
						public void accept(Entry<String, JsonNode> t) {
							try {
								Field field = cls.getField(t.getKey());
								field.set(bean, OReflectUtil.toBaseType(t.getValue(), field.getType()));
							} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
									| SecurityException e) {
								LOGGER.warn(e.getMessage());
							}
						}

					});
					OLog.debug(cls+":"+t.getKey()+"->"+bean);

				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					LOGGER.warn(e.getMessage());
				}
			}

		});
		return name2Bean;
	}
}
