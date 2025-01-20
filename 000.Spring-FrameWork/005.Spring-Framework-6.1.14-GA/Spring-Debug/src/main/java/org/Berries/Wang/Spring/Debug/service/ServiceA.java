package org.Berries.Wang.Spring.Debug.service;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ServiceA {

	@Resource
	private ServiceB serviceB;

	public String sayServiceA() {
		return "Service_A";
	}
}
