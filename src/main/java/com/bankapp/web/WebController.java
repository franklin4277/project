package com.bankapp.web;

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
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class WebController {
    private static final String SESSION_USER = "sessionUser";

    private final AuthService authService;
    private final AccountService accountService;
    private final LoanService loanService;

    public WebController(AuthService authService, AccountService accountService, LoanService loanService) {
        this.authService = authService;
        this.accountService = accountService;
        this.loanService = loanService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return getSessionUser(session) == null ? "redirect:/login" : "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (getSessionUser(session) != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = authService.login(username, password);
            session.setAttribute(SESSION_USER, user);
            redirectAttributes.addFlashAttribute("success", "Login successful.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Logged out.");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Integer accountsClientId,
                            @RequestParam(required = false) Integer loansClientId,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("withdrawalChannels", WithdrawalChannel.values());
        model.addAttribute("isClient", user.getRole() == Role.CLIENT);
        model.addAttribute("isOperator", user.getRole() == Role.OPERATOR);
        model.addAttribute("isManager", user.getRole() == Role.MANAGER);

        Integer effectiveAccountsClientId = user.getRole() == Role.CLIENT ? user.getClientId() : accountsClientId;
        Integer effectiveLoansClientId = user.getRole() == Role.CLIENT ? user.getClientId() : loansClientId;

        model.addAttribute("accountsClientId", effectiveAccountsClientId);
        model.addAttribute("loansClientId", effectiveLoansClientId);

        try {
            List<Account> accounts = effectiveAccountsClientId == null
                ? new ArrayList<>()
                : accountService.getAccountsForClient(effectiveAccountsClientId);
            model.addAttribute("accounts", accounts);

            List<Loan> loans = effectiveLoansClientId == null
                ? new ArrayList<>()
                : loanService.getClientLoans(effectiveLoansClientId);
            model.addAttribute("loans", loans);

            if (user.getRole() == Role.MANAGER) {
                model.addAttribute("pendingLoans", loanService.getPendingLoans());
            } else {
                model.addAttribute("pendingLoans", new ArrayList<Loan>());
            }
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }

        return "dashboard";
    }

    @PostMapping("/accounts/open")
    public String openAccount(@RequestParam Integer clientId,
                              @RequestParam AccountType accountType,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.OPERATOR);

            int accountId = accountService.openAccount(clientId, accountType);
            redirectAttributes.addFlashAttribute("success", "Account opened. Account ID: " + accountId);
            return "redirect:/dashboard?accountsClientId=" + clientId;
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/accounts/deposit")
    public String deposit(@RequestParam Integer accountId,
                          @RequestParam Double amount,
                          @RequestParam String channel,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.CLIENT, Role.OPERATOR);
            enforceAccountOwnershipForClient(user, accountId);

            accountService.deposit(accountId, amount, channel);
            redirectAttributes.addFlashAttribute("success", "Deposit successful.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/accounts/withdraw")
    public String withdraw(@RequestParam Integer accountId,
                           @RequestParam Double amount,
                           @RequestParam WithdrawalChannel channel,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.CLIENT, Role.OPERATOR);
            enforceAccountOwnershipForClient(user, accountId);

            accountService.withdraw(accountId, amount, channel);
            redirectAttributes.addFlashAttribute("success", "Withdrawal successful.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/accounts/transfer")
    public String transfer(@RequestParam Integer fromAccountId,
                           @RequestParam Integer toAccountId,
                           @RequestParam Double amount,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.CLIENT, Role.OPERATOR);
            enforceAccountOwnershipForClient(user, fromAccountId);

            accountService.transfer(fromAccountId, toAccountId, amount);
            redirectAttributes.addFlashAttribute("success", "Transfer successful.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/accounts/post-savings-interest")
    public String postSavingsInterest(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.OPERATOR, Role.MANAGER);

            int posted = accountService.processMonthlySavingsInterest();
            redirectAttributes.addFlashAttribute("success", "Savings interest posted to " + posted + " account(s).");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/accounts/post-fixed-interest")
    public String postFixedInterest(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.OPERATOR, Role.MANAGER);

            int posted = accountService.processFixedDepositMaturityInterest();
            redirectAttributes.addFlashAttribute("success", "Fixed interest posted to " + posted + " account(s).");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/loans/apply")
    public String applyLoan(@RequestParam Integer clientId,
                            @RequestParam Integer accountId,
                            @RequestParam Double requestedAmount,
                            @RequestParam(defaultValue = "false") boolean topUp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.CLIENT, Role.OPERATOR);

            int effectiveClientId = resolveClientId(user, clientId);
            int loanId = loanService.applyLoan(effectiveClientId, accountId, requestedAmount, topUp);
            redirectAttributes.addFlashAttribute("success", "Loan request submitted. Loan ID: " + loanId);
            return "redirect:/dashboard?loansClientId=" + effectiveClientId;
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/loans/action")
    public String approveRejectLoan(@RequestParam Integer loanId,
                                    @RequestParam String decision,
                                    @RequestParam(required = false) String remark,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.MANAGER);

            boolean approve = "approve".equalsIgnoreCase(decision);
            loanService.approveOrRejectLoan(user.getId(), loanId, approve, remark);
            redirectAttributes.addFlashAttribute("success", approve ? "Loan approved." : "Loan rejected.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/loans/repay")
    public String repayLoan(@RequestParam Integer loanId,
                            @RequestParam Integer accountId,
                            @RequestParam Double amount,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            requireRole(user, Role.CLIENT, Role.OPERATOR);
            enforceAccountOwnershipForClient(user, accountId);

            loanService.repayLoan(loanId, accountId, amount);
            redirectAttributes.addFlashAttribute("success", "Loan repayment successful.");
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/statements/account")
    public String accountStatement(@RequestParam Integer accountId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            enforceAccountOwnershipForClient(user, accountId);
            String statement = accountService.generateAccountStatement(accountId);
            redirectAttributes.addFlashAttribute("statementTitle", "Account Statement");
            redirectAttributes.addFlashAttribute("statementText", statement);
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/statements/loan")
    public String loanStatement(@RequestParam Integer loanId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = requireLoggedIn(session);
            if (user.getRole() == Role.CLIENT && user.getClientId() != null) {
                List<Loan> myLoans = loanService.getClientLoans(user.getClientId());
                boolean isOwnLoan = myLoans.stream().anyMatch(loan -> loan.getId() == loanId);
                if (!isOwnLoan) {
                    throw new BankException("Clients can only view their own loan statements.");
                }
            }

            String statement = loanService.generateLoanStatement(loanId);
            redirectAttributes.addFlashAttribute("statementTitle", "Loan Statement");
            redirectAttributes.addFlashAttribute("statementText", statement);
            return "redirect:/dashboard";
        } catch (BankException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    private User requireLoggedIn(HttpSession session) throws BankException {
        User user = getSessionUser(session);
        if (user == null) {
            throw new BankException("Login first.");
        }
        return user;
    }

    private void requireRole(User user, Role... allowed) throws BankException {
        boolean ok = Arrays.stream(allowed).anyMatch(role -> role == user.getRole());
        if (!ok) {
            throw new BankException("Action not allowed for role " + user.getRole() + ".");
        }
    }

    private int resolveClientId(User user, Integer requestedClientId) throws BankException {
        if (user.getRole() == Role.CLIENT) {
            return user.getClientId();
        }
        if (requestedClientId == null || requestedClientId <= 0) {
            throw new BankException("Client ID is required.");
        }
        return requestedClientId;
    }

    private void enforceAccountOwnershipForClient(User user, int accountId) throws BankException {
        if (user.getRole() != Role.CLIENT) {
            return;
        }

        Account account = accountService.getAccount(accountId);
        if (user.getClientId() == null || account.getClientId() != user.getClientId()) {
            throw new BankException("Clients can only operate their own accounts.");
        }
    }

    private User getSessionUser(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof User ? (User) value : null;
    }
}
