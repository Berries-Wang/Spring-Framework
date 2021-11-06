/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import org.springframework.aop.SpringProxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 * @since 12.03.2004
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	/**
	 * 创建AopProxy
	 * * 注意:
	 * * 1. 通过这段代码可以看出，Spring还是比较 "偏向于JDK动态代理" 来创建代理对象，为什么？
	 * * >>> 猜测（两点）：1：因为JDK动态代理是JDK自带的代理方式，所以比较偏向于JDK动态代理；2：JDK动态代理会随着JDK的升级会得到性能的提升；
	 *
	 * @param config the AOP configuration in the form of an
	 *               AdvisedSupport object
	 * @return
	 * @throws AopConfigException
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		// 主要是判断是直接通过JDK动态代理还是使用CGLIB代理
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			// 获取目标类的类型
			Class<?> targetClass = config.getTargetClass();
			// 为空直接抛出异常
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 若目标类型是接口或者目标类是JDK动态代理的内部类，则依旧使用JDK动态代理
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// ====>>>>>使用CGLIB代理,这里并不是使用 spring-aop/src/main/java/org/springframework/aop/framework/CglibAopProxy.java
			return new ObjenesisCglibAopProxy(config);
		} else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
