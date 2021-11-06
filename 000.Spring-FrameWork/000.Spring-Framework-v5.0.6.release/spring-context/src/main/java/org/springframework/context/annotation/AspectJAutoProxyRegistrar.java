/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers an {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
 * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
 * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see EnableAspectJAutoProxy
 * @since 3.1
 */
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
	 * {@code @Configuration} class.
	 * <p>
	 * >>>> 通过  "断点调试"  的方式就知道这个方法是在什么时候调用的
	 */
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		/**
		 * 往registry中注册类型为
		 * org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator(基于注解的自动代理创建器)
		 * 的BeanDefinition
		 * 该类间接继承了org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
		 */
		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		/**
		 * 从参数importingClassMetadata中获取EnableAspectJAutoProxy注解的属性值
		 * 即： exposeProxy的值&&proxyTargetClass的值
		 */
		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy != null) {
			/**
			 * proxyTargetClass 表明该类是使用CGLIB动态代理还是JDK动态代理
			 * true: 使用CGLIB的方式
			 * false: 尽可能使用JDK动态代理的方式
			 */
			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			// 解决内部调用不能使用代理的场景，默认为false表示不处理。
			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

}
