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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.DeclareParentsAdvisor;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

/**
 * Factory that can create Spring AOP Advisors given AspectJ classes from
 * classes honoring the AspectJ 5 annotation syntax, using reflection to
 * invoke the corresponding advice methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Phillip Webb
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

	private static final Comparator<Method> METHOD_COMPARATOR;

	static {
		Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
				new InstanceComparator<>(
						Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
				(Converter<Method, Annotation>) method -> {
					AspectJAnnotation<?> annotation =
							AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
					return (annotation != null ? annotation.getAnnotation() : null);
				});
		Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
		METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
	}


	@Nullable
	private final BeanFactory beanFactory;


	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}.
	 */
	public ReflectiveAspectJAdvisorFactory() {
		this(null);
	}

	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}, propagating the given
	 * {@link BeanFactory} to the created {@link AspectJExpressionPointcut} instances,
	 * for bean pointcut handling as well as consistent {@link ClassLoader} resolution.
	 *
	 * @param beanFactory the BeanFactory to propagate (may be {@code null}}
	 * @see AspectJExpressionPointcut#setBeanFactory
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
	 * @since 4.3.6
	 */
	public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * 为所有被At-AspectJ注解标注的方法(指定的aspect实例)构建Spring AOP Advisors
	 *
	 * @param aspectInstanceFactory the aspect instance factory
	 *                              (not the aspect instance itself in order to avoid eager instantiation)
	 * @return
	 */
	@Override
	public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
		// 从AspectMetadata从获取切面类,如com.imooc.aspect.AspectStu
		Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		// 获取切面类名称，如 apectStu
		String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
		// 对切面类进行校验
		validate(aspectClass);

		// We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
		// so that it will only instantiate once.
		// 装饰者，为了懒加载，初始化一次
		MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
				new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

		List<Advisor> advisors = new LinkedList<>();
		// 遍历切面类aspectClass中所有的方法(除了被@PointCut注解标注的方法)
		for (Method method : getAdvisorMethods(aspectClass)) {
			// 将切面类(被AspectJ注解标注的，@PointCut注解除外)中的通知转换为Advisor
			Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
			// 当切面类中的方法没有被AspectJ注解标注或者被@PointCut注解标注的时候，返回null
			if (advisor != null) {
				advisors.add(advisor);
			}
		}

		// If it's a per target aspect, emit(发出) the dummy(假的，仿真的) instantiating aspect.
		// 如果寻找的增强器不为空而且又配置了增强延迟初始化，那么需要在首位加入同步实例化增强器????
		if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
			advisors.add(0, instantiationAdvisor);
		}

		// Find introduction(n. 介绍；引进；采用；入门；传入) fields.
		// 遍历所有的field，判断是否被@DeclareParents注解标注
		for (Field field : aspectClass.getDeclaredFields()) {
			Advisor advisor = getDeclareParentsAdvisor(field);
			if (advisor != null) {
				advisors.add(advisor);
			}
		}
		// 返回所有的advisor
		return advisors;
	}

	/**
	 * 获取切面类中所有的方法，住了被@PointCut注解标注的方法
	 *
	 * @param aspectClass 切面类
	 * @return
	 */
	private List<Method> getAdvisorMethods(Class<?> aspectClass) {
		final List<Method> methods = new LinkedList<>();
		ReflectionUtils.doWithMethods(aspectClass, method -> {
			// Exclude pointcuts,即获取切面类里所有的方法，排除掉被PointCut注解标注的方法
			if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
				methods.add(method);
			}
		});
		methods.sort(METHOD_COMPARATOR);
		return methods;
	}

	/**
	 * Build a {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}
	 * for the given introduction field.
	 * <p>Resulting Advisors will need to be evaluated for targets.
	 *
	 * @param introductionField the field to introspect
	 * @return the Advisor instance, or {@code null} if not an Advisor
	 */
	@Nullable
	private Advisor getDeclareParentsAdvisor(Field introductionField) {
		DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
		if (declareParents == null) {
			// Not an introduction field
			return null;
		}

		if (DeclareParents.class == declareParents.defaultImpl()) {
			throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
		}

		return new DeclareParentsAdvisor(
				introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
	}


	/**
	 * @param candidateAdviceMethod    the candidate(候选人，候补者；应试者) advice method 切面类的方法，不一定是切面方法(即不一定被注解标注)
	 * @param aspectInstanceFactory    the aspect instance factory
	 * @param declarationOrderInAspect 切面类中切面方法的序号
	 * @param aspectName               the name of the aspect 切面类名称
	 * @return
	 */
	@Override
	@Nullable
	public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
							  int declarationOrderInAspect, String aspectName) {

		// 校验一下切面类
		validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

		// 根据方法来获取切点表达式
		AspectJExpressionPointcut expressionPointcut = getPointcut(
				candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
		// 返回为null，则说明这个method没有被AspectJ的注解标注
		if (expressionPointcut == null) {
			return null;
		}

		/**
		 * 创建Advisor
		 *
		 */
		return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
				this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
	}

	/**
	 * 获取切点
	 *
	 * @param candidateAdviceMethod 切面类中的方法
	 * @param candidateAspectClass  切面类
	 * @return
	 */
	@Nullable
	private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
		// 找到方法candidateAdviceMethod中的第一个AspectJ中的注解
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		// 如果没有被AspectJ注解标注，则直接返回null
		if (aspectJAnnotation == null) {
			return null;
		}

		AspectJExpressionPointcut ajexp =
				new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
		//  aspectJAnnotation.getPointcutExpression() 就是注解的value,例如，对于AspectStu来说就是 "embed()"
		ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
		if (this.beanFactory != null) {
			ajexp.setBeanFactory(this.beanFactory);
		}
		return ajexp;
	}


	/**
	 * 获取切面通知
	 *
	 * @param candidateAdviceMethod the candidate advice method
	 * @param expressionPointcut    the AspectJ expression pointcut
	 * @param aspectInstanceFactory the aspect instance factory
	 * @param declarationOrder      the declaration order within the aspect
	 * @param aspectName            the name of the aspect
	 * @return
	 */
	@Override
	@Nullable
	public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
							MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

		// 获取切面类的类型
		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		// 切面类的校验
		validate(candidateAspectClass);

		// 获取方法上的AspectJ的注解
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		// 严谨(判空，即使前面已经判断了)，当方法没有被AspectJ的注解标注，则返回null
		if (aspectJAnnotation == null) {
			return null;
		}

		// If we get here, we know we have an AspectJ method.
		// Check that it's an AspectJ-annotated class 即判断这个切面类是否是spring中的切面类
		if (!isAspect(candidateAspectClass)) {
			throw new AopConfigException("Advice must be declared inside an aspect type: " +
					"Offending method '" + candidateAdviceMethod + "' in class [" +
					candidateAspectClass.getName() + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
		}

		AbstractAspectJAdvice springAdvice;

		// 对获取到的注解进行处理
		switch (aspectJAnnotation.getAnnotationType()) {
			case AtBefore:
				springAdvice = new AspectJMethodBeforeAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfter:
				springAdvice = new AspectJAfterAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfterReturning:
				springAdvice = new AspectJAfterReturningAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
					springAdvice.setReturningName(afterReturningAnnotation.returning());
				}
				break;
			case AtAfterThrowing:
				springAdvice = new AspectJAfterThrowingAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
				}
				break;
			case AtAround:
				springAdvice = new AspectJAroundAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtPointcut:
				if (logger.isDebugEnabled()) {
					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
				}
				return null;
			default:
				throw new UnsupportedOperationException(
						"Unsupported advice type on method: " + candidateAdviceMethod);
		}

		// Now to configure the advice...
		// 设置切面类的名称
		springAdvice.setAspectName(aspectName);
		// 设置上声明的顺序
		springAdvice.setDeclarationOrder(declarationOrder);
		// 获取参数名称列表（AspectJ注解的argNames参数值： 如org.aspectj.lang.annotation.Before.argNames）
		String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
		if (argNames != null) {
			// 设置注解参数值
			springAdvice.setArgumentNamesFromStringArray(argNames);
		}
		// 参数绑定(org.aspectj.lang.annotation.Before.argNames里面的和切面方法的参数进行绑定)，但是为什么?
		springAdvice.calculateArgumentBindings();
		// 返回切面通知
		return springAdvice;
	}


	/**
	 * Synthetic advisor that instantiates the aspect.
	 * Triggered by per-clause pointcut on non-singleton aspect.
	 * The advice has no effect.
	 */
	@SuppressWarnings("serial")
	protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

		public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
			super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
					(method, args, target) -> aif.getAspectInstance());
		}
	}

}
