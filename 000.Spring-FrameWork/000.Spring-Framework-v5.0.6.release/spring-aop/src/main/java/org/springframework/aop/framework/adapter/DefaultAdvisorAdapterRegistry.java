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

package org.springframework.aop.framework.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}


	/**
	 * 适配器模式，将拦截器转换为Advisor
	 *
	 * @param adviceObject
	 * @return
	 * @throws UnknownAdviceTypeException
	 */
	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;
		// =====>>>  org.aopalliance.intercept.Interceptor 继承自Advice
		if (advice instanceof MethodInterceptor) {
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}
		/**
		 * 适配器模式表现
		 * 迭代所有的适配器，若适配器能够支持当前的Advice,则将Advice转换为Advisor
		 */
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	/**
	 * 返回AOP Alliance MethodInterceptors数组，以允许在基于拦截的框架中使用给定的Advisor
	 *
	 * @param advisor Advisor to find an interceptor for
	 * @return
	 * @throws UnknownAdviceTypeException
	 */
	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		Advice advice = advisor.getAdvice();
		// 如果当前的advice的类型是MethodInterceptor
		if (advice instanceof MethodInterceptor) {
			// 直接添加到列表中
			interceptors.add((MethodInterceptor) advice);
		}
		// 遍历所有的适配器(适配器模式的应用)
		for (AdvisorAdapter adapter : this.adapters) {
			// 如果当前的适配器能够支持这种类型的Advice
			if (adapter.supportsAdvice(advice)) {
				// 那就使用适配器，将Advisor转换为MethodInterceptor
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		// 转换为数组返回
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
