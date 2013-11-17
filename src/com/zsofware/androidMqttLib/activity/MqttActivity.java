package com.zsofware.androidMqttLib.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.zsofware.androidMqttLib.app.MqttApp;
import com.zsofware.androidMqttLib.service.AbsMQTTReceive;
import com.zsofware.androidMqttLib.utils.ClassHelper;

public class MqttActivity extends Activity {

	private static String	DEBUG_TAG	= "MqttActivity";

	@Override
	public void finish() {
		super.finish();
	}

	public void setMQTTReceive(Class<? extends AbsMQTTReceive> clz) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Log.i(DEBUG_TAG, String.format("%s", clz.getName()));

		// 这里只是为了检查可以生成对象,如果不能也可以及时抛出异常给前端界面
		Object newInstance = ClassHelper.newInstance(clz.getName(),true);
		if (!(newInstance instanceof AbsMQTTReceive)) {
			throw new InstantiationException("必须是 AbsMQTTReceive 的实现类");
		}
		((MqttApp) getApplication()).setMQTTReceiveActionClassName(clz
				.getName());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(DEBUG_TAG, "MqttActivity.onCreate()");

		((MqttApp) getApplication()).setMainContext(this);
	}

	public void onResume() {
		super.onResume();
		Log.v(DEBUG_TAG, "MqttActivity.onResume()");
		((MqttApp) getApplication()).setMainContext(this);
	}

	public void onPause() {
		super.onPause();
		Log.v(DEBUG_TAG, "MqttActivity.onPause()");
		((MqttApp) getApplication()).setMainContext(null);
	}

}
