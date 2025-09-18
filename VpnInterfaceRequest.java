package com.yourname.jvn;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

@Structure.FieldOrder({"ifr_name", "ifr_flags"})
public class VpnInterfaceRequest extends Structure {
    public String ifr_name;
    public short ifr_flags;
    
    public VpnInterfaceRequest() {
        super();
        ifr_name = "";
    }
}
