package fi.gtrxac.discord;

import android.app.Application;
import android.os.Build;
import android.util.Log;

/**
 * Created by gtrx on 30.8.2026.
 */

public class App extends Application {
    private static final String TAG = "Discofi";

    public static Account pendingAccount;

    // State for the password login flow (fingerprint -> ticket/login instance ID -> TOTP)
    public static String pendingTicket;
    public static String pendingFingerprint;
    public static String pendingLoginInstanceID;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Application starting on " + Build.MANUFACTURER + " " + Build.MODEL +
            " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK + ")");

        final Thread.UncaughtExceptionHandler previousHandler =
            Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "FATAL: uncaught exception on thread '" + thread.getName() + "'",
                    throwable);

                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "System is low on memory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.w(TAG, "Memory trim requested, level " + level);
        super.onTrimMemory(level);
    }
}
