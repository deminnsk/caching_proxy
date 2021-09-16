package app.client;

import app.queue.Consumer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Just dummy
 */
public class DummyConsoleClient implements Consumer<String> {

    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public CompletableFuture<Boolean> consumeBatch(List<String> items) {
        var reported = false;
        var result = new CompletableFuture<Boolean>();
        try {
            System.out.println(LocalDateTime.now().format(fmt) + ": " + items);

            // example
            if (items.equals(List.of("error"))) {
                result.completeExceptionally(new Exception("error is incorrect"));
            } else {
                result.complete(true);
            }

            reported = true;
            return result;
        } finally {
            if (!reported) {
                result.complete(false);
            }
        }
    }
}
