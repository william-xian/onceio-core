package top.onceio.core.util;

public class IDGenerator {
    private static final long zero = 946684800000L;
    private static long lastTime = 0L;
    private static int sequence = 0;
    private static int code = 0;

    public static synchronized long next() {
        long time = System.currentTimeMillis();
        if (time == lastTime) {
            sequence++;
            if (sequence >= 4096) {
                while (lastTime >= time){
                    time = System.currentTimeMillis();
                }
                lastTime = time;
                sequence = 0;
            }
        } else {
            lastTime = time;
            sequence = 0;
        }
        /** time: 42 bits, sequence: 12 bits, code: 10 bits */
        return (time - zero) << 22 | (sequence) << 10 | (code);
    }

    public static long parseToTime(long id) {
        long time = (id >> 22) + zero;
        return time;
    }
}
