package bgu.spl.net.impl.tftp;
import java.nio.charset.StandardCharsets;


import bgu.spl.net.api.MessagingProtocol;

public class TftpProtocol implements MessagingProtocol<byte[]> {
    private boolean shouldTerminate = false;

    public byte[] process(byte[] msg) {
        byte[] opcode = new byte[2];
        opcode[0] = msg[0];
        opcode[1] = msg[1];

         short op = byteToShort(opcode);
       if(op==0){
        shouldTerminate = true;
       }
        if (op == 4) { // ACK
            byte[] b = { msg[2], msg[3] };
            System.out.println("ACK: " + byteToShort(b));
            if (holder2.opcode == 10)
                shouldTerminate = true;
        }
        if (op == 5) {
            byte[] b = { msg[2], msg[3] };
            System.out.println("ERROR: " + byteToShort(b) );
        }
        if (op == 9) {
            byte b = msg[2];
            String adddel = "delete";
            if (b == 1)
                adddel = "add";
            String s = new String(msg, 3, msg.length - 4, StandardCharsets.UTF_8);
            System.out.println("BCAST: " + adddel + " " + s);
        }
return null;
    }

    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public short byteToShort(byte[] b) {
        return (short) (((short) b[0]) << 8 | ((short) (b[1]) & 0x00ff));
    }
    public byte[] ShortToByte(short a) {
        return new byte[] {(byte)(a>>8),(byte)(a&0xff) };
    }

}