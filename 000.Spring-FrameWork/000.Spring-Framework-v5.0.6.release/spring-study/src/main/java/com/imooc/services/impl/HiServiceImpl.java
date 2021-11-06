package com.imooc.services.impl;

import com.imooc.services.HelloService;
import com.imooc.services.HiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HiServiceImpl implements HiService {
	@Autowired
	private HelloService helloService;

	//构造器注入，在构造方法上添加@Autowired
	/*public HiServiceImpl(HelloService helloService) {
		this.helloService = helloService;
	}
*/
	@Override
	public void sayHi() {
		System.out.println("I am HiServiceImpl , sayHi");
	}

	@Override
	public void sayHiService() {
		System.out.println("I am HiServiceImpl , sayHiService");
	}

	public void setHelloService(HelloServiceImpl helloService) {
		this.helloService = helloService;
	}
}
