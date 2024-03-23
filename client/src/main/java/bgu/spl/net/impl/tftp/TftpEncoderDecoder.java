package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
    private short opcode=-1;
    private short size;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        pushByte(nextByte);
        if (len == 2) {
            opcode = (short) (((short) bytes[0]) << 8 | ((short) (bytes[1]) & 0X00ff));
        }
        if(opcode==0){  //not connect DISC
            byte[] result= {0,0};
            len=0;
            return result;
        }
            
        if (opcode == 9) { // BCAST
            if (len > 3 & nextByte == 0) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
        }
        if (opcode == 7 | opcode == 8) { // LOGRQ, DELRQ
            if (len > 2 & nextByte == 0) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
        }
        if ((opcode == 6 | opcode == 10 )& len == 2) { // DIRQ, DISC
            byte[] result = Arrays.copyOf(bytes, len);
            len = 0;
            return result;
        }
        if (opcode == 5) { // ERROR
            if (len > 4 & nextByte == 0) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
        }
        if (opcode == 1 | opcode == 2) { // RRQ, WRQ
            if (nextByte == 0) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
        }
        if (opcode == 4 & len == 4) { // ACK
            byte[] result = Arrays.copyOf(bytes, len);
            len = 0;
            return result;
        }
        if (opcode == 3) { // DATA
            if (len == 4) {
                size = (short) (((short) bytes[2]) << 8 | (short) ((bytes[3]) & 0X00ff));
            }
            if (len == size + 6) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
        }

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        String s = new String(message, StandardCharsets.UTF_8);
        String[] str = s.split(" ");

        if (str[0].equals("LOGRQ")) {
            byte[] temp = s.substring(6).getBytes();
            short opcode7 = 7;
            return createPacket(opcode7, temp);
        }
        if (str[0].equals("DELRQ")) {
            byte[] temp = s.substring(6).getBytes();
            short opcode8 = 8;
            return createPacket(opcode8, temp);
        }
        if (str[0].equals("RRQ")) {
            byte[] temp = s.substring(4).getBytes();
            short opcode1 = 1;
            return createPacket(opcode1, temp);
        }
        if (str[0].equals("WRQ")) {
            byte[] temp = s.substring(4).getBytes();
            short opcode2 = 2;
            return createPacket(opcode2, temp);
        }
        if (str[0].equals("DIRQ")) {
            return new byte[] { 0, 6 };
        }
        if (str[0].equals("DISC")) {
            return new byte[] { 0, 10 };
        }
        return null;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;

    }

    private byte[] createPacket(short newCpcode, byte[] temp) {
        byte[] ans = new byte[temp.length + 3];
        byte[] shortToByte = new byte[] { (byte) (newCpcode >> 8), (byte) (newCpcode & 0Xff) };
        ans[0] = shortToByte[0];
        ans[1] = shortToByte[1];
        ans[ans.length - 1] = 0;
        int j = 0;
        for (int i = 2; i < ans.length - 1; i++) {
            ans[i] = temp[j];
            j++;
        }
        return ans;

    }
}
