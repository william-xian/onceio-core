package cn.xian.app;

import top.onceio.core.annotation.BeanScan;
import top.onceio.core.beans.BeansEden;

@BeanScan("cn.xian.app")
public class Launcher {

    public static void main(String[] args) {
        BeansEden.get().resolve(new String[]{"conf"}, new String[]{"cn.xian.app"});
    }
}
