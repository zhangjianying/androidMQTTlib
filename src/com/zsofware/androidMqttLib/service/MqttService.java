package com.zsofware.androidMqttLib.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.zsofware.androidMqttLib.app.MqttApp;
import com.zsofware.androidMqttLib.entry.SubscribeEntry;
import com.zsofware.androidMqttLib.utils.ClassHelper;
import com.zsofware.androidMqttLib.utils.StringHelper;

public class MqttService extends Service implements MqttCallback {
	private static String				DEBUG_TAG							= "MqttService";

	public static final String			FIELD_USERNAME						= "USERNAME";					// 用户名
	public static final String			FIELD_BROKER						= "BROKER";					// mqtt
	public static final String			FIELD_PASSWORD						= "PASSWORD";					// 服务器地址
	public static final String			FIELD_PROJECT						= "PROJECT";					// 项目名称
	public static final String			FIELD_SUBSCRIBELIST					= "SUBSCRIBELIST";
	public static final String			FIELD_PROT							= "PROT";
	private static final String			FIELD_ACTIONNAME					= "ACTIONNAME";
	private static final String			FIELD_RECEIVE_CALLBACK_CLASSNAME	= "RECEIVE_CALLBACK_CLASSNAME";

	private static final String			MQTT_THREAD_NAME					= "MqttService_connect_thread"; // 主线程ID
	private MqttDefaultFilePersistence	mDataStore;
	private MemoryPersistence			mMemStore;
	private MqttConnectOptions			mOpts;
	private static final String			DEVICE_ID_FORMAT					= "%s_%s";						// 设备ID
	private static final String			MQTT_URL_FORMAT						= "tcp://%s:%d";
	private MqttClient					mClient;
	private Handler						mConnHandler;
	private String						mDeviceId;

	private String						PROJECT								= "";
	private String						MQTT_BROKER							= "";
	private int							MQTT_PORT							= 1883;
	private String						USERNAME							= "";
	private String						PASSWORD							= "";
	private boolean						CLEANSESSIONFLAG					= false;
	private boolean						mStarted							= false;						// 服务当前状态
	private HandlerThread				MSGhandlerThread					= new HandlerThread(
																					"MQTT_MSG_thread");
	private Handler						MSGHandler							= null;
	private long						CONNECT_LOST_WAIT					= 15 * 1000L;					// 重连间隔
	private static final String			CONNECT_INFO_STORAGE				= "CONNECT_INFO_STORAGE";
	private boolean						INITIATIVE_EXIT						= false;
	private String						ACTION_NAME							= null;

	private String[]					subject								= null;						// 频道
	private int[]						qos									= null;						// 质量
	SharedPreferences					stroeInfo							= null;						// 存储对象
	private final static String			ENCODE								= "UTF-8";
	private final static String			SEND_MSG_KEY						= "SEND_MSG_KEY";
	private final static String			SEND_MSG_TOPIC						= "SEND_MSG_TOPIC";
	private MqttApp						mqttApp								= null;
	private String						ReceiveClassName					= null;						// 回调处理类

	private Context						serviceContext						= null;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * 读取基本参数
	 */
	private void readBaseParam() {
		USERNAME = stroeInfo.getString(FIELD_USERNAME, null);
		PASSWORD = stroeInfo.getString(FIELD_PASSWORD, null);
		PROJECT = stroeInfo.getString(FIELD_PROJECT, null);
		MQTT_BROKER = stroeInfo.getString(FIELD_BROKER, null);

		ACTION_NAME = stroeInfo.getString(FIELD_ACTIONNAME, null);
		MQTT_PORT = Integer.parseInt(stroeInfo.getString(FIELD_PROT, "1883"));

		// 回调处理类
		ReceiveClassName = stroeInfo.getString(
				FIELD_RECEIVE_CALLBACK_CLASSNAME, null);

		ArrayList<SubscribeEntry> Sublist = (ArrayList<SubscribeEntry>) JSON
				.parseArray(stroeInfo.getString(FIELD_SUBSCRIBELIST, null),
						SubscribeEntry.class);

		int SublistLength = Sublist.size();
		subject = new String[SublistLength];
		qos = new int[SublistLength];

		for (int i = 0; i < SublistLength; i++) {
			subject[i] = Sublist.get(i).getSubject();
			qos[i] = Sublist.get(i).getQos();
		}

		Log.d(DEBUG_TAG,
				String.format(
						"username = %s , project = %s , broker = %s , subject = [%s][%s]",
						USERNAME, PROJECT, MQTT_BROKER,
						Arrays.toString(subject), Arrays.toString(qos)));
	}

	/**
	 * 连接MQTT Service
	 */
	private void connect() {
		Log.v(DEBUG_TAG, "MqttService.connect()");
		mConnHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					mClient.connect(mOpts);
					mClient.subscribe(subject, qos);
					mClient.setCallback(MqttService.this);
					mStarted = true; // Service is now connected
					Log.i(DEBUG_TAG,
							"Successfully connected and subscribed starting keep alives");
					// startKeepAlives();
				} catch (Exception e) {
					e.printStackTrace();
					mStarted = false;
				} finally {
					// 判断是否连接上服务器
					if (!mClient.isConnected()) {
						Log.w(DEBUG_TAG, "connection lost try connect ag ");

						sleep(CONNECT_LOST_WAIT);

						connect();
					}
				}

			}
		});
	}

	private void startMqttService(Intent intent) {
		// 第一次启动的时候 intent 不为空,如果被休眠或者杀死 intent ==null
		if (intent != null) {
			Bundle extras = intent.getExtras();
			Editor editor = stroeInfo.edit();

			if (StringHelper.isEmpty(extras.getString(FIELD_USERNAME))) {
				Log.w(DEBUG_TAG, "FIELD_USERNAME 为空");
				INITIATIVE_EXIT = true;
				stopSelf();
			}

			if (StringHelper.isEmpty(extras.getString(FIELD_PASSWORD))) {
				Log.w(DEBUG_TAG, "FIELD_PASSWORD 为空");
				INITIATIVE_EXIT = true;
				stopSelf();
			}
			if (StringHelper.isEmpty(extras.getString(FIELD_PROJECT))) {
				Log.w(DEBUG_TAG, "FIELD_PROJECT 为空");
				INITIATIVE_EXIT = true;
				stopSelf();
			}
			if (StringHelper.isEmpty(extras.getString(FIELD_BROKER))) {
				Log.w(DEBUG_TAG, "FIELD_BROKER 为空");
				INITIATIVE_EXIT = true;
				stopSelf();
			}

			ArrayList<SubscribeEntry> Sublist = (ArrayList<SubscribeEntry>) extras
					.getSerializable(FIELD_SUBSCRIBELIST);
			if (Sublist != null && Sublist.size() == 0) {
				Log.w(DEBUG_TAG, "SubscribeEntry 为空");
				INITIATIVE_EXIT = true;
				stopSelf();
			}

			// 回调处理类
			if (!StringHelper.isEmpty(mqttApp.getMQTTReceiveActionClassName())) {
				editor.putString(FIELD_RECEIVE_CALLBACK_CLASSNAME,
						mqttApp.getMQTTReceiveActionClassName());
			}

			ACTION_NAME = intent.getAction();

			editor.putString(FIELD_ACTIONNAME, ACTION_NAME);
			editor.putString(FIELD_USERNAME, extras.getString(FIELD_USERNAME));
			editor.putString(FIELD_PROJECT, extras.getString(FIELD_PROJECT));
			editor.putString(FIELD_BROKER, extras.getString(FIELD_BROKER));
			editor.putString(FIELD_PASSWORD, extras.getString(FIELD_PASSWORD));
			editor.putString(FIELD_PROT, extras.getString(FIELD_PROT));

			// 保存 Sublist 数组,偷个懒直接转成JSON字符串再存
			editor.putString(FIELD_SUBSCRIBELIST, JSON.toJSONString(Sublist));

			editor.commit();

			readBaseParam();
		} else {
			// 休眠\被杀后再次获取上次保存的基本信息
			readBaseParam();
		}

		// 连接线程. 如果网络不好,或者根本没网络的情况下,怕FC,所以开个线程来连接
		HandlerThread connect_thread = new HandlerThread(MQTT_THREAD_NAME);
		connect_thread.start();
		mConnHandler = new Handler(connect_thread.getLooper());

		initMQTT();

		connect();
	}

	private void initMQTT() {

		// 消息接收处理线程
		if (!MSGhandlerThread.isAlive()) {
			MSGhandlerThread.start();
			if (MSGHandler == null) {
				MSGHandler = new Handler(MSGhandlerThread.getLooper(),
						new Callback() {
							@Override
							public boolean handleMessage(Message msg) {
								String topicname = msg.getData().getString(
										SEND_MSG_TOPIC);
								String send_msg = msg.getData().getString(
										SEND_MSG_KEY);

								try {
									AbsMQTTReceive absMqttReceive = (AbsMQTTReceive) ClassHelper
											.newInstance(ReceiveClassName);

									if (mqttApp.getMainContext() != null) {
										absMqttReceive.MsgReceive(
												mqttApp.getMainContext(),
												topicname, send_msg, false,
												mClient);
									} else {
										Log.i(DEBUG_TAG,
												"handleMessage by service");
										// Activity 已经被回收,或者被其他管家干掉
										// mqttApp.getIMQTTReceiveListener()
										absMqttReceive.MsgReceive(
												serviceContext, topicname,
												send_msg, true, mClient);
									}
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								} catch (InstantiationException e) {
									e.printStackTrace();
								} catch (IllegalAccessException e) {
									e.printStackTrace();
								}
								return true;
							}
						});
			}
		}

		mDeviceId = String.format(DEVICE_ID_FORMAT, PROJECT,
				Secure.getString(getContentResolver(), Secure.ANDROID_ID));
		Log.i(DEBUG_TAG, "mDeviceId:" + mDeviceId);
		try {
			mDataStore = new MqttDefaultFilePersistence(getCacheDir()
					.getAbsolutePath());
		} catch (MqttPersistenceException e) {
			mDataStore = null;
			mMemStore = new MemoryPersistence();
		}

		mOpts = new MqttConnectOptions();
		mOpts.setUserName(USERNAME);
		mOpts.setPassword(PASSWORD.toCharArray());
		mOpts.setCleanSession(CLEANSESSIONFLAG);

		String url = String.format(Locale.CHINA, MQTT_URL_FORMAT, MQTT_BROKER,
				MQTT_PORT);

		Log.i(DEBUG_TAG, "Connect url:" + url);

		try {
			if (mDataStore != null) {
				Log.i(DEBUG_TAG, "Connecting with DataStore");
				mClient = new MqttClient(url, mDeviceId, mDataStore);
			} else {
				Log.i(DEBUG_TAG, "Connecting with MemStore");
				mClient = new MqttClient(url, mDeviceId, mMemStore);
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(DEBUG_TAG, "MqttService.onStartCommand()");
		super.onStartCommand(intent, flags, startId);
		mqttApp = ((MqttApp) getApplicationContext());
		serviceContext = this;

		if (mStarted == false) {
			stroeInfo = getSharedPreferences(CONNECT_INFO_STORAGE, MODE_PRIVATE);
			startMqttService(intent);
		}

		return Service.START_STICKY;
	}

	@Override
	public void onCreate() {
		Log.v(DEBUG_TAG, "MqttService.onCreate()");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.v(DEBUG_TAG, "MqttService.onDestroy()");
		super.onDestroy();

		if (!INITIATIVE_EXIT) { // 非主动退出,且能捕获到OnDestroy的时候
			Intent intent = new Intent(ACTION_NAME);
			Bundle b = new Bundle();
			b.putString(MqttService.FIELD_PROJECT, PROJECT); // 项目名称
			b.putString(MqttService.FIELD_BROKER, MQTT_BROKER); // MQTT Server
			b.putString(MqttService.FIELD_USERNAME, USERNAME); // 用户名

			intent.putExtras(b);
			startService(intent);
		}
	}

	/**
	 * 当连接丢失的时候
	 */
	@Override
	public void connectionLost(Throwable arg0) {
		Log.i(DEBUG_TAG, "MqttService.connectionLost()");
		mStarted = false;

		sleep(CONNECT_LOST_WAIT);
		connect();
	}

	@Override
	public void deliveryComplete(MqttDeliveryToken arg0) {

	}

	/**
	 * 当连推送的消息送达的时候
	 */
	@Override
	public void messageArrived(MqttTopic topic, MqttMessage message)
			throws Exception {
		Log.i(DEBUG_TAG,
				"  Topic:\t" + topic.getName() + "  Message:\t"
						+ new String(message.getPayload()) + "  QoS:\t"
						+ message.getQos());

		Message msg = MSGHandler.obtainMessage();
		Bundle b = new Bundle();
		b.putString(SEND_MSG_KEY, new String(message.getPayload(), ENCODE));
		b.putString(SEND_MSG_TOPIC, topic.getName());

		msg.setData(b);
		msg.sendToTarget();
	}

	/**
	 * 休息一段时间
	 * 
	 * @param time
	 */
	private void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
