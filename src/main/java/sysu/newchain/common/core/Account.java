package sysu.newchain.common.core;

public class Account {
	private Address address;
	private long balance;
	
	private void Acount(Address address, long balance) {
		this.address = address;
		this.balance = balance;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}
}
