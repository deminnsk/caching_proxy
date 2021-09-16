package app.queue;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Consumer<I> {
    /**
     * @param items the batched items from queue
     *
     * @return result future, commit result, true for success processing, false to retry
     */
    CompletableFuture<Boolean> consumeBatch(List<I> items);
}
