package com.bankapp.swing;

import com.bankapp.enums.AccountType;
import com.bankapp.enums.Role;
import com.bankapp.enums.WithdrawalChannel;
import com.bankapp.exception.BankException;
import com.bankapp.model.Account;
import com.bankapp.model.Loan;
import com.bankapp.model.User;
import com.bankapp.service.AccountService;
import com.bankapp.service.AuthService;
import com.bankapp.service.LoanService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SwingMainFrame extends JFrame {
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final AuthService authService;
    private final AccountService accountService;
    private final LoanService loanService;

    private User currentUser;

    private final JTextField usernameField = new JTextField(11);
    private final JPasswordField passwordField = new JPasswordField(11);
    private final JButton loginBtn = new JButton("Login");
    private final JButton logoutBtn = new JButton("Logout");
    private final JLabel sessionLabel = new JLabel("Not logged in", SwingConstants.RIGHT);

    private final JTextField openClientIdField = new JTextField(8);
    private final JComboBox<AccountType> openAccountTypeBox = new JComboBox<>(AccountType.values());
    private final JButton openAccountBtn = new JButton("Open Account");

    private final JTextField depositAccountIdField = new JTextField(8);
    private final JTextField depositAmountField = new JTextField(8);
    private final JComboBox<String> depositChannelBox = new JComboBox<>(new String[]{"MPESA", "COUNTER", "ONLINE"});
    private final JButton depositBtn = new JButton("Deposit");

    private final JTextField withdrawAccountIdField = new JTextField(8);
    private final JTextField withdrawAmountField = new JTextField(8);
    private final JComboBox<WithdrawalChannel> withdrawChannelBox = new JComboBox<>(WithdrawalChannel.values());
    private final JButton withdrawBtn = new JButton("Withdraw");

    private final JTextField transferFromField = new JTextField(8);
    private final JTextField transferToField = new JTextField(8);
    private final JTextField transferAmountField = new JTextField(8);
    private final JButton transferBtn = new JButton("Transfer");

    private final JButton postSavingsInterestBtn = new JButton("Post Savings 3%");
    private final JButton postFixedInterestBtn = new JButton("Post Fixed 8%");

    private final JTextField accountsClientIdField = new JTextField(8);
    private final JButton loadAccountsBtn = new JButton("Load Accounts");
    private final JTextArea accountsOutput = new JTextArea(12, 90);

    private final JTextField loanClientIdField = new JTextField(8);
    private final JTextField loanAccountIdField = new JTextField(8);
    private final JTextField loanAmountField = new JTextField(8);
    private final JCheckBox topUpBox = new JCheckBox("Top-up");
    private final JButton applyLoanBtn = new JButton("Apply Loan");

    private final JTextField repayLoanIdField = new JTextField(8);
    private final JTextField repayAccountIdField = new JTextField(8);
    private final JTextField repayAmountField = new JTextField(8);
    private final JButton repayLoanBtn = new JButton("Repay Loan");

    private final JTextField pendingLoanIdField = new JTextField(8);
    private final JTextField pendingRemarkField = new JTextField(14);
    private final JButton listPendingBtn = new JButton("List Pending");
    private final JButton approveLoanBtn = new JButton("Approve");
    private final JButton rejectLoanBtn = new JButton("Reject");

    private final JTextField loansClientIdField = new JTextField(8);
    private final JButton loadLoansBtn = new JButton("Load Loans");
    private final JTextArea loansOutput = new JTextArea(12, 90);

    private final JTextField statementAccountIdField = new JTextField(8);
    private final JButton accountStatementBtn = new JButton("Account Statement");
    private final JTextField statementLoanIdField = new JTextField(8);
    private final JButton loanStatementBtn = new JButton("Loan Statement");
    private final JTextArea statementOutput = new JTextArea(20, 100);

    public SwingMainFrame(AuthService authService, AccountService accountService, LoanService loanService) {
        this.authService = authService;
        this.accountService = accountService;
        this.loanService = loanService;

        setTitle("Bank Application System - Swing Frontend");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(8, 8));
        add(buildTopPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Loans", buildLoansTab());
        tabs.addTab("Statements", buildStatementsTab());
        add(tabs, BorderLayout.CENTER);

        sessionLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        add(sessionLabel, BorderLayout.SOUTH);

        wireActions();
        applyPermissions();
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createTitledBorder("Authentication"));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.add(new JLabel("Username"));
        left.add(usernameField);
        left.add(new JLabel("Password"));
        left.add(passwordField);
        left.add(loginBtn);
        left.add(logoutBtn);

        JLabel hint = new JLabel("Demo: alice/client123, bob/client123, operator/operator123, manager/manager123");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));

        top.add(left, BorderLayout.NORTH);
        top.add(hint, BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));

        forms.add(buildFormBlock("Open Account (Operator)", openClientIdField, openAccountTypeBox, openAccountBtn));
        forms.add(buildFormBlock("Deposit", depositAccountIdField, depositAmountField, depositChannelBox, depositBtn));
        forms.add(buildFormBlock("Withdraw", withdrawAccountIdField, withdrawAmountField, withdrawChannelBox, withdrawBtn));
        forms.add(buildFormBlock("Transfer", transferFromField, transferToField, transferAmountField, transferBtn));

        JPanel interestBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        interestBlock.setBorder(BorderFactory.createTitledBorder("Interest Posting"));
        interestBlock.add(postSavingsInterestBtn);
        interestBlock.add(postFixedInterestBtn);
        forms.add(interestBlock);

        JPanel loadBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        loadBlock.setBorder(BorderFactory.createTitledBorder("View Accounts"));
        loadBlock.add(new JLabel("Client ID"));
        loadBlock.add(accountsClientIdField);
        loadBlock.add(loadAccountsBtn);
        forms.add(loadBlock);

        accountsOutput.setEditable(false);
        accountsOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(accountsOutput);
        scroll.setBorder(BorderFactory.createTitledBorder("Accounts Output"));

        panel.add(forms, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLoansTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));

        JPanel applyBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        applyBlock.setBorder(BorderFactory.createTitledBorder("Apply Loan"));
        applyBlock.add(new JLabel("Client ID"));
        applyBlock.add(loanClientIdField);
        applyBlock.add(new JLabel("Account ID"));
        applyBlock.add(loanAccountIdField);
        applyBlock.add(new JLabel("Amount"));
        applyBlock.add(loanAmountField);
        applyBlock.add(topUpBox);
        applyBlock.add(applyLoanBtn);
        forms.add(applyBlock);

        JPanel repayBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        repayBlock.setBorder(BorderFactory.createTitledBorder("Repay Loan"));
        repayBlock.add(new JLabel("Loan ID"));
        repayBlock.add(repayLoanIdField);
        repayBlock.add(new JLabel("Account ID"));
        repayBlock.add(repayAccountIdField);
        repayBlock.add(new JLabel("Amount"));
        repayBlock.add(repayAmountField);
        repayBlock.add(repayLoanBtn);
        forms.add(repayBlock);

        JPanel managerBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        managerBlock.setBorder(BorderFactory.createTitledBorder("Pending Loan Approval (Manager)"));
        managerBlock.add(listPendingBtn);
        managerBlock.add(new JLabel("Loan ID"));
        managerBlock.add(pendingLoanIdField);
        managerBlock.add(new JLabel("Remark"));
        managerBlock.add(pendingRemarkField);
        managerBlock.add(approveLoanBtn);
        managerBlock.add(rejectLoanBtn);
        forms.add(managerBlock);

        JPanel viewBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        viewBlock.setBorder(BorderFactory.createTitledBorder("View Loans"));
        viewBlock.add(new JLabel("Client ID"));
        viewBlock.add(loansClientIdField);
        viewBlock.add(loadLoansBtn);
        forms.add(viewBlock);

        loansOutput.setEditable(false);
        loansOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(loansOutput);
        scroll.setBorder(BorderFactory.createTitledBorder("Loans Output"));

        panel.add(forms, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatementsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder("Generate Statements"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        top.add(new JLabel("Account ID"), gbc);
        gbc.gridx = 1;
        top.add(statementAccountIdField, gbc);
        gbc.gridx = 2;
        top.add(accountStatementBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        top.add(new JLabel("Loan ID"), gbc);
        gbc.gridx = 1;
        top.add(statementLoanIdField, gbc);
        gbc.gridx = 2;
        top.add(loanStatementBtn, gbc);

        statementOutput.setEditable(false);
        statementOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(statementOutput);
        scroll.setBorder(BorderFactory.createTitledBorder("Statement Output"));

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormBlock(String title, Object... components) {
        JPanel block = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        block.setBorder(BorderFactory.createTitledBorder(title));
        for (Object component : components) {
            if (component instanceof JTextField textField && textField.getColumns() == 0) {
                textField.setColumns(8);
            }
            block.add((java.awt.Component) component);
        }
        return block;
    }

    private void wireActions() {
        loginBtn.addActionListener(e -> {
            try {
                currentUser = authService.login(usernameField.getText(), new String(passwordField.getPassword()));
                sessionLabel.setText("Logged in as " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
                autoFillForClient();
                applyPermissions();
                showInfo("Login successful.");
            } catch (BankException ex) {
                showError(ex.getMessage());
            }
        });

        logoutBtn.addActionListener(e -> {
            currentUser = null;
            usernameField.setText("");
            passwordField.setText("");
            clearInteractiveFields();
            sessionLabel.setText("Not logged in");
            applyPermissions();
            showInfo("Logged out.");
        });

        openAccountBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.OPERATOR);
            int clientId = parseInt(openClientIdField.getText(), "Client ID");
            AccountType type = (AccountType) openAccountTypeBox.getSelectedItem();
            int accountId = accountService.openAccount(clientId, type);
            showInfo("Account opened. Account ID: " + accountId);
            showAccounts(clientId);
        }));

        depositBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.CLIENT, Role.OPERATOR);
            int accountId = parseInt(depositAccountIdField.getText(), "Account ID");
            enforceClientOwnsAccount(accountId);
            double amount = parseDouble(depositAmountField.getText(), "Amount");
            String channel = (String) depositChannelBox.getSelectedItem();
            accountService.deposit(accountId, amount, channel);
            showInfo("Deposit successful.");
            showAccountBalance(accountId);
        }));

        withdrawBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.CLIENT, Role.OPERATOR);
            int accountId = parseInt(withdrawAccountIdField.getText(), "Account ID");
            enforceClientOwnsAccount(accountId);
            double amount = parseDouble(withdrawAmountField.getText(), "Amount");
            WithdrawalChannel channel = (WithdrawalChannel) withdrawChannelBox.getSelectedItem();
            accountService.withdraw(accountId, amount, channel);
            showInfo("Withdrawal successful.");
            showAccountBalance(accountId);
        }));

        transferBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.CLIENT, Role.OPERATOR);
            int from = parseInt(transferFromField.getText(), "From account");
            int to = parseInt(transferToField.getText(), "To account");
            enforceClientOwnsAccount(from);
            double amount = parseDouble(transferAmountField.getText(), "Amount");
            accountService.transfer(from, to, amount);
            showInfo("Transfer successful.");
            showAccountBalance(from);
        }));

        postSavingsInterestBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.OPERATOR, Role.MANAGER);
            int count = accountService.processMonthlySavingsInterest();
            showInfo("Savings interest posted to " + count + " account(s).");
        }));

        postFixedInterestBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.OPERATOR, Role.MANAGER);
            int count = accountService.processFixedDepositMaturityInterest();
            showInfo("Fixed deposit interest posted to " + count + " account(s).");
        }));

        loadAccountsBtn.addActionListener(e -> runAction(() -> {
            int clientId = currentUser != null && currentUser.getRole() == Role.CLIENT
                ? currentUser.getClientId()
                : parseInt(accountsClientIdField.getText(), "Client ID");
            showAccounts(clientId);
        }));

        applyLoanBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.CLIENT, Role.OPERATOR);
            int clientId = resolveClientId(parseOptionalInt(loanClientIdField.getText()));
            int accountId = parseInt(loanAccountIdField.getText(), "Account ID");
            enforceClientOwnsAccount(accountId);
            double amount = parseDouble(loanAmountField.getText(), "Amount");
            int loanId = loanService.applyLoan(clientId, accountId, amount, topUpBox.isSelected());
            showInfo("Loan application submitted. Loan ID: " + loanId);
            showLoans(clientId);
        }));

        repayLoanBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.CLIENT, Role.OPERATOR);
            int loanId = parseInt(repayLoanIdField.getText(), "Loan ID");
            int accountId = parseInt(repayAccountIdField.getText(), "Account ID");
            enforceClientOwnsAccount(accountId);
            double amount = parseDouble(repayAmountField.getText(), "Amount");
            loanService.repayLoan(loanId, accountId, amount);
            showInfo("Loan repayment posted.");
        }));

        listPendingBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.MANAGER);
            List<Loan> pending = loanService.getPendingLoans();
            loansOutput.setText(formatLoans("Pending Loans", pending));
        }));

        approveLoanBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.MANAGER);
            int loanId = parseInt(pendingLoanIdField.getText(), "Loan ID");
            loanService.approveOrRejectLoan(currentUser.getId(), loanId, true, pendingRemarkField.getText());
            showInfo("Loan approved.");
            List<Loan> pending = loanService.getPendingLoans();
            loansOutput.setText(formatLoans("Pending Loans", pending));
        }));

        rejectLoanBtn.addActionListener(e -> runAction(() -> {
            requireRole(Role.MANAGER);
            int loanId = parseInt(pendingLoanIdField.getText(), "Loan ID");
            loanService.approveOrRejectLoan(currentUser.getId(), loanId, false, pendingRemarkField.getText());
            showInfo("Loan rejected.");
            List<Loan> pending = loanService.getPendingLoans();
            loansOutput.setText(formatLoans("Pending Loans", pending));
        }));

        loadLoansBtn.addActionListener(e -> runAction(() -> {
            int clientId = currentUser != null && currentUser.getRole() == Role.CLIENT
                ? currentUser.getClientId()
                : parseInt(loansClientIdField.getText(), "Client ID");
            showLoans(clientId);
        }));

        accountStatementBtn.addActionListener(e -> runAction(() -> {
            int accountId = parseInt(statementAccountIdField.getText(), "Account ID");
            enforceClientOwnsAccount(accountId);
            statementOutput.setText(accountService.generateAccountStatement(accountId));
        }));

        loanStatementBtn.addActionListener(e -> runAction(() -> {
            int loanId = parseInt(statementLoanIdField.getText(), "Loan ID");
            if (currentUser != null && currentUser.getRole() == Role.CLIENT && currentUser.getClientId() != null) {
                List<Loan> clientLoans = loanService.getClientLoans(currentUser.getClientId());
                boolean owns = clientLoans.stream().anyMatch(loan -> loan.getId() == loanId);
                if (!owns) {
                    throw new BankException("Clients can only view their own loan statements.");
                }
            }
            statementOutput.setText(loanService.generateLoanStatement(loanId));
        }));
    }

    private void autoFillForClient() {
        if (currentUser == null || currentUser.getRole() != Role.CLIENT || currentUser.getClientId() == null) {
            return;
        }
        String id = String.valueOf(currentUser.getClientId());
        openClientIdField.setText(id);
        accountsClientIdField.setText(id);
        loanClientIdField.setText(id);
        loansClientIdField.setText(id);
    }

    private void applyPermissions() {
        boolean loggedIn = currentUser != null;
        Role role = loggedIn ? currentUser.getRole() : null;

        boolean isClient = role == Role.CLIENT;
        boolean isOperator = role == Role.OPERATOR;
        boolean isManager = role == Role.MANAGER;

        loginBtn.setEnabled(!loggedIn);
        usernameField.setEditable(!loggedIn);
        passwordField.setEditable(!loggedIn);
        logoutBtn.setEnabled(loggedIn);

        openAccountBtn.setEnabled(loggedIn && isOperator);

        depositBtn.setEnabled(loggedIn && (isClient || isOperator));
        withdrawBtn.setEnabled(loggedIn && (isClient || isOperator));
        transferBtn.setEnabled(loggedIn && (isClient || isOperator));
        loadAccountsBtn.setEnabled(loggedIn);

        postSavingsInterestBtn.setEnabled(loggedIn && (isOperator || isManager));
        postFixedInterestBtn.setEnabled(loggedIn && (isOperator || isManager));

        applyLoanBtn.setEnabled(loggedIn && (isClient || isOperator));
        repayLoanBtn.setEnabled(loggedIn && (isClient || isOperator));
        loadLoansBtn.setEnabled(loggedIn);

        listPendingBtn.setEnabled(loggedIn && isManager);
        approveLoanBtn.setEnabled(loggedIn && isManager);
        rejectLoanBtn.setEnabled(loggedIn && isManager);

        accountStatementBtn.setEnabled(loggedIn);
        loanStatementBtn.setEnabled(loggedIn);

        loanClientIdField.setEditable(!isClient);
        accountsClientIdField.setEditable(!isClient);
        loansClientIdField.setEditable(!isClient);
    }

    private void clearInteractiveFields() {
        openClientIdField.setText("");
        depositAccountIdField.setText("");
        depositAmountField.setText("");
        withdrawAccountIdField.setText("");
        withdrawAmountField.setText("");
        transferFromField.setText("");
        transferToField.setText("");
        transferAmountField.setText("");
        accountsClientIdField.setText("");

        loanClientIdField.setText("");
        loanAccountIdField.setText("");
        loanAmountField.setText("");
        topUpBox.setSelected(false);
        repayLoanIdField.setText("");
        repayAccountIdField.setText("");
        repayAmountField.setText("");
        pendingLoanIdField.setText("");
        pendingRemarkField.setText("");
        loansClientIdField.setText("");

        statementAccountIdField.setText("");
        statementLoanIdField.setText("");
        accountsOutput.setText("");
        loansOutput.setText("");
        statementOutput.setText("");
    }

    private void showAccounts(int clientId) throws BankException {
        List<Account> accounts = accountService.getAccountsForClient(clientId);
        if (accounts.isEmpty()) {
            accountsOutput.setText("No accounts found for client " + clientId + ".");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Accounts for Client ").append(clientId).append('\n');
        for (Account account : accounts) {
            sb.append(String.format(Locale.US,
                "  Account %-4d | %-13s | Balance: %12s%n",
                account.getId(),
                account.getAccountType(),
                MONEY.format(account.getBalance())));
        }
        accountsOutput.setText(sb.toString());
    }

    private void showLoans(int clientId) throws BankException {
        List<Loan> loans = loanService.getClientLoans(clientId);
        loansOutput.setText(formatLoans("Loans for Client " + clientId, loans));
    }

    private String formatLoans(String title, List<Loan> loans) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append('\n');
        if (loans == null || loans.isEmpty()) {
            sb.append("  No loans found.\n");
            return sb.toString();
        }

        for (Loan loan : loans) {
            sb.append(String.format(Locale.US,
                "  Loan %-4d | %-8s | Requested: %12s | Outstanding: %12s | Top-up: %s%n",
                loan.getId(),
                loan.getStatus(),
                MONEY.format(loan.getRequestedAmount()),
                MONEY.format(loan.getOutstandingBalance()),
                loan.isTopUp() ? "YES" : "NO"));
        }
        return sb.toString();
    }

    private void showAccountBalance(int accountId) {
        try {
            Account account = accountService.getAccount(accountId);
            accountsOutput.setText("Account " + accountId + " current balance: " + MONEY.format(account.getBalance()));
        } catch (BankException ignored) {
            // Optional helper output only.
        }
    }

    private void requireRole(Role... roles) throws BankException {
        if (currentUser == null) {
            throw new BankException("Login first.");
        }
        boolean allowed = Arrays.stream(roles).anyMatch(role -> role == currentUser.getRole());
        if (!allowed) {
            throw new BankException("Action not allowed for role " + currentUser.getRole() + ".");
        }
    }

    private void enforceClientOwnsAccount(int accountId) throws BankException {
        if (currentUser == null || currentUser.getRole() != Role.CLIENT || currentUser.getClientId() == null) {
            return;
        }
        Account account = accountService.getAccount(accountId);
        if (account.getClientId() != currentUser.getClientId()) {
            throw new BankException("Clients can only operate their own accounts.");
        }
    }

    private int resolveClientId(Integer requestedClientId) throws BankException {
        if (currentUser != null && currentUser.getRole() == Role.CLIENT && currentUser.getClientId() != null) {
            return currentUser.getClientId();
        }
        if (requestedClientId == null || requestedClientId <= 0) {
            throw new BankException("Client ID is required.");
        }
        return requestedClientId;
    }

    private Integer parseOptionalInt(String raw) throws BankException {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            throw new BankException("Value must be an integer.");
        }
    }

    private int parseInt(String raw, String fieldName) throws BankException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            throw new BankException(fieldName + " must be a valid integer.");
        }
    }

    private double parseDouble(String raw, String fieldName) throws BankException {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception ex) {
            throw new BankException(fieldName + " must be a valid number.");
        }
    }

    private void runAction(CheckedRunnable action) {
        if (currentUser == null) {
            showError("Login first.");
            return;
        }
        try {
            action.run();
        } catch (BankException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Unexpected error: " + ex.getMessage());
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
