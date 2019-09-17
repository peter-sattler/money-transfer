package net.sattler22.transfer.api;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.sattler22.transfer.domain.AccountType.CHECKING;
import static net.sattler22.transfer.domain.AccountType.SAVINGS;
import static org.glassfish.jersey.test.TestProperties.CONTAINER_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import net.sattler22.transfer.domain.Account;
import net.sattler22.transfer.domain.Bank;
import net.sattler22.transfer.domain.Customer;
import net.sattler22.transfer.dto.AccountDTO;
import net.sattler22.transfer.dto.AccountTransferDTO;
import net.sattler22.transfer.service.TransferService;
import net.sattler22.transfer.service.TransferService.TransferResult;
import net.sattler22.transfer.service.TransferServiceInMemoryImpl;
import net.sattler22.transfer.util.TestDataFactory;

/**
 * Money Transfer REST Resource Integration Test Harness
 *
 * @author Pete Sattler
 * @version September 2019
 */
public final class MoneyTransferResourceIntegrationTestHarness extends JerseyTest {

    private static final String API_BASE_PATH = "/api/money-transfer";

    @Override
    protected Application configure() {
        this.set(CONTAINER_PORT, "0");  //Use a free port
        final ResourceConfig config = new ResourceConfig();
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                final Bank bank = initBank();
                final TransferServiceInMemoryImpl transferServiceImpl =
                    new TransferServiceInMemoryImpl(bank);
                this.bind(transferServiceImpl).to(TransferService.class);
            }
        });
        config.register(MoneyTransferResource.class);
        return config;
    }

    private Bank initBank() {
        return new Bank(1, "Money Transfer API Integration Test Harness Bank");
    }

    @Test
    public void fetchBankDetailsHappyPathTestCase() {
        final Response response = target(API_BASE_PATH).path("bank").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final Bank expected = initBank();
        final Bank actual = response.readEntity(Bank.class);
        assertEquals(expected, actual);
    }

    @Test
    public void fetchAllCustomersHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        final Customer burt = TestDataFactory.getBurt("234");
        addCustomerImpl(bob);
        addCustomerImpl(burt);
        final Response response = target(API_BASE_PATH).path("customers").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final Set<Customer> actual = response.readEntity(new GenericType<Set<Customer>>() {});
        final Set<Customer> expected = new HashSet<>();
        expected.add(bob);
        expected.add(burt);
        assertEquals(expected, actual);
    }

    @Test
    public void fetchOneCustomerHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Response response = target(API_BASE_PATH).path("customer").path(bob.getId()).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final Customer actual = response.readEntity(Customer.class);
        assertEquals(bob, actual);
    }

    @Test
    public void fetchOneCustomerNotFoundTestCase() {
        final Response response = target(API_BASE_PATH).path("customer").path("999").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void addCustomerHappyPathTestCase() {
        final Response response = addCustomerImpl(TestDataFactory.getBob("123"));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void addCustomerAlreadyExistsTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Response response = addCustomerImpl(bob);
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void deleteCustomerHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Response response = deleteCustomerImpl(bob);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteCustomerNotFoundTestCase() {
        final Response response = deleteCustomerImpl(TestDataFactory.getBob("123"));
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void fetchAccountsForCustomerHappyPathTestCase() {
        final Customer burt = TestDataFactory.getBurt("234");
        final Account checking = new Account(1, CHECKING, burt, ZERO);
        final Account savings = new Account(1, SAVINGS, burt, ZERO);
        addCustomerImpl(burt);
        addAccountImpl(checking);
        addAccountImpl(savings);
        final Response response = target(API_BASE_PATH).path("accounts").path(burt.getId()).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final Set<Account> actual = response.readEntity(new GenericType<Set<Account>>() {});
        final Set<Account> expected = new HashSet<>();
        expected.add(checking);
        expected.add(savings);
        assertEquals(expected, actual);
    }

    @Test
    public void fetchAccountsForCustomerNoAccountsTestCase() {
        final Customer burt = TestDataFactory.getBurt("234");
        addCustomerImpl(burt);
        final Response response = target(API_BASE_PATH).path("accounts").path(burt.getId()).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final Set<Account> actual = response.readEntity(new GenericType<Set<Account>>() {});
        final Set<Account> expected = new HashSet<>();
        assertEquals(expected, actual);
    }

    @Test
    public void fetchAccountsForCustomerNotFoundTestCase() {
        final Customer burt = TestDataFactory.getBurt("234");
        final Response response = target(API_BASE_PATH).path("accounts").path(burt.getId()).request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void addAccountHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Response response = addAccountImpl(new Account(1, CHECKING, bob, ONE));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void addAccountOwnerNotFoundTestCase() {
        final Response response =
            addAccountImpl(new Account(1, SAVINGS, TestDataFactory.getBob("123"), ZERO));
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void addAccountAlreadyExistsTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        addAccountImpl(new Account(1, CHECKING, bob, ZERO));
        final Response response = addAccountImpl(new Account(1, CHECKING, bob, ZERO));
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void deleteAccountHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        addAccountImpl(new Account(1, SAVINGS, bob, ONE));
        final Response response = deleteAccountImpl(bob, 1);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void deleteAccountNotFoundTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Response response = deleteAccountImpl(bob, 1);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void deleteAccountCustomerNotFoundTestCase() {
        final Response response = deleteAccountImpl(TestDataFactory.getBob("123"), 1);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void accountTransferHappyPathTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final BigDecimal sourceAccountInitialBalance = new BigDecimal("100");
        final BigDecimal targetAccountInitialBalance = new BigDecimal("200");
        final Account sourceAccount = new Account(1, CHECKING, bob, sourceAccountInitialBalance);
        final Account targetAccount = new Account(2, CHECKING, bob, targetAccountInitialBalance);
        addAccountImpl(sourceAccount);
        addAccountImpl(targetAccount);
        final BigDecimal transferAmount = new BigDecimal("50");
        final Response response =
            accountTransferImpl(bob, sourceAccount.getNumber(), targetAccount.getNumber(), transferAmount);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_JSON, response.getHeaderString(CONTENT_TYPE));
        final TransferResult transferResult = response.readEntity(TransferResult.class);
        final BigDecimal expectedSourceAccountBalance = sourceAccountInitialBalance.subtract(transferAmount);
        final BigDecimal expectedTargetAccountBalance = targetAccountInitialBalance.add(transferAmount);
        assertEquals(expectedSourceAccountBalance, transferResult.getSource().getBalance());
        assertEquals(expectedTargetAccountBalance, transferResult.getTarget().getBalance());
    }

    @Test
    public void accountTransferCustomerNotFoundTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        final Account sourceAccount = new Account(1, CHECKING, bob, new BigDecimal("100"));
        final Account targetAccount = new Account(2, CHECKING, bob, new BigDecimal("200"));
        addAccountImpl(sourceAccount);
        addAccountImpl(targetAccount);
        final BigDecimal transferAmount = new BigDecimal("50");
        final Response response =
            accountTransferImpl(bob, sourceAccount.getNumber(), targetAccount.getNumber(), transferAmount);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void accountTransferSourceAccountNotFoundTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Account targetAccount = new Account(2, CHECKING, bob, new BigDecimal("200"));
        addAccountImpl(targetAccount);
        final BigDecimal transferAmount = new BigDecimal("50");
        final Response response = accountTransferImpl(bob, 1, targetAccount.getNumber(), transferAmount);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void accountTransferTargetAccountNotFoundTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final Account sourceAccount = new Account(1, SAVINGS, bob, new BigDecimal("100"));
        addAccountImpl(sourceAccount);
        final BigDecimal transferAmount = new BigDecimal("50");
        final Response response = accountTransferImpl(bob, sourceAccount.getNumber(), 2, transferAmount);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void accountTransferAmountZeroTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final BigDecimal sourceAccountInitialBalance = new BigDecimal("100");
        final BigDecimal targetAccountInitialBalance = new BigDecimal("200");
        final Account sourceAccount = new Account(1, SAVINGS, bob, sourceAccountInitialBalance);
        final Account targetAccount = new Account(2, SAVINGS, bob, targetAccountInitialBalance);
        addAccountImpl(sourceAccount);
        addAccountImpl(targetAccount);
        final BigDecimal transferAmount = ZERO;
        final Response response =
            accountTransferImpl(bob, sourceAccount.getNumber(), targetAccount.getNumber(), transferAmount);
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    @Test
    public void accountTransferInsufficentFundsTestCase() {
        final Customer bob = TestDataFactory.getBob("123");
        addCustomerImpl(bob);
        final BigDecimal sourceAccountInitialBalance = new BigDecimal("100");
        final BigDecimal targetAccountInitialBalance = new BigDecimal("200");
        final Account sourceAccount = new Account(1, CHECKING, bob, sourceAccountInitialBalance);
        final Account targetAccount = new Account(2, SAVINGS, bob, targetAccountInitialBalance);
        addAccountImpl(sourceAccount);
        addAccountImpl(targetAccount);
        final BigDecimal transferAmount = sourceAccountInitialBalance.add(new BigDecimal("1"));
        final Response response =
            accountTransferImpl(bob, sourceAccount.getNumber(), targetAccount.getNumber(), transferAmount);
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        assertNull(response.getHeaderString(CONTENT_TYPE));
    }

    /**
     * Add a customer
     */
    private Response addCustomerImpl(Customer customer) {
        return target(API_BASE_PATH).path("customer").request().post(Entity.json(customer));
    }

    /**
     * Delete a customer
     */
    private Response deleteCustomerImpl(Customer customer) {
        return target(API_BASE_PATH).path("customer").path(customer.getId()).request().delete();
    }

    /**
     * Add an account
     */
    private Response addAccountImpl(Account account) {
        final AccountDTO accountDTO =
            new AccountDTO(account.getNumber(), account.getType(), account.getOwner().getId(), account.getBalance());
        return target(API_BASE_PATH).path("account").request().post(Entity.json(accountDTO));
    }

    /**
     * Delete an account
     */
    private Response deleteAccountImpl(Customer customer, int accountNbr) {
        return target(API_BASE_PATH).path("account")
                                    .path(customer.getId())
                                    .path(String.valueOf(accountNbr)).request().delete();
    }

    /**
     * Transfer funds between accounts
     */
    private Response accountTransferImpl(Customer owner, int sourceAccountNbr, int targetAccountNbr, BigDecimal transferAmount) {
        final AccountTransferDTO accountTransferDTO =
            new AccountTransferDTO(owner.getId(), sourceAccountNbr, targetAccountNbr, transferAmount);
        return target(API_BASE_PATH).path("/account/transfer").request().put(Entity.json(accountTransferDTO));
    }
}
