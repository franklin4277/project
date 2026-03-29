package com.bankapp.service;

import com.bankapp.config.DatabaseManager;
import com.bankapp.dao.AccountDao;
import com.bankapp.dao.LoanDao;
import com.bankapp.dao.TransactionDao;
import com.bankapp.enums.LoanStatus;
import com.bankapp.enums.TransactionType;
import com.bankapp.exception.BankException;
import com.bankapp.model.Account;
import com.bankapp.model.Loan;
import com.bankapp.model.LoanPayment;
import com.bankapp.util.BankConstants;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class LoanService {
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final LoanDao loanDao;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;

    public LoanService(LoanDao loanDao, AccountDao accountDao, TransactionDao transactionDao) {
        this.loanDao = loanDao;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

    public int applyLoan(int clientId, int accountId, double requestedAmount, boolean topUp) throws BankException {
        if (clientId <= 0) {
            throw new BankException("Valid client id is required.");
        }
        if (requestedAmount <= 0) {
            throw new BankException("Requested amount must be greater than zero.");
        }

        try {
            Account account = accountDao.findById(accountId);
            if (account == null || account.getClientId() != clientId) {
                throw new BankException("Loan disbursement account must belong to the client.");
            }

            Loan openLoan = loanDao.findOpenLoanByClient(clientId);

            if (openLoan != null && !topUp) {
                throw new BankException("Client has an existing loan. Use top-up option once eligible.");
            }
            if (openLoan == null && topUp) {
                throw new BankException("Top-up requires an active existing loan.");
            }
            if (topUp && openLoan.getStatus() != LoanStatus.ACTIVE) {
                throw new BankException("Top-up can only be requested for an active loan.");
            }

            Double topUpCharge = null;
            Integer previousLoanId = null;
            String remarks;

            if (topUp) {
                previousLoanId = openLoan.getId();
                double remainingAfterClearingExisting = requestedAmount - openLoan.getOutstandingBalance();
                if (remainingAfterClearingExisting <= 0) {
                    throw new BankException("Top-up amount must exceed existing outstanding loan.");
                }
                topUpCharge = remainingAfterClearingExisting * BankConstants.TOP_UP_CHARGE_RATE;
                remarks = "Top-up requested. Previous loan will be cleared on approval.";
            } else {
                remarks = "Loan requested and awaiting manager approval.";
            }

            try (Connection connection = DatabaseManager.getConnection()) {
                connection.setAutoCommit(false);
                int loanId = loanDao.createLoan(
                    connection,
                    clientId,
                    accountId,
                    requestedAmount,
                    requestedAmount,
                    LoanStatus.PENDING,
                    topUp,
                    topUpCharge,
                    previousLoanId,
                    remarks
                );
                connection.commit();
                return loanId;
            }
        } catch (SQLException e) {
            throw new BankException("Could not apply loan: " + e.getMessage());
        }
    }

    public void approveOrRejectLoan(int managerId, int loanId, boolean approve, String remark) throws BankException {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            Loan loan = loanDao.findById(connection, loanId);
            if (loan == null) {
                throw new BankException("Loan not found.");
            }
            if (loan.getStatus() != LoanStatus.PENDING) {
                throw new BankException("Only pending loans can be actioned.");
            }

            if (!approve) {
                loanDao.updateStatus(connection, loanId, LoanStatus.REJECTED, managerId,
                    remark == null || remark.isBlank() ? "Loan rejected by manager." : remark);
                connection.commit();
                return;
            }

            Account account = accountDao.findById(connection, loan.getAccountId());
            if (account == null) {
                throw new BankException("Disbursement account not found.");
            }

            if (!loan.isTopUp()) {
                account.deposit(loan.getRequestedAmount());
                accountDao.updateBalance(connection, account.getId(), account.getBalance());
                loanDao.updateStatus(connection, loan.getId(), LoanStatus.ACTIVE, managerId,
                    remark == null || remark.isBlank() ? "Loan approved." : remark);

                transactionDao.create(
                    connection,
                    account.getId(),
                    loan.getClientId(),
                    TransactionType.LOAN_DISBURSEMENT,
                    loan.getRequestedAmount(),
                    "SYSTEM",
                    "Loan disbursement",
                    buildReference("LOAN")
                );
                connection.commit();
                return;
            }

            Loan previousLoan = loanDao.findById(connection, loan.getPreviousLoanId());
            if (previousLoan == null || previousLoan.getStatus() != LoanStatus.ACTIVE) {
                throw new BankException("Top-up requires an active previous loan.");
            }

            double clearAmount = previousLoan.getOutstandingBalance();
            double remainingAfterClear = loan.getRequestedAmount() - clearAmount;
            if (remainingAfterClear <= 0) {
                throw new BankException("Top-up amount is too low to clear previous loan.");
            }

            double topUpCharge = loan.getTopUpCharge() == null
                ? remainingAfterClear * BankConstants.TOP_UP_CHARGE_RATE
                : loan.getTopUpCharge();
            double netDisbursement = remainingAfterClear - topUpCharge;
            if (netDisbursement <= 0) {
                throw new BankException("Top-up net disbursement is not valid.");
            }

            loanDao.updateBalanceAndStatus(
                connection,
                previousLoan.getId(),
                0.0,
                LoanStatus.CLOSED,
                "Cleared through top-up loan #" + loan.getId()
            );
            loanDao.addPayment(
                connection,
                previousLoan.getId(),
                previousLoan.getAccountId(),
                clearAmount,
                "Auto-clear by top-up loan #" + loan.getId()
            );

            loanDao.updateStatus(connection, loan.getId(), LoanStatus.ACTIVE, managerId,
                remark == null || remark.isBlank() ? "Top-up approved." : remark);

            account.deposit(netDisbursement);
            accountDao.updateBalance(connection, account.getId(), account.getBalance());

            String reference = buildReference("TOPUP");
            transactionDao.create(
                connection,
                account.getId(),
                loan.getClientId(),
                TransactionType.LOAN_DISBURSEMENT,
                netDisbursement,
                "SYSTEM",
                "Top-up disbursement after clearing previous loan",
                reference
            );
            transactionDao.create(
                connection,
                account.getId(),
                loan.getClientId(),
                TransactionType.CHARGE,
                topUpCharge,
                "SYSTEM",
                "Top-up charge at 10% of amount remaining after clearance",
                reference + "-CHG"
            );

            connection.commit();
        } catch (SQLException e) {
            throw new BankException("Could not action loan: " + e.getMessage());
        }
    }

    public void repayLoan(int loanId, int accountId, double amount) throws BankException {
        if (amount <= 0) {
            throw new BankException("Repayment amount must be greater than zero.");
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            Loan loan = loanDao.findById(connection, loanId);
            if (loan == null) {
                throw new BankException("Loan not found.");
            }
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                throw new BankException("Only active loans can be repaid.");
            }

            Account account = accountDao.findById(connection, accountId);
            if (account == null) {
                throw new BankException("Repayment account not found.");
            }
            if (account.getClientId() != loan.getClientId()) {
                throw new BankException("Repayment account must belong to loan owner.");
            }
            if (amount > loan.getOutstandingBalance()) {
                throw new BankException("Repayment exceeds outstanding balance.");
            }
            if (account.getBalance() < amount) {
                throw new BankException("Insufficient funds for loan repayment.");
            }

            account.withdraw(amount);
            accountDao.updateBalance(connection, accountId, account.getBalance());

            double newOutstanding = loan.getOutstandingBalance() - amount;
            if (newOutstanding <= 0.000001) {
                loanDao.updateBalanceAndStatus(connection, loanId, 0.0, LoanStatus.CLOSED,
                    "Loan fully serviced.");
            } else {
                loanDao.updateBalance(connection, loanId, newOutstanding,
                    "Loan repayment received.");
            }

            loanDao.addPayment(connection, loanId, accountId, amount, "Loan repayment");
            transactionDao.create(
                connection,
                accountId,
                loan.getClientId(),
                TransactionType.LOAN_REPAYMENT,
                amount,
                "SYSTEM",
                "Loan repayment for loan #" + loanId,
                buildReference("LNPAY")
            );

            connection.commit();
        } catch (SQLException e) {
            throw new BankException("Loan repayment failed: " + e.getMessage());
        }
    }

    public List<Loan> getPendingLoans() throws BankException {
        try {
            return loanDao.findPendingLoans();
        } catch (SQLException e) {
            throw new BankException("Could not load pending loans: " + e.getMessage());
        }
    }

    public List<Loan> getClientLoans(int clientId) throws BankException {
        try {
            return loanDao.findByClient(clientId);
        } catch (SQLException e) {
            throw new BankException("Could not load client loans: " + e.getMessage());
        }
    }

    public String generateLoanStatement(int loanId) throws BankException {
        try {
            Loan loan = loanDao.findById(loanId);
            if (loan == null) {
                throw new BankException("Loan not found.");
            }

            List<LoanPayment> payments = loanDao.findPaymentsByLoan(loanId);
            StringBuilder builder = new StringBuilder();
            builder.append("LOAN STATEMENT\n");
            builder.append("Loan ID: ").append(loan.getId()).append("\n");
            builder.append("Client ID: ").append(loan.getClientId()).append("\n");
            builder.append("Status: ").append(loan.getStatus()).append("\n");
            builder.append("Requested Amount: ").append(MONEY.format(loan.getRequestedAmount())).append("\n");
            builder.append("Outstanding Balance: ").append(MONEY.format(loan.getOutstandingBalance())).append("\n");
            builder.append("Top-up: ").append(loan.isTopUp() ? "YES" : "NO").append("\n");
            if (loan.isTopUp()) {
                builder.append("Top-up Charge: ").append(MONEY.format(loan.getTopUpCharge())).append("\n");
                builder.append("Previous Loan ID: ").append(loan.getPreviousLoanId()).append("\n");
            }
            builder.append("Payments:\n");
            if (payments.isEmpty()) {
                builder.append("  No payments posted.\n");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (LoanPayment payment : payments) {
                    builder.append(String.format(Locale.US,
                        "  %s | Amount: %10.2f | Account: %d | %s%n",
                        payment.getCreatedAt().format(formatter),
                        payment.getAmount(),
                        payment.getAccountId(),
                        payment.getNote()));
                }
            }
            return builder.toString();
        } catch (SQLException e) {
            throw new BankException("Could not generate loan statement: " + e.getMessage());
        }
    }

    private static String buildReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
