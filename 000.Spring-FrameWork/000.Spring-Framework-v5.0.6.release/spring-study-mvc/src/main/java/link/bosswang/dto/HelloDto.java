package link.bosswang.dto;

public class HelloDto<T> {
	private Integer code;
	private String msg;
	private boolean success;
	private T data;

	public static <T> HelloDto<T> build(Integer code, String msg, boolean success, T data) {
		HelloDto<T> helloDto = new HelloDto<>();
		helloDto.code = code;
		helloDto.msg = msg;
		helloDto.success = success;
		helloDto.data = data;
		return helloDto;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "HelloDto{" +
				"code=" + code +
				", msg='" + msg + '\'' +
				", success=" + success +
				", data=" + data +
				'}';
	}
}
