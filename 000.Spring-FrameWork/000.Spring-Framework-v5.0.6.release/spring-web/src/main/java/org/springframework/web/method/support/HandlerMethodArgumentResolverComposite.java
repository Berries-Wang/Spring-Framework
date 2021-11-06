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

package org.springframework.web.method.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Resolves method parameters by delegating to a list of registered {@link HandlerMethodArgumentResolver}s.
 * Previously resolved method parameters are cached for faster lookups.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<HandlerMethodArgumentResolver> argumentResolvers = new LinkedList<>();


	/*
	 * 缓存，用于获取解析参数
	 */
	private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<>(256);


	/**
	 * Add the given {@link HandlerMethodArgumentResolver}.
	 */
	public HandlerMethodArgumentResolverComposite addResolver(HandlerMethodArgumentResolver resolver) {
		this.argumentResolvers.add(resolver);
		return this;
	}

	/**
	 * Add the given {@link HandlerMethodArgumentResolver}s.
	 *
	 * @since 4.3
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(@Nullable HandlerMethodArgumentResolver... resolvers) {
		if (resolvers != null) {
			for (HandlerMethodArgumentResolver resolver : resolvers) {
				this.argumentResolvers.add(resolver);
			}
		}
		return this;
	}

	/**
	 * Add the given {@link HandlerMethodArgumentResolver}s.
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(
			@Nullable List<? extends HandlerMethodArgumentResolver> resolvers) {

		if (resolvers != null) {
			for (HandlerMethodArgumentResolver resolver : resolvers) {
				this.argumentResolvers.add(resolver);
			}
		}
		return this;
	}

	/**
	 * Return a read-only list with the contained resolvers, or an empty list.
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.argumentResolvers);
	}

	/**
	 * Clear the list of configured resolvers.
	 *
	 * @since 4.3
	 */
	public void clear() {
		this.argumentResolvers.clear();
	}


	/**
	 * Whether the given {@linkplain MethodParameter method parameter} is supported by any registered
	 * {@link HandlerMethodArgumentResolver}.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (getArgumentResolver(parameter) != null);
	}

	/**
	 * Iterate over registered {@link HandlerMethodArgumentResolver}s and invoke the one that supports it.
	 *
	 * @throws IllegalStateException if no suitable {@link HandlerMethodArgumentResolver} is found.
	 * @description: `解析处理器方法的参数(单个)`
	 * @param: parameter 处理器方法中的一个参数
	 * @param: mavContainer 处理结果的容器
	 * @param: webRequest 请求
	 * @param: binderFactory 参数绑定工厂
	 * @Return 'java.lang.Object' 返回值是这个参数的值
	 * @By Wei.Wang
	 * @date 2021/2/28 上午10:33
	 */
	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		// 获取参数解析器
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		// 如果参数解析器为null，则抛出异常
		if (resolver == null) {
			throw new IllegalArgumentException("Unknown parameter type [" + parameter.getParameterType().getName() + "]");
		}
		// 返回解析后的参数,在这个参数解析的时候，就会使用到消息转换器
		return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
	}

	/**
	 * Find a registered {@link HandlerMethodArgumentResolver} that supports the given method parameter.
	 * 获取参数解析器
	 */
	@Nullable
	private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		// 首先从缓存中获取参数解析器
		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		// 如果为null,则寻找参数解析器
		if (result == null) {
			for (HandlerMethodArgumentResolver methodArgumentResolver : this.argumentResolvers) {
				// 日志打印
				if (logger.isTraceEnabled()) {
					logger.trace("Testing if argument resolver [" + methodArgumentResolver + "] supports [" +
							parameter.getGenericParameterType() + "]");
				}
				// 若当前的参数解析器支持，则将其添加到缓存中，并返回. 某一个方法参数解析器支持，则立即返回
				if (methodArgumentResolver.supportsParameter(parameter)) {
					result = methodArgumentResolver;
					// 获取到了对应
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

}
