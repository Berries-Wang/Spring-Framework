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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Defines the algorithm for searching for metadata-associated methods exhaustively
 * including interfaces and parent classes while also dealing with parameterized methods
 * as well as common scenarios encountered with interface and class-based proxies.
 *
 * <p>Typically, but not necessarily, used for finding annotated handler methods.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.2.3
 */
public abstract class MethodIntrospector {

	/**
	 * Select methods on the given target type based on the lookup of associated metadata.
	 * <p>Callers define methods of interest through the {@link MetadataLookup} parameter,
	 * allowing to collect the associated metadata into the result map.
	 * 根据相关联的元数据查找，选择给定目标类型上的方法。
	 *
	 * @param targetType     the target type to search methods on
	 * @param metadataLookup a {@link MetadataLookup} callback to inspect methods of interest,
	 *                       returning non-null metadata to be associated with a given method if there is a match,
	 *                       or {@code null} for no match
	 * @return the selected methods associated with their metadata (in the order of retrieval),
	 * or an empty map in case of no match
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<>();
		Class<?> specificHandlerType = null;

		// 当targetType原始类型(非代理类)
		if (!Proxy.isProxyClass(targetType)) {
			// 获取原始类型
			specificHandlerType = ClassUtils.getUserClass(targetType);
			handlerTypes.add(specificHandlerType);
		}
		// 将targetType类型所实现的接口和超类都放到handlerTypes中
		handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

		// 遍历handlerTypes
		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			// 迭代处理currentHandlerType中的方法
			ReflectionUtils.doWithMethods(currentHandlerType, method -> {
				// 从targetClass中获取与method方法签名相同的方法，因为target可能覆盖了method方法
				Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
				// 执行回调函数
				T result = metadataLookup.inspect(specificMethod);
				// 当回调结果不为空
				if (result != null) {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
						methodMap.put(specificMethod, result);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		// 将处理器方法返回
		return methodMap;
	}

	/**
	 * Select methods on the given target type based on a filter.
	 * <p>Callers define methods of interest through the {@code MethodFilter} parameter.
	 *
	 * @param targetType   the target type to search methods on
	 * @param methodFilter a {@code MethodFilter} to help
	 *                     recognize handler methods of interest
	 * @return the selected methods, or an empty set in case of no match
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		return selectMethods(targetType,
				(MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
	}

	/**
	 * Select an invocable method on the target type: either the given method itself
	 * if actually exposed on the target type, or otherwise a corresponding method
	 * on one of the target type's interfaces or on the target type itself.
	 * <p>Matches on user-declared interfaces will be preferred since they are likely
	 * to contain relevant metadata that corresponds to the method on the target class.
	 * <p>
	 * 在目標類型中選擇一个可以调用的方法：
	 * 1. 该方法就是参数本身
	 * 2. 该方法是接口中的方法
	 * 3. 该方法是目标类上的一个方法
	 *
	 * @param method     the method to check
	 * @param targetType the target type to search methods on
	 *                   (typically an interface-based JDK proxy)
	 * @return a corresponding invocable method on the target type
	 * @throws IllegalStateException if the given method is not invocable on the given
	 *                               target type (typically due to a proxy mismatch)
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				} catch (NoSuchMethodException ex) {
					// Alright, not on this interface then...
				}
			}
			// A final desperate attempt on the proxy class itself...
			return targetType.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
							"but not found in any interface(s) of the exposed proxy type. " +
							"Either pull the method up to an interface or switch to CGLIB " +
							"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * A callback interface for metadata lookup on a given method.
	 *
	 * @param <T> the type of metadata returned
	 */
	@FunctionalInterface
	public interface MetadataLookup<T> {

		/**
		 * Perform a lookup on the given method and return associated metadata, if any.
		 *
		 * @param method the method to inspect
		 * @return non-null metadata to be associated with a method if there is a match,
		 * or {@code null} for no match
		 */
		@Nullable
		T inspect(Method method);
	}

}
