package com.imooc;

import com.imooc.controller.HelloController;
import com.imooc.controller.HiController;
import com.imooc.controller.WelcomeController;
import com.imooc.data.People;
import com.imooc.services.WelcomeService;
import com.imooc.services.impl.TransactionalServiceImpl;
import com.imooc.services.impl.WelcomeServiceImpl;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Map;

@Configuration
@EnableAspectJAutoProxy
@EnableTransactionManagement
@ComponentScan("com.imooc")
public class EntranceAnno {
	/**
	 * 通过注解来初始化IOC
	 */
	public static void main(String[] args) {
		// 获取程序运行的过程中产生的代理类(分别代表CGLIB代理和JDK动态代理)
		/**System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY,
				"/home/wei/workspace/SOURCE_CODE/Spring-Framework-v5.0.6.release/spring-proxy");
		System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles",
				"true");**/

		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EntranceAnno.class);

		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		for (String goal : beanDefinitionNames) {
			System.out.println("--->" + goal);
		}

		WelcomeServiceImpl welcomeService = (WelcomeServiceImpl) applicationContext.getBean("welcomeServiceImpl");
		WelcomeController welcomeController = applicationContext.getBean(WelcomeController.class);

		welcomeService.sayHello("I am create by annotation");


		HiController hiController = (HiController) applicationContext.getBean("hiController");
		hiController.handleRequest();

		HelloController helloController = (HelloController) applicationContext.getBean("helloController");
		helloController.handleRequest();

		TransactionalServiceImpl transactionalService = (TransactionalServiceImpl) applicationContext.getBean("transactionalServiceImpl");
		transactionalService.testTrans();
		try {
			transactionalService.testTransRollBack();
		} catch (Exception e) {
			System.out.println("==================================>" + e.getMessage());
		}

	}
}
