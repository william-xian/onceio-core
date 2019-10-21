package cn.xian.app.beans;

import top.onceio.core.annotation.Def;
import top.onceio.core.annotation.Definer;
import top.onceio.core.cache.Cache;
import top.onceio.core.cache.impl.FIFOMemoryCache;

@Definer
public class BeansDefiner {

    @Def
    public Cache createFIFOMemoryCache() {
        return new FIFOMemoryCache(1000);
    }
}
