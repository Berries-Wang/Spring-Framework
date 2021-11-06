package link.bosswang.domain;

import java.io.Serializable;

public class Response implements Serializable {
	private static final long serialVersionUID = -1L;
	private Boolean success;
	private String msg;

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
