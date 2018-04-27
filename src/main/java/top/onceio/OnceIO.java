package top.onceio;

public final class OnceIO {
	private static ClassLoader _classLoader;

	public static void setClassLoader(ClassLoader classLoader) {
		_classLoader = classLoader;
	}

	public static ClassLoader getClassLoader() {
		if (_classLoader == null) {
			_classLoader = OnceIO.class.getClassLoader();
		}
		return _classLoader;
	}
}
