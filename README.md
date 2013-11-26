#androidMQTTlib
------

快速集成MQTT协议到Android客户端,你要做的仅仅是集成几个类,

然后实现消息到达的回调类即可.

##开发工具(Tools)

* `Eclipse4.3` [@下载](http://eclipse.org)
* `ADT v22`[@下载](http://developer.android.com/tools/sdk/eclipse-adt.html)

------

##使用的第三方库(Use a third-party library)
* `Eclipse MQTTT lib` [@主页](http://www.eclipse.org/paho/)
* `fastjson` [@主页](http://code.alibabatech.com/wiki/display/FastJSON/Documentation)

------
##集成方式(The integration steps)
1) 引入本包并添加依赖包 到项目的libs目录下(Add jars to project libs folder )

![package](http://zhangjianying.github.io/androidMQTTlib/img/1.jpg)

2)在项目中主Activity 和 Application类 分别继承 

com.zsofware.androidMqttLib.activity.MqttActivity 和 com.zsofware.androidMqttLib.app.MqttApp (extends MqttActivity and  MqttApp):

```java
 public class MyAppMainActivity extends MqttActivity {

 }
```
```java
 public class MyApp extends MqttApp {

 }
```
3)修改AndroidManifest.xml配置文件,增加service节点,将MyApp添加到xml中的application节点后再添加(modif AndroidManifest.xml,add application[android:name] and service config)

```xml
 <service
            android:name="com.talkweb.mbi1.service.MqttService"
            android:exported="false"
            android:priority="1000"
             >
            <intent-filter>
                <action android:name="com.zsoftware.mqttservice" /><!--根据你需要定义该名称,该名称也是服务启动名称-->
                <category android:name="android.intent.category.default" />
            </intent-filter>
        </service>
```

4)实现一个消息到达后的处理类,该类必须继承com.zsofware.androidMqttLib.service (the action callback Class),

如:
```java
import android.content.Context;
import android.util.Log;
import com.zsofware.androidMqttLib.service.AbsMQTTReceive;
import org.eclipse.paho.client.mqttv3.MqttClient;
public class MsgReceive extends AbsMQTTReceive {
    private static String    DEBUG_TAG	= "MsgReceive";

	@Override
	public void MsgReceive(Context context, String topicName, String msg,
			boolean notifyShowing,,MqttClient client) {
		Log.d(DEBUG_TAG, String.format("MsgReceive [%s]  [%s] [%s]", topicName,
				msg, notifyShowing));
    //notifyShowing = true ,则表示程序可能已经到后台或者被回收,当前的Context是service
    //notifyShowing = false,则表示当前程序还停留在MyAppMainActivity主界面上
	}

}
```
5)最后...是启动服务,给必要的服务启动参数(setting the basic information)
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
		b.putString(MqttService.FIELD_PASSWORD, "密码"); // 密码
		b.putString(MqttService.FIELD_PROT, "1883"); // 端口
		// 订阅主题
		SubscribeEntry sub1 = new SubscribeEntry("ALL_SUB",
				SubscribeEntry.QOS_2); // 自定义 全局主题
		SubscribeEntry sub2 = new SubscribeEntry("SINGLE/139****1697",
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

------
##常见问题
###1) **服务启动到后台经常被杀掉了?**

   这个需要设置一下内存回收软件如360手机安全卫士等.MQTTService本身会自动重新自动,
   只是不把应用加到白名单的话,360等安全软件会阻止服务起来.鉴于360白名单比较明显,这里只
   针对miui说一下设置方式:
   假设开发的程序名叫:mqttsmple
   
   ![mahua](http://zhangjianying.github.io/androidMQTTlib/img/all213213.jpg.jpg)
   
   在图片序号5可以发现,利用该框架的应用[手机经分],在正确设置白名单后.已经正常运行15个小时




###2)**网络断网后会继续连接么?**

会的,有断网重连机制


###3)**发送的信息会丢失么?**
  
  定于消息主题决定你的消息策略 SubscribeEntry.QOS_2,是丢失还是必须送达
  
  可参考 [@这里](http://www.eclipse.org/paho/files/mqttdoc/Cclient/qos.html)


###4)**服务器端你用的是m2m.eclipse.org,需要搭建私有服务器怎么做?**
  
  可以参考:
    
* mosquitto(推荐) [@主页](http://mosquitto.org/)
* Apache ActiveMQ(推荐) [@主页](http://activemq.apache.org/)
* 其他请google


###5) **通信安全怎么做?**

可以在服务器端设置哪些用户有写主题的权限或者通过通信协议本身来效验.本框架不考虑安全问题
  
###6) **FIELD_PROJECT参数有什么用?**
 
 在同一台终端中如果有多个应用使用本框架,为了让同一Broker(服务器)识别,则必须设定不同的4位项目代号.
 要不就会进入无尽的互踢掉线模式
 
###7)**怎么测试收到推送信息?**
  
  这里我直接贴出发送信息的代码.请加上依赖包 org.eclipse.paho.client.mqttv3.jar
  
```java
  public class Test {
	static boolean	MQTT_CLEAN_SESSION	= true;

	public static void main(String[] args) throws MqttException,
			UnsupportedEncodingException {
		MemoryPersistence mMemStore = new MemoryPersistence();

		MqttConnectOptions mOpts = new MqttConnectOptions();
		String userName = "admin";
		String passWord = "test";
		mOpts.setCleanSession(false);
		mOpts.setUserName(userName);
		mOpts.setPassword(passWord.toCharArray());
		final MqttClient mClient = new MqttClient("tcp://m2m.eclipse.org:1883",
				"and1213887383", mMemStore);
		mClient.connect(mOpts);
		System.out.println("isConnected = " + mClient.isConnected());
		MqttTopic presenceTopic = mClient.getTopic(String.format(Locale.US,
				"SINGLE/139****1697", "and1213887383")); //主题
		Random r = new Random();
		MqttMessage message = new MqttMessage(
				("{\"id\":\"M000001\", \"date\":\"1397209323\", \"content\":\"预存营销-120元的10元流量套餐年包减免优惠(2013年“两节”促销)\"}")
						.getBytes("UTF-8"));
		message.setQos(2);
		presenceTopic.publish(message);

		try {
			mClient.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
```

###8) **连不上服务器?**

检查你的服务器是否有开启,然后检查你是不是在AndroidManifest.xml中添加了联网权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
```