# Bank Application System Website (Java + Spring Boot + JDBC + SQLite)

A simple website for a banking application using a local database (`bank.db`) with:
- Account opening (`SAVINGS`, `FIXED_DEPOSIT`, `CURRENT`)
- Deposits, withdrawals, transfer of funds
- Loan application, loan approval/rejection, repayment, top-up loans
- Account and loan statements
- Role-based users (`CLIENT`, `OPERATOR`, `MANAGER`)

## Core Rules Implemented
- Max 3 accounts per client, one per type.
- Savings interest: 3% monthly (posted by action).
- Fixed deposit interest: 8% posted at maturity (12 months), once.
- One open loan at a time (`PENDING` or `ACTIVE`).
- No new normal loan until existing loan is fully serviced.
- Top-up loan clears active existing loan first.
- Top-up charge = 10% of amount remaining after clearing existing loan.
- Withdrawals above 50,000 are counter only.
- ATM withdrawals capped at 20,000 per day.
- MPESA withdrawals capped at 20,000 per day.
- Common withdrawal charge applies to every withdrawal.

## Local Database
- SQLite via JDBC
- DB file: `bank.db` (created automatically)
- Schema and seed data initialized at startup.
- DB location can be configured with:
  - `BANK_DB_URL` (full JDBC URL), or
  - `BANK_DB_PATH` (path only, app prefixes `jdbc:sqlite:`).

## Seed Users
- `alice / client123` (CLIENT)
- `bob / client123` (CLIENT)
- `operator / operator123` (OPERATOR)
- `manager / manager123` (MANAGER)

## Run Website
1. Build:
   ```bash
   mvn clean compile
   ```
2. Start web server:
   ```bash
   mvn spring-boot:run
   ```
3. Open browser:
   ```
   http://localhost:8080/login
   ```

## Run With Docker (One Command)
1. Start app + local persistent DB volume:
   ```bash
   docker compose up --build
   ```
2. Open browser:
   ```
   http://localhost:8080/login
   ```
3. Stop:
   ```bash
   docker compose down
   ```

Notes:
- SQLite data persists in Docker named volume `bank_data`.
- Container DB path is `/app/data/bank.db`.

## Diagrams (PlantUML)
- `diagrams/use-case-diagram.puml`
- `diagrams/class-diagram.puml`
- `diagrams/activity-loan-application-approval.puml`
- `diagrams/sequence-transfer-funds.puml`

Render diagrams (if PlantUML is installed):
```bash
plantuml diagrams/*.puml
```

## Main Web Files
- Entry: `src/main/java/com/bankapp/BankWebApplication.java`
- Controller: `src/main/java/com/bankapp/web/WebController.java`
- Templates:
  - `src/main/resources/templates/login.html`
  - `src/main/resources/templates/dashboard.html`
- Styling: `src/main/resources/static/styles.css`
- DB setup: `src/main/java/com/bankapp/config/DatabaseManager.java`
