package com.imooc.services.impl;

import com.imooc.services.HelloService;
import com.imooc.services.HiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HelloServiceImpl implements HelloService {
	@Autowired
	private HiService hiService;

	 //构造器注入，在构造方法上添加@Autowired
	/*public HelloServiceImpl(HiService hiService) {
		this.hiService = hiService;
	}*/

	@Override
	public void sayHello() {
		hiService.sayHiService();
		System.out.println("I am HelloService , sayHello");
	}

	@Override
	public void sayHelloServiceThrowException() {
		// throw new IllegalArgumentException("I am HelloService , sayHelloServiceThrowException");
	}
}
