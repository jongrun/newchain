package sysu.newchain.rpc.dto;

import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;

public class TxDTO {
	String hash;
	String from;
	String to;
	long amount;
	String time;
	String data;
	
	public static TxDTO fromObject(Transaction tx){
		if (tx == null) {
			return null;
		}
		TxDTO txDTO = new TxDTO();
		txDTO.hash = Hex.encode(tx.getHash());
		txDTO.from = tx.getFrom().getEncodedBase58();
		txDTO.to = tx.getTo().getEncodedBase58();
		txDTO.amount = tx.getAmount();
		txDTO.time = tx.getTime();
		txDTO.data = Base58.encode(tx.getData());
		return txDTO;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
}
