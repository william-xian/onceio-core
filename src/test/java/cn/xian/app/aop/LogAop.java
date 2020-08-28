package cn.xian.app.aop;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.cglib.proxy.MethodProxy;
import top.onceio.core.aop.ProxyAction;
import top.onceio.core.aop.annotation.Aop;
import top.onceio.core.util.OUtils;

@Aop(pattern = ".*", order = "1")
public class LogAop extends ProxyAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogAop.class);

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug(String.format("%s.%s(%s)", obj.getClass().getName(), method.getName(), OUtils.toJson(args)));
        }
        if (next() != null) {
            return next().intercept(obj, method, args, proxy);
        } else {
            return proxy.invokeSuper(obj, args);
        }
    }

}
