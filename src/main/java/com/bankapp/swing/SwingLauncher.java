package com.bankapp.swing;

import com.bankapp.config.DatabaseManager;
import com.bankapp.dao.AccountDao;
import com.bankapp.dao.LoanDao;
import com.bankapp.dao.TransactionDao;
import com.bankapp.dao.UserDao;
import com.bankapp.service.AccountService;
import com.bankapp.service.AuthService;
import com.bankapp.service.LoanService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwingLauncher {
    public static void main(String[] args) {
        try {
            DatabaseManager.initializeDatabase();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            UserDao userDao = new UserDao();
            AccountDao accountDao = new AccountDao();
            TransactionDao transactionDao = new TransactionDao();
            LoanDao loanDao = new LoanDao();

            AuthService authService = new AuthService(userDao);
            AccountService accountService = new AccountService(accountDao, transactionDao);
            LoanService loanService = new LoanService(loanDao, accountDao, transactionDao);

            SwingUtilities.invokeLater(() -> {
                SwingMainFrame frame = new SwingMainFrame(authService, accountService, loanService);
                frame.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
