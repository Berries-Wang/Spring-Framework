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

package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

/**
 * Support base class for singleton registries which need to handle
 * {@link org.springframework.beans.factory.FactoryBean} instances,
 * integrated with {@link DefaultSingletonBeanRegistry}'s singleton management.
 *
 * <p>Serves as base class for {@link AbstractBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

	/**
	 * Cache of singleton objects created by FactoryBeans: FactoryBean name --> object
	 * <p>
	 * FactoryBean创建的对象与FactoryBean名称的映射关系缓存
	 */
	private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);


	/**
	 * Determine the type for the given FactoryBean.
	 *
	 * @param factoryBean the FactoryBean instance to check
	 * @return the FactoryBean's object type,
	 * or {@code null} if the type cannot be determined yet
	 */
	@Nullable
	protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Class<?>>)
						factoryBean::getObjectType, getAccessControlContext());
			} else {
				return factoryBean.getObjectType();
			}
		} catch (Throwable ex) {
			// Thrown from the FactoryBean's getObjectType implementation.
			logger.warn("FactoryBean threw exception from getObjectType, despite the contract saying " +
					"that it should return null if the type of its object cannot be determined yet", ex);
			return null;
		}
	}

	/**
	 * Obtain an object to expose from the given FactoryBean, if available
	 * in cached form. Quick check for minimal synchronization.
	 *
	 * @param beanName the name of the bean
	 * @return the object obtained from the FactoryBean,
	 * or {@code null} if not available
	 */
	@Nullable
	protected Object getCachedObjectForFactoryBean(String beanName) {
		return this.factoryBeanObjectCache.get(beanName);
	}

	/**
	 * Obtain an object to expose from the given FactoryBean.
	 * 从给定的FactoryBean获取要公开的对象
	 *
	 * @param factory           the FactoryBean instance
	 * @param beanName          the name of the bean
	 * @param shouldPostProcess whether the bean is subject to post-processing
	 * @return the object obtained from the FactoryBean
	 * @throws BeanCreationException if FactoryBean object creation failed
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		/**
		 * 判断FactoryBean是否是单例的，并且FactoryBean已经创建
		 * <在这里执行this.singletonObjects.get(beanName) 返回的是FactoryBean，>
		 * 即 this.singletonObjects.get(beanName).equals(factory)为true
		 *
		 * 如果工厂模式下需要维护单例,即是单例模式
		 */
		if (factory.isSingleton() && containsSingleton(beanName)) {
			// 加锁
			synchronized (getSingletonMutex()) {
				//尝试从缓存中获取，防止多线程下可能有别的线程已完成该单例Bean的创建
				Object object = this.factoryBeanObjectCache.get(beanName);
				// 这个单例的Bean还没有被初始化，则说明目前的线程是第一个获取这个Bean实例的线程
				if (object == null) {
					// 调用工厂方法，创建Bean实例
					object = doGetObjectFromFactoryBean(factory, beanName);
					// Only post-process and store if not put there already during getObject() call above
					// (e.g. because of circular reference processing triggered by custom getBean calls)
					/**
					 * 前面已经加锁了，为什么这里还要校验一下缓存中是否存在Bean呢？
					 *
					 * [不是很明白]  是因为FactoryBean是用户自己实现的，因此可能是异步的，即调用getObject方法是异步的，即在Bean还没有创建完成就返回了。
					 * 如果这里不进行校验，那么就可能另外的线程调用doGetObjectFromFactoryBean方法又创建了一个Bean,这就破坏了单例了
					 */
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					} else {
						/**
						 *  shouldPostProcess(org.springframework.beans.factory.support.AbstractBeanDefinition#synthetic为true)参数的值
						 *  即表示目前的Bean是否是Spring框架生成的内部Bean
						 *
						 *  因为Spring框架不允许第三方框架来修改Spring内部的Bean，只允许修改业务上的Bean
						 */
						if (shouldPostProcess) {
							// 如果这个Bean还在创建中(其他线程做的)，那么就不要执行后置处理，也不要缓存这个Bean
							if (isSingletonCurrentlyInCreation(beanName)) {
								// Temporarily return non-post-processed object, not storing it yet..
								return object;
							}
							// 后置处理完成前，先加入到缓存中锁定起来，即标记为“创建中”,即:isSingletonCurrentlyInCreation(beanName)返回true
							beforeSingletonCreation(beanName);
							try {
								// Bean创建的后置操作,第三方框架可以进行AOP处理
								object = postProcessObjectFromFactoryBean(object, beanName);
							} catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							} finally {
								// Bean后置处理完成，从缓存中清理，即isSingletonCurrentlyInCreation(beanName)返回false
								afterSingletonCreation(beanName);
							}
						}
						if (containsSingleton(beanName)) {
							// Bean创建完成，证明单例创建完成，构建缓存
							this.factoryBeanObjectCache.put(beanName, object);
						}
					}
				}
				return object;
			}
		} else {// 如果不是单例模式
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (shouldPostProcess) {
				try {
					object = postProcessObjectFromFactoryBean(object, beanName);
				} catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}

	/**
	 * Obtain an object to expose from the given FactoryBean.
	 *
	 * @param factory  the FactoryBean instance
	 * @param beanName the name of the bean
	 * @return the object obtained from the FactoryBean
	 * @throws BeanCreationException if FactoryBean object creation failed
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
			throws BeanCreationException {

		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				object = factory.getObject();
			}
		} catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		} catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null) {
			if (isSingletonCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(
						beanName, "FactoryBean which is currently in creation returned null from getObject");
			}
			object = new NullBean();
		}
		return object;
	}

	/**
	 * Post-process the given object that has been obtained from the FactoryBean.
	 * The resulting object will get exposed for bean references.
	 * <p>The default implementation simply returns the given object as-is.
	 * Subclasses may override this, for example, to apply post-processors.
	 *
	 * @param object   the object obtained from the FactoryBean.
	 * @param beanName the name of the bean
	 * @return the object to expose
	 * @throws org.springframework.beans.BeansException if any post-processing failed
	 */
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
		return object;
	}

	/**
	 * Get a FactoryBean for the given bean if possible.
	 *
	 * @param beanName     the name of the bean
	 * @param beanInstance the corresponding bean instance
	 * @return the bean instance as FactoryBean
	 * @throws BeansException if the given bean cannot be exposed as a FactoryBean
	 */
	protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
		if (!(beanInstance instanceof FactoryBean)) {
			throw new BeanCreationException(beanName,
					"Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
		}
		return (FactoryBean<?>) beanInstance;
	}

	/**
	 * Overridden to clear the FactoryBean object cache as well.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		synchronized (getSingletonMutex()) {
			super.removeSingleton(beanName);
			this.factoryBeanObjectCache.remove(beanName);
		}
	}

	/**
	 * Overridden to clear the FactoryBean object cache as well.
	 */
	@Override
	protected void clearSingletonCache() {
		synchronized (getSingletonMutex()) {
			super.clearSingletonCache();
			this.factoryBeanObjectCache.clear();
		}
	}

	/**
	 * Return the security context for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the security context returned by this method.
	 *
	 * @see AccessController#getContext()
	 */
	protected AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}

}
