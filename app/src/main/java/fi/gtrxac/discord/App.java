package fi.gtrxac.discord;

/**
 * Created by gtrx on 30.8.2026.
 */

public class App {
    public static Account pendingAccount;

    // State for the password login flow (fingerprint -> ticket -> TOTP)
    public static String pendingTicket;
    public static String pendingFingerprint;
}
