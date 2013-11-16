package com.zsofware.androidMqttLib.utils;

public class ClassHelper {

	public static Object newInstance(String clz) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Class<?> forName = Class.forName(clz);
		return forName.newInstance();

	}
}
