package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {
    private final AccountsRepository accountRepository;
    private final NotificationService notificationService;

    public TransferService(AccountsRepository accountRepository, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
    }

    public void transfer(String accountFromId, String accountToId, BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account accountFrom = accountRepository.getAccount(accountFromId);
        Account accountTo = accountRepository.getAccount(accountToId);

        // Lock both accounts to ensure thread safety, lower ID is locked first to avoid deadlocks
        Account firstLock = accountFrom.getAccountId().compareTo(accountTo.getAccountId()) < 0 ? accountFrom : accountTo;
        Account secondLock = accountFrom.getAccountId().compareTo(accountTo.getAccountId()) < 0 ? accountTo : accountFrom;

        firstLock.getLock().lock();
        secondLock.getLock().lock();
        try {
            if (accountFrom.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient balance in account: " + accountFromId);
            }

            // Perform transfer
            accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
            accountTo.setBalance(accountTo.getBalance().add(amount));

            // Update accounts
            accountRepository.updateAccount(accountFrom);
            accountRepository.updateAccount(accountTo);

            // Notify both account holders
            notificationService.notifyAboutTransfer(accountFrom, "Transferred " + amount + " to account " + accountTo.getAccountId());
            notificationService.notifyAboutTransfer(accountTo, "Received " + amount + " from account " + accountFrom.getAccountId());
        } finally {
            secondLock.getLock().unlock();
            firstLock.getLock().unlock();
        }
    }
}
