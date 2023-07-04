package info.kgeorgiy.ja.smirnov.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HelloUDPClient extends AbstractUDPClient {
    private static void sendAndReceive(String prefix, int thread, int request,
                                       DatagramSocket datagramSocket, InetSocketAddress address,
                                       int bufferSize) {
        var message = getMessage(prefix, thread, request);
        System.out.println("Sending:" + message);
        var datagramPacket = new DatagramPacket(new byte[bufferSize], bufferSize, address);

        while (true) {
            try {
                datagramPacket.setData(message.getBytes(StandardCharsets.UTF_8));
                datagramSocket.send(datagramPacket);
                datagramPacket.setData(new byte[bufferSize]);
                datagramSocket.receive(datagramPacket);
                var received = new String(
                        datagramPacket.getData(),
                        datagramPacket.getOffset(),
                        datagramPacket.getLength(),
                        StandardCharsets.UTF_8
                );
                if (isCorrect(received, prefix, thread, request)) {
                    System.out.println("Received:" +
                            received);
                    return;
                }
            } catch (IOException e) {
                System.err.printf("Error while sending %s%n", message);
            }
        }


    }

    private static Runnable threadTask(int thread, InetSocketAddress address, String prefix, int requests) {
        return () -> {
            try (var datagramSocket = new DatagramSocket()) {
                datagramSocket.setSoTimeout(SOCKET_TIME);
                int bufferSize = datagramSocket.getReceiveBufferSize();
                for (int i = 0; i < requests; i++) {
                    sendAndReceive(prefix, thread + 1, i + 1, datagramSocket, address, bufferSize);
                }
            } catch (SocketException e) {
                System.err.printf("Error while creating socket from address: %s%n", address);
            }
        };
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        var threadPool = Executors.newFixedThreadPool(threads);
        try {
            var address = new InetSocketAddress(InetAddress.getByName(host), port);
            for (int i = 0; i < threads; i++) {
                threadPool.submit(threadTask(i, address, prefix, requests));
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host error: " + host);
        }
        UDPUtil.terminationAwait(threadPool, REQUEST_WAIT * threads * requests);
    }

    public static void main(String[] args) {
        new HelloUDPClient().parseArgsAndRun(args);
    }
}