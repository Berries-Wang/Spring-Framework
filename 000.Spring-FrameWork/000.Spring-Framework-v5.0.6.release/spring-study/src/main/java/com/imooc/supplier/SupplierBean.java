package com.imooc.supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 *
 *
 *
 *
 *
 */
public class SupplierBean {
	public static void main(String[] args) {
		ApplicationContext application = new AnnotationConfigApplicationContext();
		BeanDefinition beanDefinition = new GenericBeanDefinition();
		((GenericBeanDefinition) beanDefinition).setBeanClass(User.class);
		/**
		 * 根据其set方法的解释：就是替代工厂方法（包含静态工厂）或者构造器创建对象，但是其后面的生命周期回调不影响。
		 *
		 */
		((GenericBeanDefinition) beanDefinition).setInstanceSupplier(SupplierBean::getUser);
		((AnnotationConfigApplicationContext) application).registerBeanDefinition(User.class.getSimpleName(), beanDefinition);
		((AnnotationConfigApplicationContext) application).refresh();
		System.out.println(application.getBean(User.class).getName());

	}

	private static User getUser() {
		User user = new User();
		user.setName("Hello");
		return user;
	}
}
