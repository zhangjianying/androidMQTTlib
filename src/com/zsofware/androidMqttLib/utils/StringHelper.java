package com.zsofware.androidMqttLib.utils;

public class StringHelper {
	public static boolean isEmpty(String str) {
		if (str == null || str.equals("")) {
			return true;
		}
		return false;
	}
}
