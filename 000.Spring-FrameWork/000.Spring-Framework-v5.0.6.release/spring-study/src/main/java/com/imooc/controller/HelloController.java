package com.imooc.controller;

import com.imooc.services.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class HelloController {
	@Autowired
	HelloService helloService;

	public String handleRequest() {
		helloService.sayHello();
		helloService.sayHelloServiceThrowException();
		return "Hello";
	}
}
