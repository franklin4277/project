package com.bankapp.config;

import com.bankapp.dao.AccountDao;
import com.bankapp.dao.LoanDao;
import com.bankapp.dao.TransactionDao;
import com.bankapp.dao.UserDao;
import com.bankapp.service.AccountService;
import com.bankapp.service.AuthService;
import com.bankapp.service.LoanService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeansConfig {

    @Bean
    public UserDao userDao() {
        return new UserDao();
    }

    @Bean
    public AccountDao accountDao() {
        return new AccountDao();
    }

    @Bean
    public TransactionDao transactionDao() {
        return new TransactionDao();
    }

    @Bean
    public LoanDao loanDao() {
        return new LoanDao();
    }

    @Bean
    public AuthService authService(UserDao userDao) {
        return new AuthService(userDao);
    }

    @Bean
    public AccountService accountService(AccountDao accountDao, TransactionDao transactionDao) {
        return new AccountService(accountDao, transactionDao);
    }

    @Bean
    public LoanService loanService(LoanDao loanDao, AccountDao accountDao, TransactionDao transactionDao) {
        return new LoanService(loanDao, accountDao, transactionDao);
    }
}
