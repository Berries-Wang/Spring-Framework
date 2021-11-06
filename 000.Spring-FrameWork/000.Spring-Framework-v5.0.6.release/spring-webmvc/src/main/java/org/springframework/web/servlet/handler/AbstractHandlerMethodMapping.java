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

package org.springframework.web.servlet.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @param <T> The mapping for a {@link HandlerMethod} containing the conditions
 *            needed to match the handler method to incoming request.
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * Bean name prefix for target beans behind scoped proxies. Used to exclude those
	 * targets from handler method detection, in favor of the corresponding proxies.
	 * <p>We're not checking the autowire-candidate status here, which is how the
	 * proxy target filtering problem is being handled at the autowiring level,
	 * since autowire-candidate may have been turned to {@code false} for other
	 * reasons, while still expecting the bean to be eligible for handler methods.
	 * <p>Originally defined in {@link org.springframework.aop.scope.ScopedProxyUtils}
	 * but duplicated here to avoid a hard dependency on the spring-aop module.
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));

	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOrigin("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}


	private boolean detectHandlerMethodsInAncestorContexts = false;

	/**
	 * 名字策略，即用于生成@RequestMapping注解的name属性的值
	 * 文档链接: https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc 在页面搜索HandlerMethodMappingNamingStrategy
	 * <p>
	 * 原文: Here is how this works. On startup, every @RequestMapping is assigned a default name through
	 * HandlerMethodMappingNamingStrategy, whose default implementation uses the capital letters of the class
	 * and the method name (for example, the getThing method in ThingController becomes "TC#getThing").
	 * If there is a name clash, you can use @RequestMapping(name="..")
	 * to assign an explicit name or implement your own HandlerMethodMappingNamingStrategy.
	 */
	@Nullable
	private HandlerMethodMappingNamingStrategy<T> namingStrategy;

	/**
	 * 一个注册表，用于维护到处理程序方法的所有映射，公开用于执行查找的方法并提供并发访问。
	 */
	private final MappingRegistry mappingRegistry = new MappingRegistry();


	/**
	 * Whether to detect handler methods in beans in ancestor ApplicationContexts.
	 * <p>Default is "false": Only beans in the current ApplicationContext are
	 * considered, i.e. only in the context that this HandlerMapping itself
	 * is defined in (typically the current DispatcherServlet's context).
	 * <p>Switch this flag on to detect handler beans in ancestor contexts
	 * (typically the Spring root WebApplicationContext) as well.
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 * Configure the naming strategy to use for assigning a default name to every
	 * mapped handler method.
	 * <p>The default naming strategy is based on the capital letters of the
	 * class name followed by "#" and then the method name, e.g. "TC#getFoo"
	 * for a class named TestController with method getFoo.
	 */
	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * Return the configured naming strategy or {@code null}.
	 */
	@Nullable
	public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
		return this.namingStrategy;
	}

	/**
	 * Return a (read-only) map with all mappings and HandlerMethod's.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.mappingRegistry.acquireReadLock();
		try {
			return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
		} finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Return the handler methods for the given mapping name.
	 *
	 * @param mappingName the mapping name
	 * @return a list of matching HandlerMethod's or {@code null}; the returned
	 * list will never be modified and is safe to iterate.
	 * @see #setHandlerMethodMappingNamingStrategy
	 */
	@Nullable
	public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
	}

	/**
	 * Return the internal mapping registry. Provided for testing purposes.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * Register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 *
	 * @param mapping the mapping for the handler method
	 * @param handler the handler
	 * @param method  the method
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * Un-register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 *
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(T mapping) {
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * Detects(检测) handler methods at initialization.
	 * 该类型实现了org.springframework.beans.factory.InitializingBean接口，会在Bean实例化的时候调用afterPropertiesSet方法来检测handler methods
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();
	}

	/**
	 * Scan beans in the ApplicationContext, detect and register handler methods.
	 * <p>
	 * 扫描ApplicationContext中的Bean，检测并且注册处理器方法
	 * 那这里的ApplicationContext是RootWebApplicationContext还是ServletWebApplicationContext ???
	 * 答案：
	 * 从link.bosswang.config.StudyDispatcherServletInitializer来看，因为MvcConfig使用了org.springframework.web.servlet.config.annotation.EnableWebMvc
	 * 注解，因此才会实例化类型org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping的Bean，因此此时的ApplicationContext是
	 * ServletWebApplicationContext，但是如果将ServiceConfig也加上@EnableWebMvc注解，则RequestMappingHandlerMapping会被实例化两次(每个ApplicationContext一次)
	 *
	 * @see #isHandler(Class)
	 * @see #getMappingForMethod(Method, Class)
	 * @see #handlerMethodsInitialized(Map)
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		// 从ServletWebApplicationContext及其父容器中获取所有Bean的名字
		String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
				obtainApplicationContext().getBeanNamesForType(Object.class));

		// 遍历所有的Bean
		for (String beanName : beanNames) {
			// Bean名称以"scopedTarget."为前缀的是生命周期相关的,不扫描这类的Bean
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					// 通过Bean名称获取Bean类型
					beanType = obtainApplicationContext().getType(beanName);
				} catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				// 如果Bean类型存在并且是Handler(Bean类型被@Controller注解或者@RequestMapping注解标注)
				if (beanType != null && isHandler(beanType)) {
					// 检测处理器方法，进行method到HandlerMethod的转换，并且构建各种缓存
					detectHandlerMethods(beanName);
				}
			}
		}
		// 空方法，在所有的处理器方法都被检测到的时候
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * Look for handler methods in a handler.
	 * <p>
	 * 从处理器中查找处理器方法
	 *
	 * @param handler the bean name of a handler or a handler instance (handler是处理器的Bean名称或者handler就是一个处理器实例)
	 */
	protected void detectHandlerMethods(final Object handler) {
		// 获取处理器类型
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
			// 获取用户定义的类型(因为可能处理器类型被代理了)
			final Class<?> userType = ClassUtils.getUserClass(handlerType);

			// 从userType中选择处理器方法，即这里的K：處理器方法 , V:RequestMappingInfo
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> {
						try {
							// 从给定的方法上提取RequestMappingInfo信息
							return getMappingForMethod(method, userType);
						} catch (Throwable ex) {
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					});

			if (logger.isDebugEnabled()) {
				logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
			}
			methods.forEach((method, mapping) -> {
				// 从指定类型中选择出可以执行的方法
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				// 注册处理器方法,构建缓存
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}

	/**
	 * Register a handler method and its unique mapping. Invoked at startup for
	 * each detected handler method.
	 * <p>
	 * 注册一个处理器方法和他唯一的映射关系，
	 *
	 * @param handler the bean name of the handler or the handler instance  处理器的Bean名称或者处理器实例
	 * @param method  the method to register 需要注册的方法
	 * @param mapping the mapping conditions associated with the handler method 处理器方法关联的映射参数
	 * @throws IllegalStateException if another method was already registered
	 *                               under the same mapping 如果有其他的方法注册时使用了相同的映射参数，则抛出异常
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * Create the HandlerMethod instance.
	 *
	 * @param handler either a bean name or an actual handler instance
	 * @param method  the target method
	 * @return the created HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName,
					obtainApplicationContext().getAutowireCapableBeanFactory(), method);
		} else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * Extract and return the CORS configuration for the mapping.
	 */
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * Invoked after all handler methods have been detected.
	 *
	 * @param handlerMethods a read-only map with handler methods and mappings.
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	// Handler method lookup

	/**
	 * Look up a handler method for the given request.
	 * 通过request来获取处理器(Controller中的方法)
	 */
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		// 从request中获取请求路径
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		// 日志打印
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method for path " + lookupPath);
		}
		// 使用读锁来锁住MappingRegistry
		this.mappingRegistry.acquireReadLock();
		try {
			// 通过请求路径找到处理器方法
			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
			// 日志打印
			if (logger.isDebugEnabled()) {
				if (handlerMethod != null) {
					logger.debug("Returning handler method [" + handlerMethod + "]");
				} else {
					logger.debug("Did not find handler method for [" + lookupPath + "]");
				}
			}
			/*
			 * 返回HandlerMethod
			 * createWithResolvedBean会判断成员属性bean是否是对象，如果不是对象，则创建一个新的HandlerMethod。
			 */
			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
		} finally {
			// 释放读锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * <p>
	 * 为当前的请求寻找最合适的处理器方法，如果找到了多个，则返回最匹配的那一个
	 *
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request    the current request
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch(Object, String, HttpServletRequest)
	 * @see #handleNoMatch(Set, String, HttpServletRequest)
	 * 通过request来获取“最合适的”处理器
	 * 1. 首先通过URL来从缓存中查找
	 * 2. 如果URL中查询不到，则遍历所有的映射关系去寻找
	 * 3. 找到匹配的了
	 * 4. 对匹配到的RequestMappingInfo进行排序
	 * 5. 获取到第一个，即第一个是最佳匹配
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		List<Match> matches = new ArrayList<>();
		// 通过url从MappingRegistry中获取最符合条件的处理器，因为这里是通过url去获取的，因此可能获取多个。即url相同，请求方式不同。详见图:MappingRegistry_urlLookup2.png
		List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
		if (directPathMatches != null) {
			// 从中找最合适的处理器
			addMatchingMappings(directPathMatches, matches, request);
		}
		// 如果找不到，则遍历所有的映射关系去查找
		if (matches.isEmpty()) {
			// No choice but to go through all mappings...
			addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, request);
		}

		// 找到了，则进一步判断
		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
			// 排序
			matches.sort(comparator);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" + lookupPath + "] : " + matches);
			}
			// 获取最合适的处理器
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				if (CorsUtils.isPreFlightRequest(request)) {
					return PREFLIGHT_AMBIGUOUS_MATCH;
				}
				Match secondBestMatch = matches.get(1);
				// 如果两个处理器是一样的，则抛出异常。
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
							request.getRequestURL() + "': {" + m1 + ", " + m2 + "}");
				}
			}
			// 将lookupPath作为request的attributes的属性放到request中
			handleMatch(bestMatch.mapping, lookupPath, request);
			// 返回处理器方法
			return bestMatch.handlerMethod;
		} else {
			// 没有找到，则返回null
			return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				matches.add(new Match(match, this.mappingRegistry.getMappings().get(mapping)));
			}
		}
	}

	/**
	 * Invoked when a matching mapping is found.
	 *
	 * @param mapping    the matching mapping
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request    the current request
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * Invoked when no matching mapping is not found.
	 *
	 * @param mappings   all registered mappings
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request    the current request
	 * @throws ServletException in case of errors
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
			} else {
				CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
				corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
			}
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * Whether the given type is a handler with handler methods.
	 *
	 * @param beanType the type of the bean being checked
	 * @return "true" if this a handler type, "false" otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * Provide the mapping for a handler method. A method for which no
	 * mapping can be provided is not a handler method.
	 *
	 * @param method      the method to provide a mapping for
	 * @param handlerType the handler type, possibly a sub-type of the method's
	 *                    declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Extract and return the URL paths contained in a mapping.
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	/**
	 * Check if a mapping matches the current request and return a (potentially
	 * new) mapping with conditions relevant to the current request.
	 *
	 * @param mapping the mapping to get a match for
	 * @param request the current HTTP servlet request
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * Return a comparator for sorting matching mappings.
	 * The returned comparator should sort 'better' matches higher.
	 *
	 * @param request the current request
	 * @return the comparator (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);


	/**
	 * A registry that maintains（vt. 维持；继续；维修；主张；） all mappings to handler methods, exposing methods
	 * to perform lookups and providing concurrent access.
	 * <p>
	 * 一个注册表，用于维护到处理程序方法的所有映射，公开用于执行查找的方法并提供并发访问。
	 * <p>Package-private for testing purposes.
	 */
	class MappingRegistry {

		/**
		 * org.springframework.web.servlet.mvc.method.RequestMappingInfo到MappingRegistration的映射关系
		 */
		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();
		/**
		 * org.springframework.web.servlet.mvc.method.RequestMappingInfo到HandlerMethod的映射关系
		 */
		private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();
		/**
		 * 采用这种方法反序列化的时候，如果json字符串中有相同的key，存的时候值会以数组的方式保存,即T是一个链表。见图片MappingRegistry_urlLookup.png
		 * <p>
		 * 请求URL到org.springframework.web.servlet.mvc.method.RequestMappingInfo的映射关系
		 **/
		private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<>();
		/**
		 * 维护RequestMapping的名字（@RequestMapping注解的name属性）到HandlerMethod的映射关系缓存
		 * 即RequestMapping : HandlerMethod = 1:N
		 */
		private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * Return all mappings and handler methods. Not thread-safe.
		 *
		 * @see #acquireReadLock()
		 */
		public Map<T, HandlerMethod> getMappings() {
			return this.mappingLookup;
		}

		/**
		 * Return matches for the given URL path. Not thread-safe.
		 *
		 * @see #acquireReadLock()
		 */
		@Nullable
		public List<T> getMappingsByUrl(String urlPath) {
			return this.urlLookup.get(urlPath);
		}

		/**
		 * Return handler methods by mapping name. Thread-safe for concurrent use.
		 */
		public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
			return this.nameLookup.get(mappingName);
		}

		/**
		 * Return CORS configuration. Thread-safe for concurrent use.
		 */
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
		 * Acquire the read lock when using getMappings and getMappingsByUrl.
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * Release the read lock after using getMappings and getMappingsByUrl.
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		/**
		 * @description: `注册处理器方法机器映射关系`
		 * @param: mapping   处理器方法关联的映射参数,类型为org.springframework.web.servlet.mvc.method.RequestMappingInfo
		 * @param: handler 处理器的Bean名称或者处理器实例
		 * @param: method 需要注册的方法
		 * @Return 'void'
		 * @By Wei.Wang
		 * @date 2021/2/27 下午6:30
		 */
		public void register(T mapping, Object handler, Method method) {
			// 获取读写锁
			this.readWriteLock.writeLock().lock();
			try {
				// 将处理器和处理器方法转换为HandlerMethod
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				// 断言请求路径是唯一的，不唯一会抛出异常
				assertUniqueMethodMapping(handlerMethod, mapping);

				if (logger.isInfoEnabled()) {
					logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
				}
				// 维护请求路径到HandlerMethod的缓存
				this.mappingLookup.put(mapping, handlerMethod);
				// 獲取org.springframework.web.servlet.mvc.method.RequestMappingInfo中完整的URL
				List<String> directUrls = getDirectUrls(mapping);
				for (String url : directUrls) {
					// 建立请求URL到org.springframework.web.servlet.mvc.method.RequestMappingInfo的映射关系
					this.urlLookup.add(url, mapping);
				}

				String name = null;
				if (getNamingStrategy() != null) {
					name = getNamingStrategy().getName(handlerMethod, mapping);
					// 建立@RequestMapping的名字与HandlerMethod的映射关系
					addMappingName(name, handlerMethod);
				}

				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					this.corsLookup.put(handlerMethod, corsConfig);
				}
				// 建立rg.springframework.web.servlet.mvc.method.RequestMappingInfo到MappingRegistration的映射关系的缓存
				this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod, directUrls, name));
			} finally {
				// 释放锁
				this.readWriteLock.writeLock().unlock();
			}
		}

		/**
		 * @description: `判断映射路径是否唯一`
		 * @param: newHandlerMethod 根据处理器和处理器方法转换的HandlerMethod
		 * @param: mapping 处理器方法对应的请求路径
		 * @Return 'void'
		 * @By Wei.Wang
		 * @date 2021/2/27 下午6:33
		 */
		private void assertUniqueMethodMapping(HandlerMethod newHandlerMethod, T mapping) {
			HandlerMethod handlerMethod = this.mappingLookup.get(mapping);
			if (handlerMethod != null && !handlerMethod.equals(newHandlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + newHandlerMethod.getBean() + "' method \n" +
								newHandlerMethod + "\nto " + mapping + ": There is already '" +
								handlerMethod.getBean() + "' bean method\n" + handlerMethod + " mapped.");
			}
		}

		private List<String> getDirectUrls(T mapping) {
			List<String> urls = new ArrayList<>(1);
			for (String path : getMappingPathPatterns(mapping)) {
				if (!getPathMatcher().isPattern(path)) {
					urls.add(path);
				}
			}
			return urls;
		}

		/**
		 * @description: `建立@RequestMapping的名字(name属性的值)到HandlerMethod的映射关系的缓存`
		 * @param: name @RequestMapping的名字，可能是用名字策略生成的也可能是用户指定的
		 * @param: handlerMethod HandlerMethod，即处理器信息
		 * @Return 'void'
		 * @By Wei.Wang
		 * @date 2021/2/27 下午6:53
		 */
		private void addMappingName(String name, HandlerMethod handlerMethod) {
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				oldList = Collections.emptyList();
			}

			for (HandlerMethod current : oldList) {
				if (handlerMethod.equals(current)) {
					return;
				}
			}

			if (logger.isTraceEnabled()) {
				logger.trace("Mapping name '" + name + "'");
			}

			List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
			newList.addAll(oldList);
			newList.add(handlerMethod);
			this.nameLookup.put(name, newList);

			if (newList.size() > 1) {
				if (logger.isTraceEnabled()) {
					logger.trace("Mapping name clash for handlerMethods " + newList +
							". Consider assigning explicit names.");
				}
			}
		}

		public void unregister(T mapping) {
			this.readWriteLock.writeLock().lock();
			try {
				MappingRegistration<T> definition = this.registry.remove(mapping);
				if (definition == null) {
					return;
				}

				this.mappingLookup.remove(definition.getMapping());

				for (String url : definition.getDirectUrls()) {
					List<T> list = this.urlLookup.get(url);
					if (list != null) {
						list.remove(definition.getMapping());
						if (list.isEmpty()) {
							this.urlLookup.remove(url);
						}
					}
				}

				removeMappingName(definition);

				this.corsLookup.remove(definition.getHandlerMethod());
			} finally {
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void removeMappingName(MappingRegistration<T> definition) {
			String name = definition.getMappingName();
			if (name == null) {
				return;
			}
			HandlerMethod handlerMethod = definition.getHandlerMethod();
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				return;
			}
			if (oldList.size() <= 1) {
				this.nameLookup.remove(name);
				return;
			}
			List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
			for (HandlerMethod current : oldList) {
				if (!current.equals(handlerMethod)) {
					newList.add(current);
				}
			}
			this.nameLookup.put(name, newList);
		}
	}


	private static class MappingRegistration<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private final List<String> directUrls;

		@Nullable
		private final String mappingName;

		public MappingRegistration(T mapping, HandlerMethod handlerMethod,
								   @Nullable List<String> directUrls, @Nullable String mappingName) {

			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directUrls = (directUrls != null ? directUrls : Collections.emptyList());
			this.mappingName = mappingName;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public List<String> getDirectUrls() {
			return this.directUrls;
		}

		@Nullable
		public String getMappingName() {
			return this.mappingName;
		}
	}


	/**
	 * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
	 * comparing the best match with a comparator in the context of the current request.
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private static class EmptyHandler {

		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
