package com.yourname.jvn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VSwitch {

    private static final Logger LOGGER = Logger.getLogger(VSwitch.class.getName());
    private static final int ETHERNET_HEADER_LENGTH = 14;
    private static final int MAX_PACKET_SIZE = 1600;

    private final int port;
    private final ConcurrentHashMap<String, InetSocketAddress> macTable = new ConcurrentHashMap<>();

    public VSwitch(int port) {
        this.port = port;
    }

    public void start() {
        LOGGER.info("VSwitch starting on port: " + port);
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(socket, packet);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "VSwitch failed", e);
        }
    }

    private void handlePacket(DatagramSocket socket, DatagramPacket packet) {
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        if (data.length < ETHERNET_HEADER_LENGTH) {
            return;
        }

        String sourceMac = getMacAddress(data, 6);
        String destMac = getMacAddress(data, 0);
        InetSocketAddress clientAddress = (InetSocketAddress) packet.getSocketAddress();

        macTable.put(sourceMac, clientAddress);
        LOGGER.info(String.format("Received frame from %s to %s via %s", sourceMac, destMac, clientAddress));
        LOGGER.info("MAC Table: " + macTable);


        if (macTable.containsKey(destMac)) {
            InetSocketAddress targetAddress = macTable.get(destMac);
            sendPacket(socket, data, targetAddress);
            LOGGER.info(String.format("Forwarded frame from %s to %s", sourceMac, destMac));
        } else {
            // Broadcast to all known clients except the source
            macTable.forEach((mac, address) -> {
                if (!address.equals(clientAddress)) {
                    sendPacket(socket, data, address);
                }
            });
            LOGGER.info(String.format("Broadcast frame from %s", sourceMac));
        }
    }

    private void sendPacket(DatagramSocket socket, byte[] data, InetSocketAddress address) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, address);
            socket.send(packet);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send packet to " + address, e);
        }
    }

    private String getMacAddress(byte[] data, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%02x", data[offset + i]));
            if (i < 5) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java VSwitch <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        new VSwitch(port).start();
    }
}
