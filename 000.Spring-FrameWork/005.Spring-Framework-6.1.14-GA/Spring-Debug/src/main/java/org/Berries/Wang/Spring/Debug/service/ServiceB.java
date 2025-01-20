package org.Berries.Wang.Spring.Debug.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ServiceB {
	@Resource
	private ServiceA serviceA;

	public String sayServiceB() {
		return "Service-B";
	}
}
