package info.kgeorgiy.ja.smirnov.hello;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer extends AbstractUDPServer {
    private ExecutorService receivePool;
    private ExecutorService sendPool;
    private DatagramSocket datagramSocket;

    @Override
    public void start(int port, int threads) {
        receivePool = Executors.newSingleThreadExecutor();
        sendPool = Executors.newFixedThreadPool(threads);
        try {
            datagramSocket = new DatagramSocket(port);
            final Runnable task = () -> {
                try {
                    final int bufferSize = datagramSocket.getReceiveBufferSize();
                    try {
                        while(!Thread.interrupted()) {
                            var datagramPacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                            datagramSocket.receive(datagramPacket);
                            sendPool.submit(() -> {
                                datagramPacket.setData(getResponse(new String(
                                        datagramPacket.getData(),
                                        datagramPacket.getOffset(),
                                        datagramPacket.getLength(),
                                        StandardCharsets.UTF_8
                                )));
                                try {
                                    datagramSocket.send(datagramPacket);
                                } catch (IOException e) {
                                    System.err.println("Error while sending on port: " + port);
                                }
                            });

                        }
                    } catch (IOException e) {
                        System.err.println("Error while receiving on port: " + port);
                    }
                } catch (SocketException e) {
                    System.err.println("Error while getting buffer size on socket " + datagramSocket);
                }
            };
            receivePool.submit(task);
        } catch (SocketException e) {
            System.err.println("Error while creating socket from port: " + port);
        }
        int[] array = new int[]{1,2,3,4,5};
    }

    public static void main(String[] args) {
        new HelloUDPServer().parseArgsAndStart(args);
    }

    @Override
    public void close() {
        datagramSocket.close();
        UDPUtil.terminationAwait(receivePool, THREAD_TERMINATION);
        UDPUtil.terminationAwait(sendPool, THREAD_TERMINATION);
    }
}
