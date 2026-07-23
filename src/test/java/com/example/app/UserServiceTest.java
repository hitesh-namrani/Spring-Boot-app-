package com.example.app;

import com.example.app.dao.UserRepository;
import com.example.app.dao.TransactionsRepository;
import com.example.app.dto.BalanceType;
import com.example.app.dto.Status;
import com.example.app.dto.TransactionType;
import com.example.app.entity.User;
import com.example.app.entity.Transactions;
import com.example.app.exception.WalletException;
import com.example.app.logger.AppLogger;
import com.example.app.service.UserService;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionsRepository transactionsRepository;

    @Mock
    private AppLogger logger;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "hashedPassword");
        user.setId(1L);
        user.setMainBalance(200.0);
        user.setVoucherBalance(500.0);
    }

    // ---------------- REGISTER ----------------

    @Test
    void registerUser_Success() throws Exception {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password"))
                .thenReturn("hashedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        User saved = userService.registerUser("john", "password");

        assertEquals("john", saved.getUsername());

        verify(userRepository).save(any(User.class));
        verify(logger).logTransaction("john", "REGISTER", 0.0);
    }

    @Test
    void registerUser_UserAlreadyExists() {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.registerUser("john", "password"));

        assertEquals("ERR_USER_EXISTS", ex.getErrorCode());

        verify(userRepository, never()).save(any());
    }

    // ---------------- LOGIN ----------------

    @Test
    void verifyLogin_Success() throws WalletException {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "hashedPassword"))
                .thenReturn(true);

        User result = userService.verifyLogin("john", "password");

        assertEquals(user, result);
    }

    @Test
    void verifyLogin_WrongPassword() {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.verifyLogin("john", "wrong"));

        assertEquals("ERR_WRONG_PASSWORD", ex.getErrorCode());
    }

    @Test
    void verifyLogin_UserNotFound() {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.verifyLogin("john", "password"));

        assertEquals(UserService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------- GET USER BY USERNAME ----------------

    @Test
    void getUserByUsername_Success() throws WalletException {
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("john");

        assertEquals(user, result);
    }

    @Test
    void getUserByUsername_UserNotFound() {
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.getUserByUsername("john"));

        assertEquals(UserService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    //---------------- PROCESS TRANSACTION USER NOT FOUND ----------------

    @Test
    void processTransaction_UserNotFound() {
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.empty());

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        100.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN,
                        null));

        assertEquals(UserService.ERR_USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------- DEPOSIT ----------------

    @Test
    void processTransaction_DepositMainSuccess() throws WalletException {

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(20.0);

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        User updated = userService.processTransaction(
                "john",
                50.0,
                TransactionType.DEPOSIT,
                BalanceType.MAIN,
                null);

        assertEquals(250.0, updated.getMainBalance());

        verify(transactionsRepository)
                .save(any(Transactions.class));
    }

    @Test
    void processTransaction_DepositVoucherSuccess() throws WalletException {
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(transactionsRepository.getDailyTransactionSum(
                eq(1L),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(2000.0);
        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));
        User updated = userService.processTransaction(
                "john",
                2000.0,
                TransactionType.DEPOSIT,
                BalanceType.VOUCHER,
                null);
        assertEquals(2500.0, updated.getVoucherBalance());
        verify(transactionsRepository)
                .save(any(Transactions.class));
    }

    @Test
    void processTransaction_DepositMainLimitExceeded() {

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(90.0);

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN,
                        null));

        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    void processTransaction_DepositVoucherLimitExceeded() {
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.DEPOSIT),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(9000.0);
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        2000.0,
                        TransactionType.DEPOSIT,
                        BalanceType.VOUCHER,
                        null));
        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    void processTransaction_BalanceTypeRequired() {
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        WalletException ex = assertThrows(WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.DEPOSIT,
                        null,
                        null));
        assertEquals("ERR_BALANCE_TYPE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void processTransaction_InvalidAmount() {

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        -1.0,
                        TransactionType.DEPOSIT,
                        BalanceType.MAIN,
                        null));

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

        user.setVoucherBalance(voucherBalance);
        user.setMainBalance(mainBalance);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

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

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.processTransaction(
                "john",
                withdrawAmount,
                TransactionType.WITHDRAW,
                null,
                null);

        assertEquals(expectedVoucherBalance, updated.getVoucherBalance());
        assertEquals(expectedMainBalance, updated.getMainBalance());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void processTransaction_InsufficientFunds() {

        user.setMainBalance(10.0);
        user.setVoucherBalance(20.0);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        100.0,
                        TransactionType.WITHDRAW,
                        null,
                        null));

        assertEquals("ERR_INSUFFICIENT_FUNDS", ex.getErrorCode());
    }

    @Test
    void processTransaction_WithdrawLimitReached() {

        user.setVoucherBalance(500.0);
        user.setMainBalance(500.0);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

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
                () -> userService.processTransaction(
                        "john",
                        200.0,
                        TransactionType.WITHDRAW,
                        null,
                        null));

        assertEquals("ERR_LIMIT_REACHED", ex.getErrorCode());

        verify(transactionsRepository).save(any(Transactions.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void processTransaction_WithdrawLimitOrFundsBreach() {

        user.setVoucherBalance(100.0);
        user.setMainBalance(500.0);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));

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
                () -> userService.processTransaction(
                        "john",
                        250.0,
                        TransactionType.WITHDRAW,
                        null,
                        null));

        assertEquals("ERR_LIMIT_OR_FUNDS_BREACH", ex.getErrorCode());
    }

    // ---------------- LIMITS ----------------

    @Test
    void limits_ReturnValues() throws WalletException {

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                any(),
                any(),
                any()))
                .thenReturn(10.0);

        Map<String, Object> limits = userService.limits("john");

        assertEquals(90.0, limits.get("MainDepositLimitLeft"));
        assertEquals(490.0, limits.get("MainWithdrawLimitLeft"));
        assertEquals(9990.0, limits.get("VoucherDepositLimitLeft"));
        assertEquals(9990.0, limits.get("VoucherWithdrawLimitLeft"));
        assertEquals(490.0, limits.get("MainTransferLimitLeft"));
        assertEquals(4990.0, limits.get("VoucherTransferLimitLeft"));
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

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(transactionsRepository.findByUserIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(tx));

        List<Transactions> history =
                userService.getTransactionHistory("john");

        assertEquals(1, history.size());

        verify(transactionsRepository)
                .findByUserIdOrderByTimestampDesc(1L);
    }

    //---------------- TRANSFER ----------------

    @Test
    void processTransaction_InvalidReceiver(){
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        50.0,
                        TransactionType.TRANSFER,
                        BalanceType.MAIN,
                        "john"));
        assertEquals("ERR_INVALID_RECEIVER",ex.getErrorCode());
    }

    @Test
    void processTransaction_ReceiverNotFound(){
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("john1"))
                .thenReturn(Optional.empty());
        WalletException ex = assertThrows(WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.TRANSFER,
                        null,
                        "john1"));
        assertEquals("ERR_RECEIVER_NOT_FOUND",ex.getErrorCode());
    }

    @Test
    void processTransaction_Transfer_BalanceTypeRequired() {
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("john1"))
                .thenReturn(Optional.of(user));
        WalletException ex = assertThrows(WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.TRANSFER,
                        null,
                        "john1"));
        assertEquals("ERR_BALANCE_TYPE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void processTransaction_TRANSFER_MAIN_SUCCESS() throws WalletException{
        User receiver = new User("receiver", "hashedPassword");
        receiver.setId(2L);
        receiver.setMainBalance(100.0);
        receiver.setVoucherBalance(100.0);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.TRANSFER),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(50.0);
        when(userRepository.save(any(User.class)))
                .thenAnswer(i->i.getArgument(0));
        User user = userService.processTransaction(
                "john",
                50.0,
                TransactionType.TRANSFER,
                BalanceType.MAIN,
                "receiver");
        assertEquals(150.0,user.getMainBalance());
        assertEquals(150,receiver.getMainBalance());
    }

    @Test
    void processTransaction_TRANSFER_VOUCHER_SUCCESS() throws WalletException{
        User receiver = new User("receiver", "hashedPassword");
        receiver.setId(2L);
        receiver.setMainBalance(100.0);
        receiver.setVoucherBalance(100.0);

        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.TRANSFER),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(50.0);
        when(userRepository.save(any(User.class)))
                .thenAnswer(i->i.getArgument(0));
        User user = userService.processTransaction(
                "john",
                50.0,
                TransactionType.TRANSFER,
                BalanceType.VOUCHER,
                "receiver");
        assertEquals(450.0,user.getVoucherBalance());
        assertEquals(150.0,receiver.getVoucherBalance());
    }

    @Test
    void processTransaction_Transfer_MainLimitExceeded() {
        User receiver = new User("receiver", "hashedPassword");
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.TRANSFER),
                eq(BalanceType.MAIN),
                any()))
                .thenReturn(490.0);
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        20.0,
                        TransactionType.TRANSFER,
                        BalanceType.MAIN,
                        "receiver"));
        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }
    @Test
    void processTransaction_Transfer_VoucherLimitExceeded() {
        User receiver = new User("receiver", "hashedPassword");
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        when(transactionsRepository.getDailyTransactionSum(
                anyLong(),
                eq(TransactionType.TRANSFER),
                eq(BalanceType.VOUCHER),
                any()))
                .thenReturn(4950.0);
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        100.0,
                        TransactionType.TRANSFER,
                        BalanceType.VOUCHER,
                        "receiver"));
        assertEquals("ERR_LIMIT_EXCEEDED", ex.getErrorCode());
    }
    @Test
    void processTransaction_Transfer_InsufficientMainFunds() {
        User receiver = new User("receiver", "hashedPassword");
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        250.0,
                        TransactionType.TRANSFER,
                        BalanceType.MAIN,
                        "receiver"));
        assertEquals("ERR_INSUFFICIENT_FUNDS", ex.getErrorCode());
    }
    @Test
    void processTransaction_Transfer_InsufficientVoucherFunds() {
        User receiver = new User("receiver", "hashedPassword");
        when(userRepository.findByUsernameForUpdate("john"))
                .thenReturn(Optional.of(user));
        when(userRepository.findByUsernameForUpdate("receiver"))
                .thenReturn(Optional.of(receiver));
        WalletException ex = assertThrows(
                WalletException.class,
                () -> userService.processTransaction(
                        "john",
                        600.0,
                        TransactionType.TRANSFER,
                        BalanceType.VOUCHER,
                        "receiver"));
        assertEquals("ERR_INSUFFICIENT_FUNDS", ex.getErrorCode());
    }
}