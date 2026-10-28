package com.indra.jpa.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.indra.jpa.transactions.exception.InsufficientFundsException;
import com.indra.jpa.transactions.model.Account;
import com.indra.jpa.transactions.model.TransferEvent;
import com.indra.jpa.transactions.repository.AccountRepository;
import com.indra.jpa.transactions.repository.TransferEventRepository;
import com.indra.jpa.transactions.service.TransferService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true")
class TransactionsApplicationTests {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransferEventRepository transferEventRepository;

	@Autowired
	private TransferService transferService;

	private Account source;
	private Account destination;

	@BeforeEach
	void setUp() {
		transferEventRepository.deleteAll();
		accountRepository.deleteAll();
		source = createAccount("Alice", "100.00");
		destination = createAccount("Bob", "50.00");
	}

	@Test
	void insufficientFundsRollsBackBothBalances() {
		assertThrows(InsufficientFundsException.class,
				() -> transferService.transfer(source.getId(), destination.getId(), new BigDecimal("500.00")));

		assertBalance(source.getId(), "100.00");
		assertBalance(destination.getId(), "50.00");
	}

	@Test
	void auditSurvivesTransferRollback() {
		assertThrows(InsufficientFundsException.class,
				() -> transferService.transfer(source.getId(), destination.getId(), new BigDecimal("500.00")));

		List<TransferEvent> events = transferEventRepository.findAll();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getFromAccountId()).isEqualTo(source.getId());
		assertThat(events.get(0).getToAccountId()).isEqualTo(destination.getId());
		assertThat(events.get(0).getAmount()).isEqualByComparingTo("500.00");
	}

	@Test
	void successfulTransferUpdatesBothBalancesCorrectly() throws InsufficientFundsException {
		transferService.transfer(source.getId(), destination.getId(), new BigDecimal("30.00"));

		assertBalance(source.getId(), "70.00");
		assertBalance(destination.getId(), "80.00");

		List<TransferEvent> events = transferEventRepository.findAll();
		assertThat(events).hasSize(1);
		assertThat(events.get(0).getAmount()).isEqualByComparingTo("30.00");
	}

	private Account createAccount(String owner, String balance) {
		Account account = new Account();
		account.setOwner(owner);
		account.setBalance(new BigDecimal(balance));
		return accountRepository.saveAndFlush(account);
	}

	private void assertBalance(Long accountId, String expectedBalance) {
		assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
				.isEqualByComparingTo(expectedBalance);
	}
}