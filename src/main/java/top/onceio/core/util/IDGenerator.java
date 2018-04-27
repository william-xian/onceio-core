package top.onceio.core.util;

public class IDGenerator {
	private static final long zero = 946684800000L;
	private static long lasttime = 0L;
	private static int sequence = 0;
	private static int code = 0;

	public static synchronized long randomID() {
		long time = System.currentTimeMillis();
		if (time == lasttime) {
			sequence++;
			if (sequence >= 4096) {
				while (lasttime <= System.currentTimeMillis())
					;
				lasttime = time = System.currentTimeMillis();
				sequence = 1;
			}
		} else {
			lasttime = time;
			sequence = 1;
		}
		return (time - zero) << 22 | (sequence) << 10 | (code);
	}

	public static long parseToTime(long id) {
		long time = (id >> 22) + zero;
		return time;
	}
}
