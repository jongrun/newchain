package sysu.newchain.dao;

import sysu.newchain.common.core.Address;

public class AccountDaoTest {
	public static void main(String[] args) throws Exception {
		AccountDao accountDao = AccountDao.getInstance();
		
		Address address = new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD");
		Address address2 = new Address("16fRnCmnf2ic9Bv5LsEydtpEdG3CpPCVj1");
		System.out.println(accountDao.getBalance(address));
		accountDao.setBalance(address, 100L);
		accountDao.transfer(address, address2, 20L);
		System.out.println(accountDao.getBalance(address) == 80L);
		System.out.println(accountDao.getBalance(address2) == 20L);
	}
}
