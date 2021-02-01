package top.onceio.core.beans;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiResolver.class);

    private Map<String, ApiPair> patternToApi = new TreeMap<>();
    private List<String> apis = new ArrayList<>();

    public ApiResolver push(HttpMethod httpMethod, String api, Object bean, Method method) {
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
        patternToApi.put(httpMethod.name() + ":" + pattern, new ApiPair(httpMethod, api, bean, method));
        return this;
    }

    public ApiResolver build() {
        apis.addAll(patternToApi.keySet());
        Collections.sort(apis, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o2, o1);
            }
        });
        for (String apiPttern : apis) {
            ApiPair ap = patternToApi.get(apiPttern);
            LOGGER.info(ap.getHttpMethod() + " " + ap.getApi());
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
