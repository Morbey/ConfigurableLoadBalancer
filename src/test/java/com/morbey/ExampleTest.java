package com.morbey;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExampleTest {

    private HttpServer server;
    private MyService myService;

    private HttpServer mockServer;

    @BeforeEach
    public void setUp() throws Exception {
        myService = mock(MyService.class);
        MyController myController = new MyController(myService);

        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/next-server", myController);
        server.setExecutor(Executors.newFixedThreadPool(5));
        server.start();
        System.out.println("My server started on port 8080");

        mockServer = MockServer.startServer(9001);

        AtomicInteger counter = new AtomicInteger(0);

        List<Server> servers = List.of(
                new Server("http://localhost:9001"),
                new Server("http://localhost:9002"),
                new Server("http://localhost:9003"),
                new Server("http://localhost:9004"));

        when(myService.getServer()).thenAnswer((Answer<Server>) invocationOnMock -> servers.get(servers.get(0)));
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
        mockServer.stop(0);
    }

    @Test
    public void testConcurrentRequests() throws Exception {
        int numRequests = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(5); // Simulating 5 threads for 100 users
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < numRequests; i++) {
            futures.add(executorService.submit(() -> {
                URL url = new URL("http://localhost:8080/my-app");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                int responseCode = con.getResponseCode();
                assertEquals(200, responseCode);

                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    return in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    con.disconnect();
                }
            }));
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        int actualRequestCount = 0;

        for (Future<String> future : futures) {
            String response = future.get();
            if (response.contains("success")) {
                actualRequestCount++;
            }
        }

        System.out.println("Server responses: " + actualRequestCount);

        assertEquals(actualRequestCount, numRequests);
    }
}
