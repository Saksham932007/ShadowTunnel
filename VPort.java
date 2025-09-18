package com.yourname.jvn;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VPort {

    private static final Logger LOGGER = Logger.getLogger(VPort.class.getName());
    private static final String TAP_DEVICE_NAME = "j-vpn0";
    private static final int MAX_PACKET_SIZE = 1600;


    private final String serverHost;
    private final int serverPort;

    public VPort(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void start() throws IOException, LastErrorException {
        if (!Platform.isLinux()) {
            throw new UnsupportedOperationException("This VPort implementation only supports Linux.");
        }

        int tapFd = CLibrary.INSTANCE.tap_alloc(TAP_DEVICE_NAME);
        if (tapFd < 0) {
            throw new IOException("Failed to create TAP device");
        }
        LOGGER.info("TAP device created: " + TAP_DEVICE_NAME);

        FileDescriptor fd = new FileDescriptor();
        // This is a bit of a hack to set the file descriptor, requires reflection in newer Java versions
        try {
            java.lang.reflect.Field f = FileDescriptor.class.getDeclaredField("fd");
            f.setAccessible(true);
            f.set(fd, tapFd);
        } catch (Exception e) {
           throw new IOException("Failed to set file descriptor", e);
        }

        FileInputStream tapIn = new FileInputStream(fd);
        FileOutputStream tapOut = new FileOutputStream(fd);

        DatagramSocket socket = new DatagramSocket();
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread to read from TAP and send to VSwitch
        executor.submit(() -> {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (true) {
                try {
                    int len = tapIn.read(buffer);
                    if (len > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, len, serverAddress);
                        socket.send(packet);
                        LOGGER.info("Sent packet from TAP to VSwitch");
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error reading from TAP", e);
                }
            }
        });

        // Thread to read from VSwitch and write to TAP
        executor.submit(() -> {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    tapOut.write(packet.getData(), 0, packet.getLength());
                    LOGGER.info("Received packet from VSwitch and wrote to TAP");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error receiving from VSwitch", e);
                }
            }
        });
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java VPort <server_host> <server_port>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            new VPort(host, port).start();
        } catch (IOException | LastErrorException e) {
            LOGGER.log(Level.SEVERE, "VPort failed to start", e);
        }
    }
}
