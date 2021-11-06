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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Strategy: 战略，策略
 * extended: adj. 延伸的；扩大的；长期的；广大的 v. 延长；扩充（extend 的过去分词）
 * 
 * 
 * Strategy interface for loading resources (e.. class path or file system
 * resources). An {@link org.springframework.context.ApplicationContext}
 * is required to provide this functionality, plus extended
 * {@link org.springframework.core.io.support.ResourcePatternResolver} support.
 *
 * <p>{@link DefaultResourceLoader} is a standalone implementation that is
 * usable outside an ApplicationContext, also used by {@link ResourceEditor}.
 *
 * <p>Bean properties of type Resource and Resource array can be populated
 * from Strings when running in an ApplicationContext, using the particular
 * context's resource loading strategy.
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * 
 * ResourceLoader接口需要被实现，用于返回Resource实例
 */
public interface ResourceLoader {

	/** 
	 * Pseudo: n. 伪君子；假冒的人 adj. 冒充的，假的
	 * Pseudo URL prefix for loading from the class path: "classpath:" */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * Return a Resource handle for the specified resource location.
	 * <p>The handle should always be a reusable resource descriptor,
	 * allowing for multiple {@link Resource#getInputStream()} calls.
	 * <p><ul>
	 * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
	 * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
	 * <li>Should support relative file paths, e.g. "WEB-INF/test.dat".
	 * (This will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)
	 * </ul>
	 * <p>Note that a Resource handle does not imply an existing resource;
	 * you need to invoke {@link Resource#exists} to check for existence.
	 * @param location the resource location
	 * @return a corresponding Resource handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 * 
	 * 根据指定的资源位置来返回一个资源处理器(Resource对象)
	 */
	Resource getResource(String location);

	/**
	 * Expose: vt. 揭露，揭发；使曝光；显示
	 * uniform: adj. 统一的；一致的；相同的；均衡的；始终如一的 n. 制服 vt. 使穿制服；使成一样
	 * manner:  n. 方式；习惯；种类；规矩；风俗
	 * 
	 * Expose the ClassLoader used by this ResourceLoader.
	 * <p>Clients which need to access the ClassLoader directly can do so
	 * in a uniform manner with the ResourceLoader, rather than relying
	 * on the thread context ClassLoader.
	 * @return the ClassLoader
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * 
	 * 公开ResourceLoader所使用的类加载器.
	 * 需要直接访问类加载器的客户端可以通过ResourceLoader以统一的方式进行访问
	 */
	@Nullable
	ClassLoader getClassLoader();

}
