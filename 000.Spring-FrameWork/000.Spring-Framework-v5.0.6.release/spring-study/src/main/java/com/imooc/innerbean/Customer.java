package com.imooc.innerbean;

/**
 * Spring inner Bean学习
 */
public class Customer {
	private Person person;

	public Customer() {
	}

	public Customer(Person person) {
		this.person = person;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Override
	public String toString() {
		return "Customer{" +
				"person=" + person +
				'}';
	}
}
