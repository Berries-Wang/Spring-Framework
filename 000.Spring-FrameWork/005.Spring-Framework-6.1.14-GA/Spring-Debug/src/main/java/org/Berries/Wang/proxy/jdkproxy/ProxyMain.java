package org.Berries.Wang.proxy.jdkproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * JDK动态代理实践一下
 */
public class ProxyMain {
	public static void main(String[] args) {

		// 创建原始对象(即被代理对象)
		ToBPayment payment = new ToBPaymentImpl();
		// 创建代理处理器
		InvocationHandler handler = new StuInvocationHandler(payment);
		// 生成代理类对象
		ToBPayment proxyInstance = (ToBPayment) Proxy.newProxyInstance(payment.getClass().getClassLoader(),
				payment.getClass().getInterfaces(), handler);
		proxyInstance.pay();

	}
}
