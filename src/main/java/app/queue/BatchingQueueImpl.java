package app.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.lang.Math.min;

public class BatchingQueueImpl<E> implements BatchingQueue<E> {

    private final int size;
    private final int batchSize;
    private final long flushTimeoutMills;
    private final long consumeTimeoutMills;
    private final Consumer<E> consumer;
    private final ThrowableListener throwableListener;
    private final CircuitBreaker circuitBreaker;

    private final ExecutorService executorService;
    private final ScheduledExecutorService flushExecutor = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService consumeTimeoutExecutor = Executors.newScheduledThreadPool(1);

    private final List<E> elements = new ArrayList<>();

    private ScheduledFuture<?> flushFuture;

    private final Object elementsLock = new Object();
    private final Object flushTaskLock = new Object();

    private volatile boolean isRunning = true;

    public static <E> BatchingQueueImplBuilder<E> builder() {
        return new BatchingQueueImplBuilder<>();
    }

    BatchingQueueImpl(int size, int batchSize, long flushTimeoutMills, long consumeTimeoutMills,
                      int consumerParallelism, Consumer<E> consumer, ThrowableListener throwableListener,
                      CircuitBreaker circuitBreaker) {

        this.size = size;
        this.batchSize = batchSize;
        this.flushTimeoutMills = flushTimeoutMills;
        this.consumeTimeoutMills = consumeTimeoutMills;
        this.consumer = consumer;
        this.throwableListener = throwableListener;
        this.circuitBreaker = circuitBreaker;

        this.flushFuture = flushExecutor.scheduleAtFixedRate(this::evalFlush, flushTimeoutMills,
                flushTimeoutMills, TimeUnit.MILLISECONDS);

        this.executorService = Executors.newFixedThreadPool(consumerParallelism);
    }

    @Override
    public boolean offer(E element) {

        List<E> batch = null;

        synchronized (elementsLock) {
            if (elements.size() >= size || !isRunning) {
                return false;
            }
            elements.add(element);

            if ((circuitBreaker == null || circuitBreaker.canAttempt()) && elements.size() >= batchSize) {
                var sublist = elements.subList(0, batchSize);
                batch = new ArrayList<>(sublist);
                sublist.clear();
            }
        }

        if (batch != null) {
            executeConsumer(batch);
            refreshFlushTask();
        }

        return true;
    }

    private void refreshFlushTask() {
        synchronized (flushTaskLock) {
            flushFuture.cancel(false);
            flushFuture = flushExecutor.scheduleAtFixedRate(this::evalFlush, flushTimeoutMills,
                    flushTimeoutMills, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void shutdown() {
        isRunning = false;
        flushExecutor.shutdownNow();
        executorService.shutdownNow();
        consumeTimeoutExecutor.shutdownNow();
    }

    private void evalFlush() {

        List<E> batch = null;

        synchronized (elementsLock) {
            if ((circuitBreaker == null || circuitBreaker.canAttempt()) && !elements.isEmpty()) {
                var sublist = elements.subList(0, min(batchSize, elements.size()));
                batch = new ArrayList<>(sublist);
                sublist.clear();
            }
        }

        if (batch != null)
            executeConsumer(batch);
    }

    private void executeConsumer(final List<E> list) {
        var taskFut = CompletableFuture.supplyAsync(()
                -> consumer.consumeBatch(list), executorService);

        taskFut.thenAcceptAsync(resultFut -> {
            setupTimeout(taskFut, resultFut);
            setupResultCallback(list, resultFut);
        }, executorService);
    }

    private void setupTimeout(final CompletableFuture<CompletableFuture<Boolean>> taskFuture,
                              final CompletableFuture<Boolean> resultFuture) {
        consumeTimeoutExecutor.schedule(() -> {
            if (!resultFuture.isDone()) {
                taskFuture.cancel(true);
                resultFuture.completeExceptionally(new CancellationException("cancelled due to timeout"));
            }
        }, consumeTimeoutMills, TimeUnit.MILLISECONDS);
    }

    private void setupResultCallback(final List<E> list, final CompletableFuture<Boolean> completableFuture) {
        completableFuture.exceptionally(throwable -> {
            if (throwableListener != null)
                throwableListener.onThrowable(throwable);

            return false;
        }).thenAccept(successed -> {
            synchronized (elementsLock) {
                if (successed) {
                    if (circuitBreaker != null) circuitBreaker.success();
                } else {
                    elements.addAll(list);
                    if (circuitBreaker != null) circuitBreaker.failure();
                }
            }
        });
    }
}
