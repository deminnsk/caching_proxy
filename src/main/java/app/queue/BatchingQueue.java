package app.queue;


/**
 * Simple async queue, to create use BatchingQueueImpl.builder()
 *
 * @param <E>
 */
public interface BatchingQueue<E> {

    /**
     * Put an element to queue
     *
     * @param element
     *
     * @return true on success, false if no space left or stopped
     */
    boolean offer(E element);

    //... TODO: some other queue methods, just don't need them in this test

    /**
     * Stop executers and so on
     */
    void shutdown();
}
