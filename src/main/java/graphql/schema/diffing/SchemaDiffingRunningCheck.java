package graphql.schema.diffing;

import java.util.concurrent.atomic.AtomicBoolean;

class SchemaDiffingRunningCheck {
    private final AtomicBoolean wasStopped = new AtomicBoolean(false);

    void check() {
        if (Thread.interrupted()) {
            throw new SchemaDiffingCancelledException(true);
        }
        if (wasStopped.get()) {
            throw new SchemaDiffingCancelledException(false);
        }
    }

    void stop() {
        wasStopped.set(true);
    }
}
