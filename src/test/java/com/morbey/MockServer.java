package com.morbey;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MockServer {

    public static void main(String[] args) throws IOException {
        startServer(9001);
        startServer(9002);
        startServer(9003);
        startServer(9004);
    }

    public static HttpServer startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MockHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Mock server started on port " + port);
        return server;
    }

    static class MockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Response from server on port " + exchange.getLocalAddress().getPort();
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}