package com.imooc.innerbean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.FileSystemXmlApplicationContext;

@Configuration
@ComponentScan("com.com.imooc")
public class Entrance {

	/**
	 * 通过XMl文件来初始化IOC
	 */
	public static void main(String[] args) {
		String xmlPath1 = "G:\\Study_WorkSpace\\Spring\\Spring-Framework-v5.0.6.release\\" +
				"spring-study\\src\\main\\resources\\spring\\innerbean\\Spring-inner-bean-1.xml";

		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(xmlPath1);
		Object customer = applicationContext.getBean("customer");
		System.out.println(customer.toString());

		//-----------
		String xmlPath2 = "G:\\Study_WorkSpace\\Spring\\Spring-Framework-v5.0.6.release\\" +
				"spring-study\\src\\main\\resources\\spring\\innerbean\\Spring-inner-bean-2.xml";

		ApplicationContext applicationContext2 = new FileSystemXmlApplicationContext(xmlPath2);
		Object customer2 = applicationContext2.getBean("customer");
		System.out.println(customer2.toString());

		//------
		String xmlPath3 = "G:\\Study_WorkSpace\\Spring\\Spring-Framework-v5.0.6.release\\" +
				"spring-study\\src\\main\\resources\\spring\\innerbean\\Spring-inner-bean-3.xml";

		ApplicationContext applicationContext3 = new FileSystemXmlApplicationContext(xmlPath3);
		Object customer3 = applicationContext3.getBean("customer");
		System.out.println(customer3.toString());
	}
}
