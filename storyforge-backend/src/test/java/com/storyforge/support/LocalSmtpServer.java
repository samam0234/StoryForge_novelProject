package com.storyforge.support;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;

/** 외부로 전달하지 않는 루프백 SMTP 수신기. 실제 JavaMail 직렬화와 전송을 검증합니다. */
public final class LocalSmtpServer {
    private final ServerSocket server;
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    public volatile boolean rejectNext;
    public LocalSmtpServer() {
        try { server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress()); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
        Thread.ofPlatform().daemon().start(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);
                    out.print("220 localhost test SMTP\r\n"); out.flush();
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.equals("DATA")) {
                            if (rejectNext) {
                                rejectNext = false;
                                out.print("451 temporarily unavailable\r\n"); out.flush(); continue;
                            }
                            out.print("354 send data\r\n"); out.flush();
                            StringBuilder raw = new StringBuilder();
                            while ((line = in.readLine()) != null && !line.equals(".")) {
                                raw.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                            }
                            messages.add(raw.toString());
                            out.print("250 accepted\r\n");
                        } else if (line.equals("QUIT")) {
                            out.print("221 bye\r\n"); out.flush(); break;
                        } else { out.print("250 localhost\r\n"); }
                        out.flush();
                    }
                } catch (IOException ex) { if (!server.isClosed()) messages.add("SMTP ERROR: " + ex.getClass().getSimpleName()); }
            }
        });
    }
    public int port() { return server.getLocalPort(); }
    public void clear() { messages.clear(); }
    public int size() { return messages.size(); }
    public String body() throws Exception {
        String raw = messages.poll(5, TimeUnit.SECONDS);
        if (raw == null) throw new AssertionError("SMTP message missing");
        return new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII))).getContent().toString();
    }
}
