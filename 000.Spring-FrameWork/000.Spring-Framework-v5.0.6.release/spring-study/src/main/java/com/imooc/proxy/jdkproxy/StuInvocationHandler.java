package com.imooc.proxy.jdkproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK动态代理处理器
 */
public class StuInvocationHandler implements InvocationHandler {

	/**
	 * 被代理的对象
	 */
	private Object target;

	/**
	 * 通过构造函数将被代理的对象传递进来
	 *
	 * @param target 被代理的对象
	 */
	public StuInvocationHandler(Object target) {
		this.target = target;
	}


	/**
	 * @param proxy  JDK动态代理生成的代理类对象,(proxy.getClass().toString() -> "class com.sun.proxy.$Proxy0")
	 * @param method 被代理的方法
	 * @param args   方法的参数
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object returnVal = null;
		System.out.println("JDK 动态代理 invoke调用之前");

		returnVal = method.invoke(this.target, args);

		System.out.println("JDK 动态代理 invoke调用之后");

		return returnVal;
	}
}
