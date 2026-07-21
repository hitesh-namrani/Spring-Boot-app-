package com.example.app;

import com.example.app.dao.ClientRepository;
import com.example.app.dao.TransactionsRepository;
import com.example.app.dto.BalanceType;
import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import com.example.app.entity.Client;
import com.example.app.entity.Transactions;
import com.example.app.exception.WalletException;
import com.example.app.logger.AppLogger;
import com.example.app.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TransactionsRepository transactionsRepository;

    @Mock
    private AppLogger logger;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client("john", "hashedPassword");
        client.setId(1L);
        client.setMainBalance(200.0);
        client.setVoucherBalance(500.0);
    }

    // ---------------- REGISTER ----------------

    @Test
    void registerClient_Success() throws Exception {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password"))
                .thenReturn("hashedPassword");

        when(clientRepository.save(any(Client.class)))
                .thenAnswer(i -> i.getArgument(0));

        Client saved = clientService.registerClient("john", "password");

        assertEquals("john", saved.getUsername());

        verify(clientRepository).save(any(Client.class));
        verify(logger).logTransaction("john", "REGISTER", 0.0);
    }

    @Test
    void registerClient_UserAlreadyExists() {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.registerClient("john", "password"));

        assertEquals("ERR_USER_EXISTS", ex.getErrorCode());

        verify(clientRepository, never()).save(any());
    }

    // ---------------- LOGIN ----------------

    @Test
    void verifyLogin_Success() throws WalletException {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        when(passwordEncoder.matches("password", "hashedPassword"))
                .thenReturn(true);

        Client result = clientService.verifyLogin("john", "password");

        assertEquals(client, result);
    }

    @Test
    void verifyLogin_WrongPassword() {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.verifyLogin("john", "wrong"));

        assertEquals("ERR_WRONG_PASSWORD", ex.getErrorCode());
    }

    @Test
    void verifyLogin_UserNotFound() {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.verifyLogin("john", "password"));

        assertEquals(ClientService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------- GET CLIENT BY USERNAME ----------------

    @Test
    void getClientByUsername_Success() throws WalletException {
        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        Client result = clientService.getClientByUsername("john");

        assertEquals(client, result);
    }

    @Test
    void getClientByUsername_UserNotFound() {
        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.getClientByUsername("john"));

        assertEquals(ClientService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    //---------------- PROCESS TRANSACTION USER NOT FOUND ----------------

    @Test
    void processTransaction_UserNotFound() {
        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        100.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN));

        assertEquals(ClientService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------- DEPOSIT ----------------

    @Test
    void processTransaction_DepositMainSuccess() throws WalletException {

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(20.0);

        when(clientRepository.save(any(Client.class)))
                .thenAnswer(i -> i.getArgument(0));

        Client updated = clientService.processTransaction(
                "john",
                50.0,
                TransactionType.DEPOSIT,
                BalanceType.MAIN);

        assertEquals(250.0, updated.getMainBalance());

        verify(transactionsRepository)
                .save(any(Transactions.class));
    }

    @Test
    void processTransaction_DepositVoucherSuccess() throws WalletException {
        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));
        when(transactionsRepository.getDailyTransactionSum(
                eq(1L),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(2000.0);
        when(clientRepository.save(any(Client.class)))
                .thenAnswer(i -> i.getArgument(0));
        Client updated = clientService.processTransaction(
                "john",
                2000.0,
                TransactionType.DEPOSIT,
                BalanceType.VOUCHER);
        assertEquals(2500.0, updated.getVoucherBalance());
        verify(transactionsRepository)
                .save(any(Transactions.class));
    }

    @Test
    void processTransaction_DepositMainLimitExceeded() {

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(90.0);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN));

        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    void processTransaction_DepositVoucherLimitExceeded() {
        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(9000.0);
        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        2000.0,
                        TransactionType.DEPOSIT,
                        BalanceType.VOUCHER
                )
        );
        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    void processTransaction_BalanceTypeRequired() {
        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));
        WalletException ex = assertThrows(WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.DEPOSIT,
                        null
                ));
        assertEquals("ERR_BALANCE_TYPE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void processTransaction_InvalidAmount() {

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        -1.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN));

        assertEquals("ERR_INVALID_AMOUNT", ex.getErrorCode());
    }

    // ---------------- WITHDRAW ----------------

    @ParameterizedTest
    @CsvSource({
            // voucherBalance, mainBalance, voucherWithdrawnToday, mainWithdrawnToday,
            // amount, expectedVoucherBalance, expectedMainBalance

            // Entire withdrawal from voucher
            "500,200,0,0,300,200,200",

            // Voucher limit exhausted (all from main)
            "500,500,10000,0,300,500,200",

            // Voucher balance less than amount
            "100,500,0,0,300,0,300",

            // Voucher limit less than amount
            "500,200,9900,0,300,400,0",

            // Voucher balance exactly equals amount
            "300,200,0,0,300,0,200",

            // Voucher balance zero
            "0,500,0,0,300,0,200",

            // Voucher partially used, remainder from main
            "150,400,0,0,300,0,250"
    })
    void processTransaction_WithdrawSuccess(
            double voucherBalance,
            double mainBalance,
            double voucherWithdrawnToday,
            double mainWithdrawnToday,
            double withdrawAmount,
            double expectedVoucherBalance,
            double expectedMainBalance) throws Exception {

        client.setVoucherBalance(voucherBalance);
        client.setMainBalance(mainBalance);

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(voucherWithdrawnToday);

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(mainWithdrawnToday);

        when(clientRepository.save(any(Client.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Client updated = clientService.processTransaction(
                "john",
                withdrawAmount,
                TransactionType.WITHDRAW,
                null);

        assertEquals(expectedVoucherBalance, updated.getVoucherBalance());
        assertEquals(expectedMainBalance, updated.getMainBalance());

        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void processTransaction_InsufficientFunds() {

        client.setMainBalance(10.0);
        client.setVoucherBalance(20.0);

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        100.0,
                        TransactionType.WITHDRAW,
                        null));

        assertEquals("ERR_INSUFFICIENT_FUNDS", ex.getErrorCode());
    }

    @Test
    void processTransaction_WithdrawLimitReached() {

        client.setVoucherBalance(500.0);
        client.setMainBalance(500.0);

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        // Voucher limit left = 10000 - 9900 = 100
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(9900.0);

        // Main limit left = 500 - 450 = 50
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(450.0);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        200.0,
                        TransactionType.WITHDRAW,
                        null));

        assertEquals("ERR_LIMIT_REACHED", ex.getErrorCode());

        verify(transactionsRepository).save(any(Transactions.class));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void processTransaction_WithdrawLimitOrFundsBreach() {

        client.setVoucherBalance(100.0);
        client.setMainBalance(500.0);

        when(clientRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(client));

        // Voucher limit left = 200
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(9800.0);

        // Main limit left = 100
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.WITHDRAW),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(400.0);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> clientService.processTransaction(
                        "john",
                        250.0,
                        TransactionType.WITHDRAW,
                        null));

        assertEquals("ERR_LIMIT_OR_FUNDS_BREACH", ex.getErrorCode());
    }

    // ---------------- LIMITS ----------------

    @Test
    void limits_ReturnValues() throws WalletException {

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                any(),
                any(),
                any()))
                .thenReturn(10.0);

        Map<String, Object> limits = clientService.limits("john");

        assertEquals(90.0, limits.get("MainDepositLimitLeft"));
        assertEquals(490.0, limits.get("MainWithdrawLimitLeft"));
        assertEquals(9990.0, limits.get("VoucherDepositLimitLeft"));
        assertEquals(9990.0, limits.get("VoucherWithdrawLimitLeft"));
    }

    // ---------------- HISTORY ----------------

    @Test
    void getTransactionHistory_ReturnsTransactions() throws WalletException {

        Transactions tx = new Transactions(
                1L,
                100.0,
                TransactionType.DEPOSIT,
                BalanceType.MAIN,
                Status.SUCCESS);

        when(clientRepository.findByUsername("john"))
                .thenReturn(Optional.of(client));

        when(transactionsRepository.findByClientIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(tx));

        List<Transactions> history =
                clientService.getTransactionHistory("john");

        assertEquals(1, history.size());

        verify(transactionsRepository)
                .findByClientIdOrderByTimestampDesc(1L);
    }

}