androidMQTTlib
==============

快速集成MQTT协议到Android客户端,你要做的仅仅是集成几个类,

然后实现消息到达的回调类即可.

##开发工具

* `Eclipse4.3` [@下载](http://eclipse.org)
* `ADT v22`[@下载](http://developer.android.com/tools/sdk/eclipse-adt.html)

##使用的第三方库
* `Eclipse MQTTT lib` [@主页](http://www.eclipse.org/paho/)
* `fastjson` [@主页](http://code.alibabatech.com/wiki/display/FastJSON/Documentation)

##集成方式
1) 引入本包并添加依赖包 到项目的libs目录下
![mahua](http://t29-3.yunpan.360.cn/p/800-600.9ca48a6acae0370d8221f10cf7c7fd157ce6cdad.e805db.jpg?t=8320ea97142b9ebce772fe50bb11d098&d=20131116)

2)在项目中主Activity 和 Application类 分别继承 

com.zsofware.androidMqttLib.activity 和 com.zsofware.androidMqttLib.app.MqttApp :

```java
 public class MyAppMainActivity extends MqttActivity {

 }
```
```java
 public class MyApp extends MqttApp {

 }
```
3)修改AndroidManifest.xml配置文件,增加service节点

```xml
 <service
            android:name="com.talkweb.mbi1.service.MqttService"
            android:exported="false"
            android:priority="1000"
             >
            <intent-filter>
                <action android:name="com.talkweb.mbi.mqtt" /><!--根据你需要定义该名称,该名称也是服务启动名称-->
                <category android:name="android.intent.category.default" />
            </intent-filter>
        </service>
```

4)实现一个消息到达后的处理类,该类必须继承com.zsofware.androidMqttLib.service,

如:
```java
import android.content.Context;
import android.util.Log;
import com.zsofware.androidMqttLib.service.AbsMQTTReceive;
public class MsgReceive extends AbsMQTTReceive {
    private static String	DEBUG_TAG	= "MsgReceive";

	@Override
	public void MsgReceive(Context context, String topicName, String msg,
			boolean notifyShowing) {
		Log.d(DEBUG_TAG, String.format("MsgReceive [%s]  [%s] [%s]", topicName,
				msg, notifyShowing));
    //notifyShowing = true ,则表示程序可能已经到后台或者被回收,当前的Context是service
    //notifyShowing = false,则表示当前程序还停留在MyAppMainActivity主界面上
	}

}
```
5)最后...是启动服务,给必要的服务启动参数
```java
 public class MyAppMainActivity extends MqttApp {
    private static String	DEBUG_TAG	= "SMPLE_MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(DEBUG_TAG, "MainActivity.onCreate()");
		// 启动服务
		Intent intent = new Intent("com.zsoftware.mqttservice");
		Bundle b = new Bundle();
		b.putString(MqttService.FIELD_PROJECT, "CMCC"); // 项目名称 只能必须是4位唯一代码
		b.putString(MqttService.FIELD_BROKER, "m2m.eclipse.org"); // MQTT Server
		b.putString(MqttService.FIELD_USERNAME, "139****1697"); // 用户名
		// 订阅主题
		SubscribeEntry sub1 = new SubscribeEntry("ALL_SUB",
				SubscribeEntry.QOS_2); // 自定义 全局主题
		SubscribeEntry sub2 = new SubscribeEntry("SINGLE/13975151697",
				SubscribeEntry.QOS_2); // 自定义 个人主题 根据USERNAME识别
		ArrayList<SubscribeEntry> list = new ArrayList<SubscribeEntry>();
		list.add(sub1);
		list.add(sub2);
		b.putSerializable(MqttService.FIELD_SUBSCRIBELIST, list);
		intent.putExtras(b);

		// 设置消息回调处理类
		try {
			setMQTTReceive(MsgReceive.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		startService(intent);
	}
 }
```

##常见问题
1) 服务启动到后台经常被杀掉了?
   这个需要设置一下内存回收软件如360手机安全卫士等.MQTTService本身会自动重新自动,
   只是不把应用加到白名单的话,360等安全软件会阻止服务起来.鉴于360白名单比较明显,这里只
   针对miui说一下设置方式:
   假设开发的程序名叫:mqttsmple
   
   ![mahua](http://t29-2.yunpan.360.cn/p/800-600.0c0960c6a8ac59da445e52ed9f29af81078437ea.411154.jpg?t=8320ea97142b9ebce772fe50bb11d098&d=20131116)
   
   在图片序号5可以发现,利用该框架的应用[手机经分],在正确设置白名单后.已经正常运行15个小时


2)网络断网后会继续连接么?

会的,有断网重连机制


3)发送的信息会丢失么?
  
  定于消息主题决定你的消息策略 SubscribeEntry.QOS_2,是丢失还是必须送达
  
  可参考 [@这里](http://www.eclipse.org/paho/files/mqttdoc/Cclient/qos.html)


4)服务器端你用的是m2m.eclipse.org,需要搭建私有服务器怎么做?
   太多了.
    
* mosquitto [@主页](http://mosquitto.org/)
* moquette-mqtt(JAVA) [@主页](https://code.google.com/p/moquette-mqtt/)
* 其他请google


5)通信安全怎么做?

可以在服务器端设置哪些用户有写主题的权限或者通过通信协议本身来效验.本框架不考虑安全问题
  
6) FIELD_PROJECT参数有什么用?
 
 在同一台终端中如果有多个应用使用本框架,为了让同一Broker(服务器)识别,则必须设定不同的4位项目代号.
 要不就会进入无尽的互踢掉线模式
