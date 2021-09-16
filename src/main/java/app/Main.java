package app;

import app.client.DummyConsoleClient;
import app.queue.BatchingQueueImpl;
import app.queue.CircuitBreaker;
import app.server.SimpleTelnetServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        int port = parsePort(args);

        var batchingQueue = BatchingQueueImpl.<String>builder()
                .setSize(10000)
                .setBatchSize(5)
                .setFlushTimeoutMills(5 * 1000)
                .setConsumeTimeoutMills(60 * 1000)
                .setConsumerParallelism(10)
                .setConsumer(new DummyConsoleClient())
                .setThrowableListener(Throwable::printStackTrace)
                .setCircuitBreaker(new CircuitBreaker(3, 10 * 1000))
                .build();

        var server = new SimpleTelnetServer(port, batchingQueue::offer);
        server.start();

        // just example where shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(batchingQueue::shutdown));
    }

    private static int parsePort(String[] args) {
        int port = 10033;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException th) {
                System.out.println("Incorrect port format: " + args[0] + ", using default: " + port);
            }
        }
        return port;
    }
}
