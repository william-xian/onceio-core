package top.onceio.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OLog {
	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static void debug(String format, Object... args) {
		StackTraceElement curStack = Thread.currentThread().getStackTrace()[2];
		String location = curStack.getClassName() + ":" + curStack.getLineNumber();
		String msg = String.format(format, args);
		System.out.printf("%s DEBUG %s  %s\n", SDF.format(new Date()), location, msg);
	}

	public static void info(String format, Object... args) {
		StackTraceElement curStack = Thread.currentThread().getStackTrace()[2];
		String location = curStack.getFileName() + ":" + curStack.getLineNumber();
		String msg = String.format(format, args);
		System.out.printf("%s LOG %s  %s\n", SDF.format(new Date()), location, msg);
	}

	public static void warn(String format, Object... args) {
		StackTraceElement curStack = Thread.currentThread().getStackTrace()[2];
		String location = curStack.getFileName() + ":" + curStack.getLineNumber();
		String msg = String.format(format, args);
		System.err.printf("%s WARN %s  %s\n", SDF.format(new Date()), location, msg);
	}

	public static void error(String format, Object... args) {
		StackTraceElement curStack = Thread.currentThread().getStackTrace()[2];
		String location = curStack.getFileName() + ":" + curStack.getLineNumber();
		String msg = String.format(format, args);
		System.err.printf("%s error %s  %s\n", SDF.format(new Date()), location, msg);
	}

	public static void fatal(String format, Object... args) {
		StackTraceElement curStack = Thread.currentThread().getStackTrace()[2];
		String location = curStack.getFileName() + ":" + curStack.getLineNumber();
		String msg = String.format(format, args);
		System.err.printf("%s fatal %s  %s\n", SDF.format(new Date()), location, msg);
	}
}
