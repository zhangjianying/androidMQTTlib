package com.zsofware.androidMqttLib.entry;

import java.io.Serializable;

/**
 * 订阅的频道
 * 
 * @author Administrator
 * 
 */
public class SubscribeEntry implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long	serialVersionUID	= -8561680609110807012L;

	public final static int		QOS_0				= 0;
	public final static int		QOS_1				= 1;
	public final static int		QOS_2				= 2;

	private String				subject;										// 主题
	private int					qos;											// 质量

	public SubscribeEntry() {
	}

	public SubscribeEntry(String subject, int qos) {
		super();
		this.subject = subject;
		this.qos = qos;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

}
