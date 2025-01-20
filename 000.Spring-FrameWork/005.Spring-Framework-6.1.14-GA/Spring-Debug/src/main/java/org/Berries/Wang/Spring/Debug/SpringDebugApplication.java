package org.Berries.Wang.Spring.Debug;

import org.Berries.Wang.Spring.Debug.service.ServiceA;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringDebugApplication {
	public static void main(String[] args) {
		ApplicationContext app = new AnnotationConfigApplicationContext("org.Berries.Wang.Spring.Debug");

		ServiceA serviceA = app.getBean(ServiceA.class);
		System.out.println(serviceA.sayServiceA());
	}
}
