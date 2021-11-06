/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Closeable;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base class with common functionality for proxy processors, in particular
 * ClassLoader management and the {@link #evaluateProxyInterfaces} algorithm.
 *
 * @author Juergen Hoeller
 * @see AbstractAdvisingBeanPostProcessor
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
 * @since 4.1
 */
@SuppressWarnings("serial")
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

	/**
	 * This should run after all other processors, so that it can just add
	 * an advisor to existing proxies rather than double-proxy.
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;


	/**
	 * Set the ordering which will apply to this processor's implementation
	 * of {@link Ordered}, used when applying multiple processors.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 *
	 * @param order the ordering value
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the containing
	 * {@link org.springframework.beans.factory.BeanFactory} for loading all bean classes.
	 * This can be overridden here for specific proxies.
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	/**
	 * Return the configured proxy ClassLoader for this processor.
	 */
	@Nullable
	protected ClassLoader getProxyClassLoader() {
		return this.proxyClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}


	/**
	 * Check the interfaces on the given bean class and apply them to the {@link ProxyFactory},
	 * if appropriate（适当的）.
	 * <p>
	 * 校对给定的Bean的类型的相关接口，并且将适当的接口应用到ProxyFactory上
	 * <p>
	 * 那什么是适当的接口呢?? =>
	 *
	 * <p>Calls {@link #isConfigurationCallbackInterface} and {@link #isInternalLanguageInterface}
	 * to filter for reasonable(合理的) proxy interfaces, falling back to a target-class proxy otherwise.
	 *
	 * @param beanClass    the class of the bean
	 * @param proxyFactory the ProxyFactory for the bean
	 */
	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
		// 获取Bean Class所有的接口
		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
		boolean hasReasonableProxyInterface = false;

		// 遍历所有的接口
		for (Class<?> ifc : targetInterfaces) {
			/**
			 * 合适的标准:
			 *   1. 通过调用isConfigurationCallbackInterface来判断是否是Spring内置的回调接口
			 *   2. 通过调用isInternalLanguageInterface来判断是否是内部语言接口
			 *   3. 当前接口里面的方法数是否大于0
			 */
			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
					ifc.getMethods().length > 0) {
				/**
				 *  只要满足一个条件，则break; 即找到了合适的接口
				 * 那如果目标类集成了多个这样的接口的？
				 */
				hasReasonableProxyInterface = true;
				break;
			}
		}
		// 如果存在合适的接口
		if (hasReasonableProxyInterface) {
			/**
			 * Must allow for introductions; can't just set interfaces to the target's interfaces only.
			 * 自省
			 * 存在合适的接口，则再次遍历目标类所有的接口
			 */
			for (Class<?> ifc : targetInterfaces) {
				//  将接口添加到ProxyFactory中
				proxyFactory.addInterface(ifc);
			}
		} else {
			// 当没有合适的接口的时候，则使用代理对象进行代理，即CGLIB代理
			proxyFactory.setProxyTargetClass(true);
		}
	}

	/**
	 * Determine whether the given interface is just a container callback and
	 * therefore not to be considered as a reasonable proxy interface.
	 * <p>If no reasonable proxy interface is found for a given bean, it will get
	 * proxied with its full target class, assuming that as the user's intention.
	 *
	 * @param ifc the interface to check
	 * @return whether the given interface is just a container callback
	 */
	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
		return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
				AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
	}

	/**
	 * Determine whether the given interface is a well-known internal language interface
	 * and therefore not to be considered as a reasonable proxy interface.
	 * <p>If no reasonable proxy interface is found for a given bean, it will get
	 * proxied with its full target class, assuming that as the user's intention.
	 *
	 * @param ifc the interface to check
	 * @return whether the given interface is an internal language interface
	 */
	protected boolean isInternalLanguageInterface(Class<?> ifc) {
		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
				ifc.getName().endsWith(".cglib.proxy.Factory") ||
				ifc.getName().endsWith(".bytebuddy.MockAccess"));
	}

}
