package com.yourname.jvn;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

public interface CLibrary extends Library {
    CLibrary INSTANCE = Native.load("c", CLibrary.class);

    int open(String path, int flags);
    int ioctl(int fd, int request, VpnInterfaceRequest ifr) throws LastErrorException;
    int close(int fd);

    default int tap_alloc(String devName) {
        int fd = open("/dev/net/tun", 2 /* O_RDWR */);
        if (fd < 0) {
            return fd;
        }

        VpnInterfaceRequest ifr = new VpnInterfaceRequest();
        ifr.ifr_name = devName;
        ifr.ifr_flags = 0x1002; // IFF_TAP | IFF_NO_PI

        try {
            ioctl(fd, 0x400454ca, ifr); // TUNSETIFF
        } catch (LastErrorException e) {
            close(fd);
            throw e;
        }
        return fd;
    }
}
