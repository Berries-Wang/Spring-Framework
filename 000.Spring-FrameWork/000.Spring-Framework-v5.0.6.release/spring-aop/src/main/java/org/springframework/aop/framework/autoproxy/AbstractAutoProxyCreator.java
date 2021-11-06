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

package org.springframework.aop.framework.autoproxy;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not be any
 * common interceptors. If there are, they are set using the interceptorNames property.
 * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
 * in the current factory are used rather than bean references to allow correct handling
 * of prototype advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, e.g. by type,
 * by name, by definition details, etc. They can also return additional interceptors that
 * should just be applied to the specific bean instance. A simple concrete implementation is
 * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source: for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 * @since 13.10.2003
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/**
	 * Logger available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Default is global AdvisorAdapterRegistry
	 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Indicates whether or not the proxy should be frozen. Overridden from super
	 * to prevent the configuration from becoming frozen too early.
	 */
	private boolean freezeProxy = false;

	/**
	 * Default is no common interceptors
	 */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Set<Object> earlyProxyReferences = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

	/**
	 * 存放的是所有已经被处理过的Bean
	 * true: 表示被代理了的
	 * false： 表示被处理了，但是不需要被代理的
	 */
	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether or not the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>Overridden from the super class to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Specify the {@link AdvisorAdapterRegistry} to use.
	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
	 *
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 这里是设置TargetSourceCreator，用于创建TargetSource,从而来创建代理对象，调用是在：
	 * resources/org/springframework/aop/framework/autoproxy/AdvisorAutoProxyCreatorTests-custom-targetsource.xml
	 * resources/org/springframework/aop/framework/autoproxy/AdvisorAutoProxyCreatorTests-quick-targetsource.xml
	 * <p>
	 * Set custom {@code TargetSourceCreators} to be applied in this order.
	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
	 * will be created for each bean.
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
	 *
	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
	 *                             Ordering is significant: The {@code TargetSource} returned from the first matching
	 *                             {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning {@link BeanFactory}.
	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.contains(cacheKey)) {
			this.earlyProxyReferences.add(cacheKey);
		}
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	/**
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName  the name of the bean
	 * @return
	 * @throws BeansException
	 */
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		// 获取缓存的键
		Object cacheKey = getCacheKey(beanClass, beanName);

		/**
		 * 如果beanName为空:因为后续的add逻辑?
		 * this.targetSourcedBeans： 从代码this.targetSourcedBeans.add(beanName);看出，
		 * targetSourcedBeans存放的是用户自定义了TargetSource的BeanName，如果有缓存，那么就证明处理过了
		 */
		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			// 如果这个Bean已经被处理了,那么返回null，继续doCreateBean
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			/**
			 * isInfrastructureClass: 判断beanClass是否是Spring AOP中的基础类
			 * shouldSkip： 判断beanClass是否是Advisor,这个方法值得分析一下(是否该被跳过： 该bean是否是切面类)
			 */
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		/**
		 * Create proxy here if we have a custom TargetSource.
		 * Suppresses(取缔，压制) unnecessary(不必要) default instantiation of the target bean:
		 * The TargetSource will handle target instances in a custom fashion.(TargetSource将以自定义方式处理目标实例。)
		 *
		 * getCustomTargetSource 即获取《“用户自定了的TargetSource”,请注意，一定是用户自定义的》,见"020.AOP学习之TargetSource.md",
		 * 只有用户自定义了TargetSource的时候才有效，其实创建代理是在：org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)
		 */
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		// 如果获取到了用户自定义的TargetSource,则构建缓存,并且直接构建代理对象并返回.但是当用户没有自定义的时候，
		// 那么代理对象又在哪里创建的(答案: org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessAfterInitialization)
		// 但是代码为什么又执行到postProcessAfterInitialization(实现于org.springframework.beans.factory.config.BeanPostProcessor)中呢，回顾一下Bean创建的过程
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return pvs;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 * <p>
	 * 创建动态代理类的核心，这是 Bean级别的后置处理器，即实现于org.springframework.beans.factory.config.BeanPostProcessor
	 * 这里是对创建好的Bean进行后置增强
	 */
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
		if (bean != null) {
			// 如果有缓存，那么就是切面类了，切面类不用处理
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * <p>Note: As of 4.2.3, this implementation does not return a concatenated
	 * class/name String anymore but rather the most efficient cache key possible:
	 * a plain bean name, prepended with {@link BeanFactory#FACTORY_BEAN_PREFIX}
	 * in case of a {@code FactoryBean}; or if no bean name specified, then the
	 * given bean {@code Class} as-is.
	 *
	 * @param beanClass the bean class
	 * @param beanName  the bean name
	 * @return the cache key for the given class and name
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		} else {
			return beanClass;
		}
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible(有资格的; 合格的; 具备条件的;) for being proxied.
	 * 如果有必要，则对指定的Bean进行包装,即是否有资格被代理。
	 *
	 * @param bean     the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		// 如果包含在this.targetSourcedBeans中，则说明已经被包装过了(见代码org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessBeforeInstantiation)
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		// 如果是切面类，则返回
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		// 再次校验一下，见代码: org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessBeforeInstantiation
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		/**
		 * Create proxy if we have advice.(如果有切面通知，则创建代理)
		 * 获取目前Bean匹配上的Advisor,若匹配上了，则返回对应的advisor数组，反之，返回DO_NOT_PROXY;
		 */
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		// 当有advisor与Bean匹配上了，即当有匹配的Advisor时才回去创建代理对象
		if (specificInterceptors != DO_NOT_PROXY) {
			// 添加缓存,并设置为被代理了
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			/***
			 * 创建代理对象！！！
			 *
			 *  小朋友，是不是有点疑惑，为什么在属性注入完成之后再创建代理对象，之前注入的属性还能访问？
			 *  ------>>>>> 这里的bean能解释一切,以及AOP代理的并不是被代理类对象，而是TargetSource
			 *
			 *
			 * 其实AOP需要注意：
			 *   1. 各个方法(切面方法,被代理方法)之间的调用顺序
			 *   2. 调用顺序保证之后，就是被代理方法的方法接收者保存的问题了
			 */
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		// 当没有advisor与目前的Bean匹配上，则添加缓存并设置为不需要代理
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class
	 * that should never be proxied.
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 *
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>Sometimes we need to be able to avoid this happening if it will lead to
	 * a circular reference. This implementation returns {@code false}.
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether to skip the given bean
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return false;
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 *
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName  the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 * <p>
	 * 提供给用户，实现自定义AOP
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.(我们无法为直接注册的单例创建奇特的目标源)
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isDebugEnabled()) {
						logger.debug("TargetSourceCreator [" + tsc +
								" found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 * <p>
	 * 给指定的Bean创建AOP代理
	 *
	 * @param beanClass            the class of the bean 当前Bean的类型
	 * @param beanName             the name of the bean 当前Bean的名称
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null) 符合当前Bean的Advisor列表
	 * @param targetSource         the TargetSource for the proxy,  被代理对象包装器
	 *                             already pre-configured to access the bean 重要！！！，<<<<<<=== Spring AOP代理的是TargetSource =====>
	 *                             org.springframework.aop.target.SingletonTargetSource
	 * @return the AOP proxy for the bean 该Bean的代理对象
	 * @see #buildAdvisors
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
								 @Nullable Object[] specificInterceptors, TargetSource targetSource) {

		/**
		 * 为什么BeanFactory的类型得是ConfigurableListableBeanFactory？
		 * 给BeanDefinition设置上属性，用于
		 */
		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		// 创建代理工厂
		ProxyFactory proxyFactory = new ProxyFactory();
		// 复制代理相关的配置到代理工厂
		proxyFactory.copyFrom(this);

		/**
		 * 判断代理配置是否是直接通过接口来创建代理对象, false表示是的（默认值）,因为为TRUE表示是已经处理了，即使用CGLIB来创建代理对象
		 */
		if (!proxyFactory.isProxyTargetClass()) {
			// 判断当前Bean是否使用目标类来创建代理对象(即是否是使用CGLIB来创建代理)
			if (shouldProxyTargetClass(beanClass, beanName)) {
				// 修改ProxyFactory的配置，即当前Bean是使用CGLIB来创建代理对象的
				proxyFactory.setProxyTargetClass(true);
			} else {
				// 校对Bean所有的接口，并将合适的接口应用到ProxyFactory中,若没有，则使用目标类来创建代理类(即CGLIB代理)
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		// 获取BeanFactory中所有的Advisor
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);

		/**
		 * 注意，这里汇集了该所有与该Bean所匹配的Advisor,后续代理类执行的时候会从这里获取。
		 * ProxyFactory间接继承了org.springframework.aop.framework.AdvisedSupport
		 *
		 *
		 */
		proxyFactory.addAdvisors(advisors);

		/**
		 * 这个targetSource也是需要注意一下的
		 */
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);
		// 设置frozen标志位，即设置代理类创建完成之后是否能修改
		proxyFactory.setFrozen(this.freezeProxy);

		// org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		return proxyFactory.getProxy(getProxyClassLoader());
	}

	/**
	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 * <p>
	 * 判断给定的Bean是否使用他的目标类型进行创建代理而不是他的接口
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 *
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine(确定) the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted(改编，改写) to the Advisor interface.
	 *
	 * @param beanName             the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		// 将IOC容器中的拦截器转换为Advisor
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			allInterceptors.addAll(Arrays.asList(specificInterceptors));
			if (commonInterceptors.length > 0) {
				// 是否拦截器先调用
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				} else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isDebugEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.debug("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		// 遍历所有的Advisor
		for (int i = 0; i < allInterceptors.size(); i++) {
			// 因为allInterceptors元素的类型是Object，因此这里还是需要包装一下，即包装为Advisor
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * <p>
	 * 将指定的Interceptor转换为Advisor对象
	 *
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			// 当BeanFactor没有初始化完成或者该Bean不是在创建中
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 *
	 * @param proxyFactory ProxyFactory that is already configured with
	 *                     TargetSource and interfaces and will be used to create the proxy
	 *                     immediately after this method returns
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional
	 * advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 *
	 * @param beanClass          the class of the bean to advise
	 * @param beanName           the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 *                           {@link #getCustomTargetSource} method: may be ignored.
	 *                           Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
															 @Nullable TargetSource customTargetSource) throws BeansException;

}
