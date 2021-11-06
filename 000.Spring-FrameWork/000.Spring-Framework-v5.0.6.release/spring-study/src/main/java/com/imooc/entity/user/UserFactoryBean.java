package com.imooc.entity.user;

import org.springframework.beans.factory.FactoryBean;

public class UserFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		User user = new User();
		user.setName("Create By UserFactoryBean");
		return user;
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}
}
