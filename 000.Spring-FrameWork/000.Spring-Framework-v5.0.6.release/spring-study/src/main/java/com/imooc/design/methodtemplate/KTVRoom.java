package com.imooc.design.methodtemplate;

/**
 * 使用KTV来讲解模版方法模式
 */
public abstract class KTVRoom {

	/**
	 * 模版方法
	 */
	public void procedure() {

		// 打开设备
		openDevice();
		// 点歌
		orderSong();
		// 额外消费
		orderExtra();
		// 付款
		pay();

	}

	/**
	 * 模版自带方法，KTV最后是要付款的
	 */
	public void pay() {
		System.out.println("支付本次的消费账单....");
	}

	/**
	 * 钩子方法，额外的开销 "视情况" 而定
	 */
	public void orderExtra() {

	}

	/**
	 * 抽象方法，即子类必须实现的方法，必须得选歌
	 */
	public abstract void orderSong();

	/**
	 * 模版自带方法，使用前必须打开设备
	 */
	public void openDevice() {
		System.out.println("打开视频和音响");
	}
}
