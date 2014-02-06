package org.apache.karaf.tooling.semantic;

import java.lang.reflect.Field;

public class ReflectUtil {

	@SuppressWarnings("unchecked")
	public static <T> T readField(Object instance, String name) {
		try {
			Field field = instance.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(instance);
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to read field.", e);
		}
	}

}
