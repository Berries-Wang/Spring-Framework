/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Annotation for a method invocation that is a recovery handler. A suitable(adj. 适宜的，合适的) recovery
 * handler has a first parameter of type Throwable (or a subtype of Throwable) and a
 * return value of the same type as the <code>@Retryable</code> method to recover from.
 * The Throwable first argument is optional (but a method without it will only be called
 * if no others match). Subsequent(adj. 随后的，接着的；) arguments are populated from the argument list of the
 * failed method in order.
 * <p>
 * 被该注解标注的方法是一个recovery Handler,一个标准的recovery handler第一个参数类型是Throwable 或者是其子类
 * 并且返回值要和@Retryable标准的方法一致。第一个Throwable参数是可选的(但是没有它的方法只有在其他方法不匹配时才会被调用)，
 * 随后的参数会根据调用失败的方法参数列表按顺序填充
 *
 * @author Dave Syer
 * @since 2.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(RetryConfiguration.class)
@Documented
public @interface Recover {
}
