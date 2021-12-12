package com.imooc.services.impl;

import com.imooc.services.WelcomeService;
import org.springframework.stereotype.Service;

@Service
public class WelcomeServiceImpl implements WelcomeService {
	@Override
	public String sayHello(String name) {
		System.out.println("SayHello: " + name);
		return "Success";
	}
}
