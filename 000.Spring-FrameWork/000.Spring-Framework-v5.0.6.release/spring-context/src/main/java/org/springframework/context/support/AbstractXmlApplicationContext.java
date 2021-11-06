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

package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 
 * Convenient(方便的) base class for {@link org.springframework.context.ApplicationContext}
 * implementations(这是一个方面ApplicationContext实现者实现的基类), drawing（画） configuration from XML documents containing bean definitions
 * understood by an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 * 从xml文档中描述能够被XmlBeanDefinitionReader理解的Bean Definition
 *
 * <p>Subclasses just have to implement the {@link #getConfigResources} and/or
 * the {@link #getConfigLocations} method. Furthermore, they might override
 * the {@link #getResourceByPath} hook to interpret relative paths in an
 * environment-specific fashion, and/or {@link #getResourcePatternResolver}
 * for extended pattern resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	/**
	 * Create a new AbstractXmlApplicationContext with no parent.
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * Create a new AbstractXmlApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * via: 通过
	 * 
	 * Loads the bean definitions via an XmlBeanDefinitionReader.
	 * 通过XmlBeanDefinitionReader加载Bean Definition
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 * 
	 * @param: beanFactory 初始化容器时新建的BeanFactory
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		/**
		 *  Create a new XmlBeanDefinitionReader for the given BeanFactory.
		 *  通过给定的BeanFactory创建一个新的XmlBeanDefinitionReader
		 * 
		 *  注意：该参数类型是: org.springframework.beans.factory.support.BeanDefinitionRegistry
		 *   DefaultListableBeanFactory 实现了BeanDefinitionRegistry，从而将自身作为一个BeanDefinitionRegistry来提供给BeanDefinitionReader来使用
		 *  
		 */
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's resource loading environment.
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		/**
		 * 这里的this就是代表容器本身(因为AbstractRefreshableApplicationcontext间接实现了ResourceLoader接口)，这里将this设置到beanDefinitionReader中去，作为ReourceLoader，
		 * 将DefaultListableBeanFactory实例和容器(例如:FileSystemXmlApplicationContext)串联起来，打通了资源加载和BeanDefinition的注册的通路
		 * 
		 * 
		 */
		beanDefinitionReader.setResourceLoader(this);
		// EntityResolver: EntityResolver 就是用来处理 XML 验证的
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		/**
		 * Allow a subclass to provide custom initialization of the reader,then proceed with actually loading the bean definitions.
		 * 允许子类提供一个定制化的初始化动作给Reader,然后继续实际加载bean definition
		 * 
		 * ====>>> 例如，在子类org.springframework.context.support.AbstractXmlApplicationContext中，就是实现了对XML文件的校验
		 */
		initBeanDefinitionReader(beanDefinitionReader);
		/** 进行Bean Definition的加载，(因为基于FileSystemXmlApplicationContext分析)详见:org.springframework.context.support
		* .AbstractXmlApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.xml.XmlBeanDefinitionReader) */
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * Initialize the bean definition reader used for loading the bean
	 * definitions of this context. Default implementation is empty.
	 * <p>Can be overridden in subclasses, e.g. for turning off XML validation
	 * or using a different XmlBeanDefinitionParser implementation.
	 * @param reader the bean definition reader used by this context
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * Load the bean definitions with the given XmlBeanDefinitionReader.
	 * 通过给定的XmlBeanDefinitionReader加载Bean definition
	 * <p>The lifecycle of the bean factory is handled by the {@link #refreshBeanFactory}
	 * method; hence this method is just supposed to load and/or register bean definitions.
	 * @param reader the XmlBeanDefinitionReader to use
	 * @throws BeansException in case of bean registration errors
	 * @throws IOException if the required XML document isn't found
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			reader.loadBeanDefinitions(configResources);
		}
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			/**
			 * 详见org.springframework.beans.factory.support.AbstractBeanDefinitionReader#loadBeanDefinitions(java.lang.String, java.util.Set<org.springframework.core.io.Resource>)方法
			 * 
			 * 
			 */
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * Return an array of Resource objects, referring to the XML bean definition
	 * files that this context should be built with.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide pre-built Resource objects rather than location Strings.
	 * @return an array of Resource objects, or {@code null} if none
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
