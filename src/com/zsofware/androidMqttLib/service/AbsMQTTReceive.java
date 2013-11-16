package com.zsofware.androidMqttLib.service;

import android.content.Context;

public abstract class AbsMQTTReceive {
	public AbsMQTTReceive() {

	}

	/**
	 * 消息到达事件
	 * 
	 * @param context
	 * @param topicName
	 * @param msg
	 * @param notifyShowing
	 *            false --当前主窗体 true --service触发
	 */
	public abstract void MsgReceive(Context context, String topicName,
			String msg, boolean notifyShowing);
}
