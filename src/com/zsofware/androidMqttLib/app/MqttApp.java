package com.zsofware.androidMqttLib.app;

import android.app.Application;
import android.content.Context;

public class MqttApp extends Application {
	private Context	mainContext;
	private String	MQTTReceiveActionClassName	= null;

	public String getMQTTReceiveActionClassName() {
		return MQTTReceiveActionClassName;
	}

	public void setMQTTReceiveActionClassName(String mQTTReceiveActionClassName) {
		this.MQTTReceiveActionClassName = mQTTReceiveActionClassName;
	}

	/**
	 * 主进程
	 * 
	 * @return
	 */
	public Context getMainContext() {
		return mainContext;
	}

	public void setMainContext(Context mainContext) {
		this.mainContext = mainContext;
	}

}
