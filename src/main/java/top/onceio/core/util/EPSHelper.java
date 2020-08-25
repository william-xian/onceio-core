package top.onceio.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class EPSHelper<T> {
    private static final long TIME_WINDOW = 1000L;
    private int defaultMaxEPS;
    private Map<T, Integer> nameToMaxEPS;
    private ConcurrentHashMap<T, ConcurrentHashMap<Long, AtomicInteger>> nameToEPS = new ConcurrentHashMap<>(100);

    public EPSHelper(int defaultMaxEPS) {
        this.defaultMaxEPS = defaultMaxEPS;
        this.nameToMaxEPS = new HashMap<>(16);
    }

    public EPSHelper(Map<T, Integer> nameToMaxEPS, int defaultMaxEPS) {
        this.nameToMaxEPS = nameToMaxEPS;
        this.defaultMaxEPS = defaultMaxEPS;
    }

    public void waiting(T name) {
        do {
            ConcurrentHashMap<Long, AtomicInteger> empty = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, AtomicInteger> eps = null;
            while ((eps = nameToEPS.putIfAbsent(name, empty)) == null) ;

            long currentTime = System.currentTimeMillis();
            long time = currentTime - currentTime % TIME_WINDOW;
            AtomicInteger cnt = null;
            AtomicInteger ZERO = new AtomicInteger(0);
            while ((cnt = eps.putIfAbsent(time, ZERO)) == null) ;
            int waitingCnt = cnt.addAndGet(1);
            int maxEPS = nameToMaxEPS.getOrDefault(name, defaultMaxEPS);
            if (waitingCnt > maxEPS) {
                try {
                    //Thread.sleep((((waitingCnt - maxEPS) / maxEPS) * TIME_WINDOW) + (TIME_WINDOW + time - currentTime));
                    Thread.sleep(TIME_WINDOW + time - currentTime);
                } catch (InterruptedException e) {
                    OLog.error(e.toString());
                }
                nameToEPS.remove(time);
            } else {
                break;
            }
        } while (true);
    }
}