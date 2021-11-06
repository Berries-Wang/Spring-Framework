package com.imooc.proxy.jdkproxy;

public class ToBPaymentImpl implements ToBPayment {
	@Override
	public void pay() {
		System.out.println("To B Payment");
	}
}
