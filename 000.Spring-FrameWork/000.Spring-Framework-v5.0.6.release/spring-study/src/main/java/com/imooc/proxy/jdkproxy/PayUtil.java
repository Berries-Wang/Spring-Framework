package com.imooc.proxy.jdkproxy;

public class PayUtil {
	public static void beforePay() {
		System.out.println("支付之前的一系列动作");
	}

	public static void afterPay() {
		System.out.println("支付之后的一系列动作");
	}
}
