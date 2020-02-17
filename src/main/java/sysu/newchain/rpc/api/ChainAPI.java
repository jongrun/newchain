package sysu.newchain.rpc.api;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import sysu.newchain.rpc.dto.InsertTransRespDTO;

@JsonRpcService("/transaction")
public interface ChainAPI {
	/**
	 * @param from 		来源地址
	 * @param to		目标地址
	 * @param amount	金额
	 * @param time		时间戳
	 * @param sign		签名，base58编码
	 * @param pubKey	公钥，base58编码
	 * @param data		附带数据
	 * @return
	 * @throws Exception
	 */
	InsertTransRespDTO insertTransaction(
			@JsonRpcParam(value = "from") String from, 
			@JsonRpcParam(value = "to") String to, 
			@JsonRpcParam(value = "amount") long amount, 
			@JsonRpcParam(value = "time") String time,
			@JsonRpcParam(value = "sign") String sign, 
			@JsonRpcParam(value = "pubKey") String pubKey,
			@JsonRpcParam(value = "data") String data) throws Exception;
}
