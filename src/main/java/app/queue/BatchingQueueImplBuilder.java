package app.queue;

public class BatchingQueueImplBuilder<E> {

    private int size = Integer.MAX_VALUE;
    private int batchSize = 10;
    private long flushTimeoutMills = Long.MAX_VALUE;
    private long consumeTimeoutMills = 60 * 1000;
    private int consumerParallelism = 5;
    private Consumer<E> consumer = null;
    private ThrowableListener throwableListener = null;
    private CircuitBreaker circuitBreaker = null;

    /**
     * @param size max queue size
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * @param batchSize the queue will execute consumer when reached, cannot be less than 1
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * @param flushTimeoutMills timeout, the queue will execute consumer when reached
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setFlushTimeoutMills(long flushTimeoutMills) {
        this.flushTimeoutMills = flushTimeoutMills;
        return this;
    }

    /**
     * @param consumeTimeoutMills the time to consume batch of elements
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setConsumeTimeoutMills(long consumeTimeoutMills) {
        this.consumeTimeoutMills = consumeTimeoutMills;
        return this;
    }

    /**
     * @param consumerParallelism thread pool size
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setConsumerParallelism(int consumerParallelism) {
        this.consumerParallelism = consumerParallelism;
        return this;
    }

    /**
     * @param consumer callback function, executes when batchSize or flushTimeoutMills reached
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setConsumer(Consumer<E> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * @param throwableListener executed on some error
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setThrowableListener(ThrowableListener throwableListener) {
        this.throwableListener = throwableListener;
        return this;
    }

    /**
     * @param circuitBreaker - CB object
     * @return instance
     */
    public BatchingQueueImplBuilder<E> setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    /**
     * @return the instance of ParallelQueueImpl
     */
    public BatchingQueueImpl<E> build() {

        if (size <= 0)
            throw new IllegalArgumentException("size should be > 0");

        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize should be > 0");

        if (consumer == null)
            throw new IllegalArgumentException("consumer must not be null");

        if (flushTimeoutMills <= 0L)
            throw new IllegalArgumentException("flushTimeoutMills should be > 0");

        if (consumeTimeoutMills <= 0L)
            throw new IllegalArgumentException("consumeTimeoutMills should be > 0");

        if (consumerParallelism <= 0)
            throw new IllegalArgumentException("consumerParallelism should be > 0");

        return new BatchingQueueImpl<>(size, batchSize, flushTimeoutMills,
                consumeTimeoutMills, consumerParallelism, consumer, throwableListener, circuitBreaker);
    }
}