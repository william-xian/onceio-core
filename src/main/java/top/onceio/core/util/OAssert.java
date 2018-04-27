package top.onceio.core.util;

public class OAssert {

	public static void fatal(String format, Object... args) {
		fatal(false, format, args);
	}

	public static void fatal(boolean cnd, String format, Object... args) {
		assert cnd : String.format(format, args);
	}

	public static void err(boolean cnd, String format, Object... args) {
		assert cnd : String.format(format, args);
	}

	public static void err(String format, Object... args) {
		err(false, format, args);
	}

	public static void warnning(boolean cnd, String format, Object... args) {
		assert cnd : String.format(format, args);
	}

	public static void warnning(String format, Object... args) {
		warnning(false, format, args);
	}
}
