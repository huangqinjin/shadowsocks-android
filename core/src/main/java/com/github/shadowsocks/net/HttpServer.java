package com.github.shadowsocks.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

public class HttpServer extends Thread {

    private final ServerSocket acceptor;
    private final Function<String, String> response;

    public HttpServer(int port, Function<String, String> response) throws IOException {
        acceptor = new ServerSocket(port);
        this.response = response;
        start();
    }

    @Override
    public void run() {
        while (true) {
            try(Socket socket = acceptor.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream()) {
                    @Override
                    public void println() {
                        if ("\n".equals(System.lineSeparator())) write('\r');
                        super.println();
                    }
                }) {
                socket.setSoTimeout(1000);
                String uri = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (uri == null) {
                        if (!line.startsWith("GET /")) break;
                        int i = line.indexOf(' ', 4);
                        if (i < 0) break;
                        uri = line.substring(4, i);
                    }
                    if (line.isEmpty()) {
                        line = response.apply(uri);
                        writer.println("HTTP/1.0 200 OK");
                        writer.println("Content-Length: " + line.length());
                        writer.println();
                        writer.print(line);
                        break;
                    } else {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new HttpServer(8388, uri -> {
                return uri + "\n";
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}