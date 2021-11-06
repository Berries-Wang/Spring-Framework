package com.imooc.controller;

import com.imooc.services.WelcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WelcomeController {
	//@Autowired
	private WelcomeService welcomeService;
}
