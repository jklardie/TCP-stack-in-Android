package nl.vu.cs.cn;

/**
 * This class ...
 *
 * @author Jeffrey Klardie
 */
public class Log {
    private static final boolean IS_ENABLED = true;

    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(final String tag, final String msg) {
        return (isLoggingEnabled() ? android.util.Log.v(tag, System.currentTimeMillis() + " | " + msg) : 0);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(final String tag, final String msg, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.v(tag, System.currentTimeMillis() + " | " + msg, tr) : 0);
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(final String tag, final String msg) {
        return (isLoggingEnabled() ? android.util.Log.d(tag, System.currentTimeMillis() + " | " + msg) : 0);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(final String tag, final String msg, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.v(tag, System.currentTimeMillis() + " | " + msg, tr) : 0);
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(final String tag, final String msg) {
        return (isLoggingEnabled() ? android.util.Log.i(tag, System.currentTimeMillis() + " | " + msg) : 0);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(final String tag, final String msg, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.i(tag, System.currentTimeMillis() + " | " + msg, tr) : 0);
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(final String tag, final String msg) {
        return (isLoggingEnabled() ? android.util.Log.w(tag, System.currentTimeMillis() + " | " + msg) : 0);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(final String tag, final String msg, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.w(tag, System.currentTimeMillis() + " | " + msg, tr) : 0);
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(final String tag, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.w(tag, tr) : 0);
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(final String tag, final String msg) {
        return (isLoggingEnabled() ? android.util.Log.e(tag, System.currentTimeMillis() + " | " + msg) : 0);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(final String tag, final String msg, final Throwable tr) {
        return (isLoggingEnabled() ? android.util.Log.e(tag, System.currentTimeMillis() + " | " + msg, tr) : 0);
    }

    private static boolean isLoggingEnabled() {
        return IS_ENABLED;
    }

}