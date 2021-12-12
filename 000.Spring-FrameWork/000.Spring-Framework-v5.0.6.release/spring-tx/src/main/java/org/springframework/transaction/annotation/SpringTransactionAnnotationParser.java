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

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy implementation for parsing Spring's {@link Transactional} annotation.
 * <p>
 * 解析Spring事务注释的策略实现。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {


	/**
	 * Description: '对目标元素进行解析'
	 *
	 * @param ae 注解元素，可能是个method，也可能是class
	 * @Return "TransactionAttribute" 解析出来的事务属性
	 * @Author: 'Wei.Wang(Email: ziyang.ww@raycloud.com)'
	 * @Date: 2021/12/12 5:45 下午
	 **/
	@Override
	@Nullable
	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement ae) {
		// 将Transactional注解解析成AnnotationAttributes(LinkedHashMap子类)
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
				ae, Transactional.class, false, false);

		if (attributes != null) {
			// attributes不为null时，对attributes进行解析
			return parseTransactionAnnotation(attributes);
		} else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
		return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
	}

	/**
	 * 从代码： org.springframework.transaction.interceptor.TransactionAspectSupport#invokeWithinTransaction
	 * (该方法里的代码: final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);)可以定位到这里
	 * 从代码中可以看出，解析出来的TransactionAttribute的类型是org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
	 * <p>
	 * 从代码：org.springframework.transaction.interceptor.RuleBasedTransactionAttribute#rollbackOn并结合这个解析的代码，就可以知道当事务遇到指定异常需要回滚的功能是如何实现的了。
	 * 即：
	 * 1. 在这里，即将@Tranactional注解转换为TransactionAttribute的时候，将需要回滚的和不需要回滚的配置均转换为RollbackRuleAttribute
	 * 2. 在org.springframework.transaction.interceptor.RuleBasedTransactionAttribute#rollbackOn中迭代这个list，如果类型是NoRollbackRuleAttribute则表示不需要回滚；若类型是RollbackRuleAttribute则需要回滚
	 */
	protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
		// 解析事务传播行为
		Propagation propagation = attributes.getEnum("propagation");
		rbta.setPropagationBehavior(propagation.value());
		Isolation isolation = attributes.getEnum("isolation");
		rbta.setIsolationLevel(isolation.value());
		rbta.setTimeout(attributes.getNumber("timeout").intValue());
		rbta.setReadOnly(attributes.getBoolean("readOnly"));
		// 从注解org.springframework.transaction.annotation.Transactional可以判断出来，qualifier是代表的是事务管理器
		rbta.setQualifier(attributes.getString("value"));
		ArrayList<RollbackRuleAttribute> rollBackRules = new ArrayList<>();
		Class<?>[] rbf = attributes.getClassArray("rollbackFor");
		for (Class<?> rbRule : rbf) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		String[] rbfc = attributes.getStringArray("rollbackForClassName");
		for (String rbRule : rbfc) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		Class<?>[] nrbf = attributes.getClassArray("noRollbackFor");
		for (Class<?> rbRule : nrbf) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		String[] nrbfc = attributes.getStringArray("noRollbackForClassName");
		for (String rbRule : nrbfc) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(rbRule);
			rollBackRules.add(rule);
		}
		rbta.getRollbackRules().addAll(rollBackRules);
		return rbta;
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringTransactionAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringTransactionAnnotationParser.class.hashCode();
	}

}
