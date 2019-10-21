package top.onceio.core.beans;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class ApiResover {
    private static final Logger LOGGER = Logger.getLogger(ApiResover.class);

    private Map<String, ApiPair> patternToApi = new TreeMap<>();
    private List<String> apis = new ArrayList<>();

    public ApiResover push(ApiMethod apiMethod, String api, Object bean, Method method) {
        StringBuilder sb = new StringBuilder();
        String[] ts = api.split("/");
        for (String s : ts) {
            if (s.startsWith("{") && s.endsWith("}")) {
                sb.append("/[^/]+");
            } else if (!s.isEmpty()) {
                sb.append("/" + s);
            }
        }
        String pattern = sb.toString();
        patternToApi.put(apiMethod.name() + ":" + pattern, new ApiPair(apiMethod, api, bean, method));
        return this;
    }

    public ApiResover build() {
        apis.addAll(patternToApi.keySet());
        Collections.sort(apis, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o2, o1);
            }
        });
        for (String apiPttern : apis) {
            ApiPair ap = patternToApi.get(apiPttern);
            LOGGER.info(ap.getApiMethod() + " " + ap.getApi());
        }
        return this;
    }

    public Map<String, ApiPair> getPatternToApi() {
        return patternToApi;
    }

    public List<String> getApis() {
        return apis;
    }

}
