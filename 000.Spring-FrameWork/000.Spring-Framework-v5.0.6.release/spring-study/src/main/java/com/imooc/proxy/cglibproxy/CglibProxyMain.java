package com.imooc.proxy.cglibproxy;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Enhancer;

/**
 * CGLIB代理实践一下
 */
public class CglibProxyMain {
	public static void main(String[] args) {

		System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY,
				"/home/wei/workspace/SOURCE_CODE/Spring-Framework-v5.0.6.release/spring-proxy");
		System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles",
				"true");

		AliPayment o = (AliPayment) Enhancer.create(AliPayment.class, new StuMethodInterceptor());
		o.pay();

	}
}
