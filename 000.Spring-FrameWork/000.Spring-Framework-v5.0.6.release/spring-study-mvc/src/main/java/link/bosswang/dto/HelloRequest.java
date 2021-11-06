package link.bosswang.dto;

public class HelloRequest {
	private String hello;

	public String getHello() {
		return hello;
	}

	public void setHello(String hello) {
		this.hello = hello;
	}

	@Override
	public String toString() {
		return "HelloRequest{" +
				"hello='" + hello + '\'' +
				'}';
	}
}
