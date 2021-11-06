/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * kicks in: 踢开，开始生效
 * BeanFactoryPostProcessor detection(侦查，探测，发觉) kicks in. In particular,
 * particular: 特别的，详细的，独有的
 * Extension to the standard {@link BeanFactoryPostProcessor} SPI, allowing for
 * the registration(登记，注册) of further(更远的，更多的) bean definitions <i>before</i> regular(定期的，有规则的)
 * BeanFactoryPostProcessor detection(侦查，探测，发觉) kicks in. In particular,
 * BeanDefinitionRegistryPostProcessor may register further bean definitions
 * which in turn define BeanFactoryPostProcessor instances.
 *
 * 拓展到标准的BeanFactoryPostProcessor SPI，允许在常规的BeanFactoryPostProcessor探测开始之前定义更多的BeanDefinition，
 * 特别是BeanDefinitionRegistryPostProcessor允许注册更多的BeanDefinition，这些定义也反过来又定义了BeanFactoryPostProcessor实例
 * 
 * @author Juergen Hoeller
 * @since 3.0.1
 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean definition registry after its
	 * standard initialization. All regular bean definitions will have been loaded,
	 * but no beans will have been instantiated yet. This allows for adding further
	 * bean definitions before the next post-processing phase kicks in.
	 * @param registry the bean definition registry used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
