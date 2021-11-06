package link.bosswang.controller;

import link.bosswang.dto.HelloDto;
import link.bosswang.dto.HelloRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/hello")
public class HelloController {

	@RequestMapping(value = "/sayHello", method = RequestMethod.GET)
	@ResponseBody
	public static HelloDto<String> sayHello() {

		System.out.println("Hello Controller say Hello");


		return HelloDto.<String>build(200, "OK", true, "OK");
	}

	@RequestMapping(value = "/hello", method = RequestMethod.GET)
	public  String hello(String hello) {
		return "Hello";
	}

	@RequestMapping(value = "/json", method = RequestMethod.GET)
	@ResponseBody
	public HelloDto<String> json(@RequestBody HelloRequest request) {
		System.out.println("Hello Controller say json");


		return HelloDto.<String>build(200, "OK", true, "OK");
	}

}
