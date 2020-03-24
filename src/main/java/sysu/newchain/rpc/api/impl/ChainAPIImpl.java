package sysu.newchain.rpc.api.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;
import com.google.protobuf.RpcCallback;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;

import sysu.newchain.common.core.Address;
import sysu.newchain.common.core.Block;
import sysu.newchain.common.core.BlockHeader;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.client.RequestClient;
import sysu.newchain.consensus.server.RequestResponer;
import sysu.newchain.consensus.server.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.server.pbft.msg.TxMsg;
import sysu.newchain.dao.service.DaoService;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.TransactionPb;
import sysu.newchain.rpc.api.ChainAPI;
import sysu.newchain.rpc.dto.BlockDTO;
import sysu.newchain.rpc.dto.BlockHeaderDTO;
import sysu.newchain.rpc.dto.TxDTO;
import sysu.newchain.rpc.dto.TxRespDTO;

@Service
@AutoJsonRpcServiceImpl
public class ChainAPIImpl implements ChainAPI{
	private static final Logger logger = LoggerFactory.getLogger(ChainAPIImpl.class);
	
	private RequestClient client;
	DaoService daoService = DaoService.getInstance();
	{
		try {
			SchnorrKey clientKey = SchnorrKey.fromPrivate(Base58.decode(AppConfig.getClientPriKey()));
			client = new RequestClient(clientKey, clientKey.getPubKeyAsBase58());
			client.start();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	@Override
	public TxRespDTO insertTransaction(String from, String to, long amount, String time, String sign,
			String pubKey, String data) throws Exception {
		logger.debug("rpc insert transaction");
		Transaction tx = new Transaction(
				new Address(from),
				new Address(to), 
				amount, 
				time, 
				Base58.decode(sign), 
				Base58.decode(pubKey), 
				Base58.decode(data));
//		tx.calculateAndSetHash();
		tx.verifySign(); // 过滤签名格式出错等情况
		CompletableFuture<TxRespDTO> future = client.sendTxToServer(tx);
		// TODO 暂时用同步方式，待改进
		TxRespDTO dto = future.get(3000, TimeUnit.SECONDS);
		return dto;
	}
	
	@Override
	public TxDTO getTransaction(String hash) throws Exception {
		Transaction tx = daoService.getTransactionDao().getTransaction(Hex.decode(hash));
		return TxDTO.fromObject(tx);
	}

	@Override
	public BlockDTO getBlock(long height) throws Exception {
		Block block = daoService.getBlockDao().getBlock(height);
		return BlockDTO.fromObject(block);
	}
	
	@Override
	public BlockHeaderDTO getBlockHeader(long height) throws Exception {
		BlockHeader header = daoService.getBlockDao().getBlockHeader(height);
		return BlockHeaderDTO.fromObject(header);
	}

	@Override
	public long getLastHeight() throws Exception {
		return daoService.getBlockDao().getLastHeight();
	}
}
