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

package org.springframework.transaction.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans
 * necessary to enable proxy-based annotation-driven transaction management.
 *
 * @author Chris Beams
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 * @since 3.1
 * <p>
 * <p>
 * <p>
 * 注意：
 * 1. 这里往Spring IOC容器中注入了两个Advisor，这两个Advisor的作用分别是什么?
 */
@Configuration
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

	/**
	 * 这是一个Advisor，在Spring中，所有的切面都会被转换为Advisor
	 * <p>
	 * 这里添加这个Bean，是要告诉Spring在创建Bean的时候需要创建代理对象
	 */
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
		// 关键是这个对象，这是一个Advisor,与其他的Advisor一致，这里是绑定了一个PointCut，通过PointCut去与方法匹配，判断该方法是否需要事务的支持。
		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		//
		advisor.setTransactionAttributeSource(transactionAttributeSource());
		//
		advisor.setAdvice(transactionInterceptor());
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}

	/**
	 * TransactionAttributeSource 是什么呢?  主要是用于判断该方法是被@Transactional注解标注了，是需要进行AOP代理的方法,即主要明白 “什么是TransactionAttribute”
	 * 见： 029.Spring事务实现原理.md
	 * 这个主要是一个工具，用于解析出方法上的@Transactional注解并且转换为TransactionAttribute
	 * <p>
	 * 注入这个Bean，是为了向Spring中注入事务解析器
	 * <p>
	 * 见构造方法:{@link AnnotationTransactionAttributeSource#AnnotationTransactionAttributeSource(boolean)},可以分析一下
	 * {@link org.springframework.transaction.annotation.SpringTransactionAnnotationParser}
	 */
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}

	/**
	 * org.springframework.transaction.interceptor.TransactionInterceptor 实现了 org.aopalliance.intercept.MethodInterceptor接口
	 * 在Spring中，所有的MethodInterceptor也都会被转换为Advisor===》org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry#wrap(java.lang.Object)
	 * <p>
	 * TransactionInterceptor 该种情况下，这个Bean并没有PointCut，因此，他不会应用到任何方法上。
	 * </p>
	 * TransactionInterceptor 仅有该类型的Bean是无法实现事务的，因为他并不知道该为哪个方法所服务，即没有对应的PointCut
	 */
	@Bean // 这里为什么要将该Bean注入到Spring IOC中呢.不注入则不会进行事务回滚,为什么? 因为这里有最为关键的一步骤，即指定事务管理器，没有事务管理器，则事务是不存在的
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor() {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		/* 注意,这里是关键点，这里将TransactionAttributeSource类型的对象设置到TransactionInterceptor，
		 * 即添加org.springframework.transaction.annotation.SpringTransactionAnnotationParser
		 * 用于解析@Transactional注解
		 */
		interceptor.setTransactionAttributeSource(transactionAttributeSource());
		if (this.txManager != null) {
			// ====> 20210502:很重要的一点，即注入事务管理器，没有事务管理器，则事务不存在
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
