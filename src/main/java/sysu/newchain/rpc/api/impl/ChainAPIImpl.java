package sysu.newchain.rpc.api.impl;

import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;

import sysu.newchain.client.Client;
import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.TxMsg;
import sysu.newchain.core.Address;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.TransactionPb;
import sysu.newchain.rpc.api.ChainAPI;
import sysu.newchain.rpc.dto.InsertTransRespDTO;
import sysu.newchain.server.Server;

@Service
@AutoJsonRpcServiceImpl
public class ChainAPIImpl implements ChainAPI{
	Logger logger = LoggerFactory.getLogger(ChainAPIImpl.class);
	
	Client client;
	ECKey clientKey;
	
	{
		try {
			clientKey = ECKey.fromPrivate(Base58.decode(AppConfig.getClientPriKey()));
			client = new Client(clientKey, clientKey.getPubKeyAsBase58());
			client.start();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	@Override
	public InsertTransRespDTO insertTransaction(String from, String to, long amount, String time, String sign,
			String pubKey, String data) throws Exception {
		logger.debug("rpc insert transaction");
		
//		TransactionPb.Builder txBuilder = TransactionPb.newBuilder();
//		txBuilder.setFrom(from);
//		txBuilder.setTo(to);
//		txBuilder.setAmount(amount);
//		txBuilder.setTime(time);
//		txBuilder.setSign(ByteString.copyFrom(Base58.decode(sign)));
//		txBuilder.setPubKey(ByteString.copyFrom(Base58.decode(pubKey)));
//		txBuilder.setData(ByteString.copyFrom(Base58.decode(data)));
//		txBuilder.setClientPubKey(ByteString.copyFrom(clientKey.getPubKeyAsBytes()));
//		TxMsg txMsg = new TxMsg();
//		txMsg.setBuilder(txBuilder);
		Transaction tx = new Transaction(
				new Address(from),
				new Address(to), 
				amount, 
				time, 
				Base58.decode(sign), 
				Base58.decode(pubKey), 
				Base58.decode(data), 
				clientKey.getPubKeyAsBytes());
		tx.calculateAndSetHash();
		try {
			client.sendTxToServer(tx);
		} catch (Exception e) {
			logger.error("", e);
		}
		return new InsertTransRespDTO(1, "ok", "ssssss", "12", "12333");
	}
}
