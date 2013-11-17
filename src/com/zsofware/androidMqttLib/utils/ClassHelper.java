package com.zsofware.androidMqttLib.utils;

import java.util.HashMap;

public class ClassHelper {

	HashMap<String, Object>	cacheMap	= new HashMap<String, Object>();

	public static Object newInstance(String clz) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		
		return newInstance(clz, false);
	}

	public static Object newInstance(String clz, boolean useCache)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		Object retVal = null;
		if (useCache) {
		} else {
			Class<?> forName = Class.forName(clz);
			retVal = forName.newInstance();
		}
		
		return retVal;
	}
}
