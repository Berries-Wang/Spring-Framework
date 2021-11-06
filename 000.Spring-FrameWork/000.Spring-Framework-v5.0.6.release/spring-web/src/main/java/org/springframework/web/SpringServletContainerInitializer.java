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

package org.springframework.web;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Servlet 3.0 {@link ServletContainerInitializer} designed to support code-based
 * configuration of the servlet container using Spring's {@link WebApplicationInitializer}
 * SPI as opposed to (or possibly in combination with) the traditional
 * {@code web.xml}-based approach.
 *
 * <h2>Mechanism of Operation</h2>
 * This class will be loaded and instantiated and have its {@link #onStartup}
 * method invoked by any Servlet 3.0-compliant container during container startup assuming
 * that the {@code spring-web} module JAR is present on the classpath. This occurs through
 * the JAR Services API {@link ServiceLoader#load(Class)} method detecting the
 * {@code spring-web} module's {@code META-INF/services/javax.servlet.ServletContainerInitializer}
 * service provider configuration file. See the
 * <a href="http://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR Services API documentation</a> as well as section <em>8.2.4</em> of the Servlet 3.0
 * Final Draft specification for complete details.
 *
 * <h3>In combination with {@code web.xml}</h3>
 * A web application can choose to limit the amount of classpath scanning the Servlet
 * container does at startup either through the {@code metadata-complete} attribute in
 * {@code web.xml}, which controls scanning for Servlet annotations or through an
 * {@code <absolute-ordering>} element also in {@code web.xml}, which controls which
 * web fragments (i.e. jars) are allowed to perform a {@code ServletContainerInitializer}
 * scan. When using this feature, the {@link SpringServletContainerInitializer}
 * can be enabled by adding "spring_web" to the list of named web fragments in
 * {@code web.xml} as follows:
 *
 * <pre class="code">
 * {@code
 * <absolute-ordering>
 *   <name>some_web_fragment</name>
 *   <name>spring_web</name>
 * </absolute-ordering>
 * }</pre>
 *
 * <h2>Relationship to Spring's {@code WebApplicationInitializer}</h2>
 * Spring's {@code WebApplicationInitializer} SPI consists of just one method:
 * {@link WebApplicationInitializer#onStartup(ServletContext)}. The signature is intentionally
 * quite similar to {@link ServletContainerInitializer#onStartup(Set, ServletContext)}:
 * simply put, {@code SpringServletContainerInitializer} is responsible for instantiating
 * and delegating the {@code ServletContext} to any user-defined
 * {@code WebApplicationInitializer} implementations. It is then the responsibility of
 * each {@code WebApplicationInitializer} to do the actual work of initializing the
 * {@code ServletContext}. The exact process of delegation is described in detail in the
 * {@link #onStartup onStartup} documentation below.
 *
 * <h2>General Notes</h2>
 * In general, this class should be viewed as <em>supporting infrastructure</em> for
 * the more important and user-facing {@code WebApplicationInitializer} SPI. Taking
 * advantage of this container initializer is also completely <em>optional</em>: while
 * it is true that this initializer will be loaded and invoked under all Servlet 3.0+
 * runtimes, it remains the user's choice whether to make any
 * {@code WebApplicationInitializer} implementations available on the classpath. If no
 * {@code WebApplicationInitializer} types are detected, this container initializer will
 * have no effect.
 *
 * <p>Note that use of this container initializer and of {@code WebApplicationInitializer}
 * is not in any way "tied" to Spring MVC other than the fact that the types are shipped
 * in the {@code spring-web} module JAR. Rather, they can be considered general-purpose
 * in their ability to facilitate convenient code-based configuration of the
 * {@code ServletContext}. In other words, any servlet, listener, or filter may be
 * registered within a {@code WebApplicationInitializer}, not just Spring MVC-specific
 * components.
 *
 * <p>This class is neither designed for extension nor intended to be extended.
 * It should be considered an internal type, with {@code WebApplicationInitializer}
 * being the public-facing SPI.
 *
 * <h2>See Also</h2>
 * See {@link WebApplicationInitializer} Javadoc for examples and detailed usage
 * recommendations.<p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see #onStartup(Set, ServletContext)
 * @see WebApplicationInitializer
 * @since 3.1
 */

/**
 * @HandlesTypes 注解的作用： 将注解指定的Class对象作为参数传递到onStartup（ServletContainerInitializer）方法中
 * <p>
 * 为什么实现javax.servlet.ServletContainerInitializer接口： spring-note/023.SpringMVC源码学习.md
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * Delegate the {@code ServletContext} to any {@link WebApplicationInitializer}
	 * implementations present on the application classpath.（将ServletContext委托给应用程序类路径上的任何WebApplicationInitializer实现。）
	 *
	 * <p>Because this class declares @{@code HandlesTypes(WebApplicationInitializer.class)},
	 * Servlet 3.0+ containers will automatically scan the classpath for implementations
	 * of Spring's {@code WebApplicationInitializer} interface and provide the set of all
	 * such types to the {@code webAppInitializerClasses} parameter of this method.
	 * <p>
	 *     =========>>>>>>>>>
	 * 因为这个类声明了@HandlesTypes(WebApplicationInitializer.class)，所以Servlet3.0+的容器将自动扫描类路径上的Spring中的WebApplicationInitializer实现
	 * 并且提供所有的WebApplicationInitializer实现的set集合作为这个方法的参数
	 *
	 *   <<<<<<<<<-===========
	 * <p>If no {@code WebApplicationInitializer} implementations are found on the classpath,
	 * this method is effectively a no-op. An INFO-level log message will be issued（v. 发布；（正式）发给；将……诉诸法律；出版；发行（新的一批）；流出；由……产生（issue 的过去式及过去分词））
	 * notifying the user that the {@code ServletContainerInitializer} has indeed been invoked but that
	 * no {@code WebApplicationInitializer} implementations were found.
	 * 如果在类路径上没有WebApplicationInitializer的实现，则这个方法实际上是无效的，一个info级别的日志将会被打印出来去提醒用户
	 * ServletContainerInitializer需要被调用但是没有WebApplicationInitializer的实现
	 *
	 * <p>Assuming(vi. 假定；设想；承担；采取（assume的现在分词）) that one or more
	 * {@code WebApplicationInitializer} types are detected(v. （尤指用特殊方法）发现，识别),
	 * they will be instantiated (and <em>sorted</em> if the @{@link
	 * org.springframework.core.annotation.Order @Order} annotation is present or
	 * the {@link org.springframework.core.Ordered Ordered} interface has been
	 * implemented). Then the {@link WebApplicationInitializer#onStartup(ServletContext)}
	 * method will be invoked on each instance, delegating the {@code ServletContext} such
	 * that each instance may register and configure servlets such as Spring's
	 * {@code DispatcherServlet}, listeners such as Spring's {@code ContextLoaderListener},
	 * or any other Servlet API componentry（n.元件部分） such as filters.
	 * <p>
	 * 假设有一个或者多个WebApplicationInitializer的实现者被检测到了，如果@Order注解存在或者实现了Ordered接口，那么将先排序再进行实例化。
	 * 然后逐个调用WebApplicationInitializer实现者的onStartUp方法，每一个实例或许注册和配置servlets（例如Spring中的DispatcherServlet）,
	 * 监听器（例如Spring中的ContextLoaderListener）或者任何的其他Servlet 功能部件,例如filter
	 *
	 * @param webAppInitializerClasses all implementations of
	 *                                 {@link WebApplicationInitializer} found on the application classpath
	 * @param servletContext           the servlet context to be initialized
	 * @see WebApplicationInitializer#onStartup(ServletContext)
	 * @see AnnotationAwareOrderComparator
	 */
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		// 声明容器，存放需要调用onStartUp方法的WebApplicationInitializer的实现者
		List<WebApplicationInitializer> initializers = new LinkedList<>();

		// 当类路径中存在WebApplicationInitializer（注意，是Class类型的对象）
		if (webAppInitializerClasses != null) {
			// 遍历所有的WebApplicationInitializer的实现者
			for (Class<?> waiClass : webAppInitializerClasses) {
				// Be defensive（adj. 自卫的；防御用的 n. 防御；守势）: Some servlet containers provide us with invalid classes,
				// no matter （no matter 不管）what @HandlesTypes says...
				//类不能是接口，不能是抽象类并且类是继承自WebApplicationInitializer
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						// 将符合条件的类放到容器中
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					} catch (Throwable ex) {
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		// 当类路径下没有WebApplicationInitializer的实现者，则先打印日志后直接返回
		if (initializers.isEmpty()) {
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
		// 对WebApplicationInitializer的实现者进行排序
		AnnotationAwareOrderComparator.sort(initializers);
		// 遍历WebApplicationInitializer实现者的集合，逐个调用他们的onStartUp方法
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
