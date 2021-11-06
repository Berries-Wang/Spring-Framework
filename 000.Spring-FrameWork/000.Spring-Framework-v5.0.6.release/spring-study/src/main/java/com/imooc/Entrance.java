package com.imooc;

import com.imooc.controller.WelcomeController;
import com.imooc.services.WelcomeService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class Entrance {

	/**
	 * 通过XMl文件来初始化IOC
	 */
	public static void main(String[] args) {
		String xmlPath = "./spring-study/src/main/resources/spring/Spring-config.xml";

		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(xmlPath);
		WelcomeService welcomeService = (WelcomeService) applicationContext.getBean("welcomeService");
		WelcomeController welcomeController = (WelcomeController) applicationContext.getBean("welcomeController");
		System.out.println(welcomeService.sayHello("Hello Spring Source Code"));

		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		for (String goal : beanDefinitionNames) {
			System.out.println("--->" + goal);
		}

		Object bean = applicationContext.getBean("userFactoryBean");
		Object bean2 = applicationContext.getBean(BeanFactory.FACTORY_BEAN_PREFIX + "userFactoryBean");
		System.out.println(bean.toString());
		System.out.println(bean2.toString());
	}
}
