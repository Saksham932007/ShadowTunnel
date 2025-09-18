# ShadowTunnel: A Simple Layer 2 VPN in Java

Introduction

ShadowTunnel is a Layer 2 Virtual Private Network (VPN) implemented in Java. It simulates a virtual Ethernet switch, allowing multiple clients to connect to a central server and communicate as if they were on the same local network.

This project is a modernized, Java-based version of a simple L2 VPN, originally written in C and Python. The core concepts remain the same, but the implementation has been updated for better scalability, maintainability, and ease of use.
Architecture

The VPN consists of two main components:

    VSwitch (Virtual Switch): The central server that acts as a virtual Layer 2 switch. It listens for connections from clients, maintains a MAC address table, and forwards Ethernet frames between clients.

    VPort (Virtual Port): The client application that runs on each machine you want to connect to the VPN. It creates a virtual TAP network interface and forwards traffic between the local machine and the VSwitch server over a UDP connection.

How to Build

This project uses Apache Maven for dependency management and building.

    Prerequisites:

        Java 11 or higher

        Apache Maven

        A Linux environment (for the VPort client, as it uses a TAP device)

    Build the project:

    mvn clean package


    This will create a single, executable JAR file in the target/ directory named j-vpn-1.0.0-jar-with-dependencies.jar.

How to Deploy

1. Run the VSwitch Server

On a server with a public IP address:

java -cp target/j-vpn-1.0.0-jar-with-dependencies.jar com.yourname.jvn.VSwitch <SERVER_PORT>

Replace <SERVER_PORT> with the port you want the server to listen on (e.g., 8000). 2. Run the VPort Client

On each client machine you want to connect to the VPN:

sudo java -cp target/j-vpn-1.0.0-jar-with-dependencies.jar com.yourname.jvn.VPort <SERVER_IP> <SERVER_PORT>

    Replace <SERVER_IP> with the public IP of your VSwitch server.

    Replace <SERVER_PORT> with the port the VSwitch is listening on.

This command needs to be run with sudo because creating a TAP network interface requires root privileges. 3. Configure the TAP Interface

After starting the VPort client, a new network interface named j-vpn0 will be created. You need to configure it with an IP address.

On Client 1:

sudo ip addr add 10.10.0.1/24 dev j-vpn0
sudo ip link set j-vpn0 up

On Client 2:

sudo ip addr add 10.10.0.2/24 dev j-vpn0
sudo ip link set j-vpn0 up

4. Test Connectivity

You should now be able to ping between the clients:

From Client 1:

ping 10.10.0.2

From Client 2:

ping 10.10.0.1# ShadowTunnel
A lightweight VPN built from scratch to provide encrypted tunneling and secure internet access.

Congratulations, you have your own L2 VPN running in Java!
