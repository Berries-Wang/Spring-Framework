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

package org.springframework.core.env;

/**
 * <ul>
 *     <li>Capable: adj.有能力的；有本领的，能干的；可以……的，容许……的. 这个单词表明该接口具有环境能力。</li>
 * </ul>
 * Interface indicating(v.表明，要求；暗示；指示；标示；写明；简要陈述；显示（量度）；打行车转向信号；有必要（indicate 的现在分词形式）) a component
 * that contains and exposes an {@link Environment} reference.
 * <p>公开包含和公开Environment组件引用的接口</p>
 *
 * <p>All Spring application contexts are EnvironmentCapable, and the interface is used primarily
 * for performing {@code instanceof} checks in framework methods that accept BeanFactory
 * instances that may or may not actually be ApplicationContext instances in order to interact
 * with the environment if indeed it is available.
 * <p>所有Spring应用上下文都实现了EnvironmentCapable接口，该接口主要用于在接受BeanFactory实例的框架方法中执行instanceof检查，
 * 这些实例可能是也可能不是ApplicationContext实例，以便与环境交互(如果环境确实可用的话)。</p>
 *
 * <p>As mentioned（v.提及，说起，谈到（mention 的过去式和过去分词））, {@link org.springframework.context.ApplicationContext ApplicationContext}
 * extends EnvironmentCapable, and thus exposes a {@link #getEnvironment()} method; however,
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * redefines {@link org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * getEnvironment()} and narrows(v.	使窄小; 变窄; 缩小;) the signature to return a {@link ConfigurableEnvironment}.
 * The effect is that an Environment object is 'read-only' until it is being accessed from
 * a ConfigurableApplicationContext, at which point it too may be configured.
 * <p>如前所述,ApplicationContext继承了EnvironmentCapable并公开了getEnvironment()方法,然而，ConfigurableApplicationContext重新定义了getEnvironment()方法，
 * 且修改了方法签名使其返回ConfigurableEnvironment，其效果是，在从ConfigurableApplicationContext访问环境对象之前，环境对象是“只读”的，此时也可以对其进行配置。
 * </p>
 * <p>即该接口返回的环境对象是只读的，而从ConfigurableApplicationContext修改后的接口返回的环境对象可以被配置.</p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment
 * @see ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface EnvironmentCapable {

	/**
	 * Return the {@link Environment} associated with this component.
	 * 获取Spring的启动参数
	 */
	Environment getEnvironment();

}
