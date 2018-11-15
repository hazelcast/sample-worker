package com.hazelcast.examples.worker;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

import static io.undertow.util.Headers.CONTENT_TYPE;
import static java.lang.Runtime.getRuntime;

/**
 * Starts a worker and an HTTP server that delivers memory usage measured every few milliseconds.
 * The HTTP response consists of one timestamped measurement per line.
 * The server delivers the data accumulated since the last request and then forgets it.
 */
public class Worker {
    private static final int NUM_WORKER_THREADS = 4;
    private final Queue<Entry<Long, Long>> q = new ConcurrentLinkedDeque<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage:");
            System.err.println("  Worker <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        System.out.println("Starting worker on port " + port);
        Worker worker = new Worker();
        worker.startWorkerThreads();
        worker.startMonitoringThread();
        Undertow server = worker.httpServer(port);
        server.start();
    }

    private void startWorkerThreads() {
        // mock implementation, in reality it would do something useful
        // here it's just allocating some blocks of memory and releasing them
        for (int i = 0; i < NUM_WORKER_THREADS; i++) {
            Thread t = new Thread(() -> {
                while (true) {
                    byte[] buffer = new byte[ThreadLocalRandom.current().nextInt(16 * 1024)];
                    sleep(ThreadLocalRandom.current().nextInt(50));
                }
            });
            t.setDaemon(true);
            t.setName("worker-thread-" + i);
            t.start();
        }
    }

    private void startMonitoringThread() {
        Thread t = new Thread(() -> {
            while (true) {
                long monitoredValue = getRuntime().totalMemory() - getRuntime().freeMemory();
                q.add(new SimpleImmutableEntry<>(System.currentTimeMillis(), monitoredValue));
                sleep(10);
            }
        });
        t.setName("monitoring-thread");
        t.setDaemon(true);
        t.start();
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private Undertow httpServer(int port) {
        return Undertow.builder()
                       .addHttpListener(port, "localhost")
                       .setHandler(this::handleRequest)
                       .build();
    }

    private void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(CONTENT_TYPE, "text/plain");
        if (q.isEmpty()) {
            return;
        }
        StringBuilder b = new StringBuilder();
        for (int i = q.size(); i > 0; i--) {
            Map.Entry<Long, Long> event = q.remove();
            b.append(event.getKey()).append(' ')
             .append(event.getValue()).append('\n');
        }
        exchange.getResponseSender().send(b.toString());
    }
}