package com.bankapp.util;

public final class BankConstants {
    private BankConstants() {
    }

    public static final double SAVINGS_MONTHLY_INTEREST_RATE = 0.03;
    public static final double FIXED_DEPOSIT_INTEREST_RATE = 0.08;
    public static final int FIXED_DEPOSIT_PERIOD_MONTHS = 12;
    public static final double WITHDRAWAL_CHARGE = 50.0;
    public static final double COUNTER_WITHDRAWAL_MIN_FOR_LARGE = 50_000.0;
    public static final double DIGITAL_WITHDRAWAL_DAILY_CAP = 20_000.0;
    public static final double TOP_UP_CHARGE_RATE = 0.10;
}
