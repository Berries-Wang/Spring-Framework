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

package org.springframework.aop.aspectj.annotation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving(檢索) @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 * <p>
 * 協助檢索被@AspectJ標注的Bean，並且基於他們構建Spring 中的Advisors，並將用於自動代理
 *
 * @author Juergen Hoeller
 * @see AnnotationAwareAspectJAutoProxyCreator
 * @since 2.0.2
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	/**
	 * 功能1： 存放切面类bean的名称，可用于判断spring中advisor是否已经构建过
	 */
	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory    the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 *
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 * <p>
	 * Eligible: 合格的，有資格的
	 */
	public List<Advisor> buildAspectJAdvisors() {
		List<String> aspectNames = this.aspectBeanNames;

		// 当没有处理过Advisor
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new LinkedList<>();
					aspectNames = new LinkedList<>();
					// 從BeanFactory(包括其父容器)中獲取到指定類型的Bean名稱
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					// 迭代所有的bean,构建Advisor
					for (String beanName : beanNames) {
						// 判斷當前BeanName是否有資格
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						/**
						 * 我們必須小心不要去急於實例化Bean在，在這種情況下，他們會被Spring 容器緩存，但是還沒有被编织
						 */
						Class<?> beanType = this.beanFactory.getType(beanName);
						if (beanType == null) {
							continue;
						}
						// 判斷這個beanType是否爲切面類型
						if (this.advisorFactory.isAspect(beanType)) {
							// 保存aspectName
							aspectNames.add(beanName);
							// 构造AspectJ的元数据
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) { // singleton
								// 构建Aspect实例工厂
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								// 获取切面类中所有的Advice，并且转换为Advisor
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 如果切面类是单例的，则构建对于Advisor的缓存，否则，则构建对应的AspectFactory的缓存
								if (this.beanFactory.isSingleton(beanName)) {
									this.advisorsCache.put(beanName, classAdvisors);
								} else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							} else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) { // aspect不能是单例的
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					// 构建缓存,表示advisor已经构建过
					this.aspectBeanNames = aspectNames;
					//  返回所有的Advisor
					return advisors;
				}
			}
		}

		// 当已经处理过一次了，则直接从缓存中获取
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new LinkedList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			} else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 *
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
