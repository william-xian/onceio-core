package top.onceio.beans;

import org.junit.Test;

import top.onceio.core.beans.HttpMethod;
import top.onceio.core.beans.ApiResolver;


public class ApiResolverTest {


    public void a() {
    }

    public void a1() {
    }

    public void ab() {
    }

    public void av1(String v1) {
    }

    public void av1b(String v1) {
    }

    public void av1v2(String v1, String v2) {
    }

    @Test
    public void test() throws NoSuchMethodException, SecurityException {
        ApiResolver r = new ApiResolver();
        ApiResolverTest bean = new ApiResolverTest();
        r.push(HttpMethod.GET, "/a/", bean, ApiResolverTest.class.getMethod("a"));
        r.push(HttpMethod.GET, "/a", bean, ApiResolverTest.class.getMethod("a"));
        r.push(HttpMethod.GET, "/a/b", bean, ApiResolverTest.class.getMethod("ab"));
        r.push(HttpMethod.GET, "/a/{v1}", bean, ApiResolverTest.class.getMethod("av1", String.class));
        r.push(HttpMethod.GET, "/a/{v1}/b", bean, ApiResolverTest.class.getMethod("av1", String.class));
        r.push(HttpMethod.GET, "/a/{v1}/{v2}", bean, ApiResolverTest.class.getMethod("av1v2", String.class, String.class));
        r.build();
    }
}
