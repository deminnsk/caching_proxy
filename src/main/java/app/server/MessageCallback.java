package app.server;

@FunctionalInterface
public interface MessageCallback {
    void onMessage(String message);
}
