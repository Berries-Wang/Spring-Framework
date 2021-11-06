package com.imooc.controller;

import com.imooc.services.HiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class HiController {
	@Autowired
	HiService hiService;

	public String handleRequest() {
		hiService.sayHi();
		hiService.sayHiService();
		return "HI";
	}
}
