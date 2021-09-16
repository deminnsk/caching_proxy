package app.queue;

/**
 * The simplified circuit breaker implementation: https://martinfowler.com/bliki/CircuitBreaker.html
 * <p>
 * This CB is not thread safe
 */
public class CircuitBreaker {

    private final int failureThreshold;
    private final long resetTimeoutMills;

    private State state = State.CLOSED;
    private long lastFailureTime = 0;
    private int failureCount = 0;

    /**
     * @param failureThreshold  - allowed failures before open the CB
     * @param resetTimeoutMills - time in mills since last failure to mark the CB as half opened
     */
    public CircuitBreaker(int failureThreshold, int resetTimeoutMills) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMills = resetTimeoutMills;
    }

    private enum State {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    public void success() {
        failureCount = 0;
        lastFailureTime = 0;
        state = State.CLOSED;
    }

    public void failure() {
        failureCount += 1;
        lastFailureTime = System.currentTimeMillis();
    }

    public boolean canAttempt() {
        evalState();
        return state != State.OPEN;
    }

    private void evalState() {
        if (failureCount >= failureThreshold) {
            if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMills) {
                state = State.HALF_OPEN;
            } else {
                state = State.OPEN;
            }
        } else {
            state = State.CLOSED;
        }
    }
}
