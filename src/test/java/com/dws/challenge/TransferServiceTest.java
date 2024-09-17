package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    @Mock
    private AccountsRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testTransferSuccess() throws Exception {
        Account accountFrom = new Account("1", new BigDecimal("1000.00"));
        Account accountTo = new Account("2", new BigDecimal("2000.00"));

        when(accountRepository.getAccount("1")).thenReturn(accountFrom);
        when(accountRepository.getAccount("2")).thenReturn(accountTo);

        transferService.transfer("1", "2", new BigDecimal("500.00"));

        verify(accountRepository, times(1)).updateAccount(accountFrom);
        verify(accountRepository, times(1)).updateAccount(accountTo);
        verify(notificationService, times(1)).notifyAboutTransfer(accountFrom, "Transferred 500.00 to account 2");
        verify(notificationService, times(1)).notifyAboutTransfer(accountTo, "Received 500.00 from account 1");
    }

    @Test
    void testInsufficientFunds() {
        Account accountFrom = new Account("1", new BigDecimal("100.00"));
        Account accountTo = new Account("2", new BigDecimal("2000.00"));

        when(accountRepository.getAccount("1")).thenReturn(accountFrom);
        when(accountRepository.getAccount("2")).thenReturn(accountTo);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transferService.transfer("1", "2", new BigDecimal("500.00"));
        });

        verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
    }
}
