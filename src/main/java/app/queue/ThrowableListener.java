package app.queue;

@FunctionalInterface
public interface ThrowableListener {
    void onThrowable(Throwable th);
}
