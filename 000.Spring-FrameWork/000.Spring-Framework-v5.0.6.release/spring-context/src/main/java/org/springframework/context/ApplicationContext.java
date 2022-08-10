/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 * <p> 为应用程序提供配置的中心接口。这在应用程序运行时是只读的，但如果实现支持，则可能会重新加载。
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <p> 从org.springframework.beans.factory.ListableBeanFactory 继承访问应用程序组件的Bean Factory方法</p>
 *
 * <li>The ability(n.能力，能够；才能，技能，本领) to load file resources in a generic(adj.一般的;通用的;类的;) fashion(n.时髦打扮，流行装扮；时尚;v.制作.).
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <p>以一般方式加载文件资源的能力，继承自从org.springframework.core.io.ResourceLoader接口</p>
 *
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <p>发布事件给已经注册的监听器，继承自ApplicationEventPublisher接口</p>
 *
 * <li>The ability to resolve messages, supporting internationalization(国际化).
 * Inherited from the {@link MessageSource} interface.
 * <p>解析消息的能力，支持国际化。继承自MessageSource接口</p>
 *
 * <li>Inheritance(n.继承物，遗产；遗传特征) from a parent context. Definitions in a descendant(n.后裔，子孙；派生物，衍生物;adj.下降的;祖传的;) context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent(adj.自治的，独立的；自立的，自力更生的；公正的，不受他人影响的；分离的，单独的；私营的；无党派的；有主见的，有独立见解的;n.自治的，独立的；自立的，自力更生的；公正的，不受他人影响的；分离的，单独的；私营的；无党派的；有主见的，有独立见解的)
 * of that of any other servlet.
 * <p>从父上下文继承。 在子上下文中定义的会被优先考虑。这意味着，例如: 单独的父上下文可以被整个web应用使用，尽管每个servlet都有自己与其他servlet独立的子上下文。</p>
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities(n.能力；功能；性能；（军事）力量（capability 的复数）), ApplicationContext implementations detect(检测) and invoke(调用)
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 * <p>(In addition to: 除..外). 除了标准的BeanFactory生命周期功能外，ApplicationContext的实现者会检测了调用实现ApplicationContextAware
 * 、ResourceLoaderAware、ApplicationEventPublisherAware以及MessageSourceAware 接口的Bean的对应方法</p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 * <p>
 * EnvironmentCapable
 * ListableBeanFactory
 * HierarchicalBeanFactory
 * MessageSource
 * ApplicationEventPublisher
 * ResourcePatternResolver
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * Return the unique id of this application context.
	 *
	 * @return the unique id of the context, or {@code null} if none
	 * <p>获取应用上下文ID(并不是应用的ID)</p>
	 */
	@Nullable
	String getId();

	/**
	 * Return a name for the deployed application that this context belongs to.
	 *
	 * @return a name for the deployed application, or the empty String by default
	 * <p>返回此上下文所属的已部署应用程序的名称。</p>
	 */
	String getApplicationName();

	/**
	 * Return a friendly name for this context.
	 * <p>返回此上下文的友好名称(展示给外部的上下文名称?)</p>
	 *
	 * @return a display name for this context (never {@code null})
	 */
	String getDisplayName();

	/**
	 * Return the timestamp when this context was first loaded.
	 * <p>获取首次加载的时间</p>
	 *
	 * @return the timestamp (ms) when this context was first loaded
	 */
	long getStartupDate();

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * and this is the root of the context hierarchy(n.等级制度；统治集团；等级体系).
	 * <p>返回父上下文，如果没有父上下文并且这是上下文层次结构的根，则返回null</p>
	 *
	 * @return the parent context, or {@code null} if there is no parent
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * Expose(v.暴露;揭露;n.揭露;曝光;) AutowireCapableBeanFactory functionality(n.功能) for this context.
	 * <p>为该上下文暴露AutowireCapableBeanFactory的功能</p>
	 *
	 * <p>This is not typically used by application code, except for the purpose of
	 * initializing bean instances that live outside of the application context,
	 * applying the Spring bean lifecycle (fully or partly) to them.
	 * <p>Alternatively, the internal BeanFactory exposed by the
	 * {@link ConfigurableApplicationContext} interface offers access to the
	 * {@link AutowireCapableBeanFactory} interface too. The present method mainly
	 * serves as a convenient, specific facility on the ApplicationContext interface.
	 * <p><b>NOTE: As of 4.2, this method will consistently throw IllegalStateException
	 * after the application context has been closed.</b> In current Spring Framework
	 * versions, only refreshable application contexts behave that way; as of 4.2,
	 * all application context implementations will be required to comply.
	 *
	 * @return the AutowireCapableBeanFactory for this context
	 * @throws IllegalStateException if the context does not support the
	 *                               {@link AutowireCapableBeanFactory} interface, or does not hold an
	 *                               autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 *                               never been called), or if the context has been closed already
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
