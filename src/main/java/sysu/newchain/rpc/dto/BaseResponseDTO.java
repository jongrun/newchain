package sysu.newchain.rpc.dto;

/**
 * @Description rpc响应的数据传输对象的基类
 * @author jongliao
 * @date 2020年1月20日 上午10:55:58
 */
public class BaseResponseDTO {
	int code;
	String msg;
	
	public BaseResponseDTO(int code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public String getMsg() {
		return msg;
	}
	
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
}
