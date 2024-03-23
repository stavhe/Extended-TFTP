package bgu.spl.net.impl.tftp;


import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes = new byte[1 << 10];
    private int len = 0;
  

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (len < 2) {
            pushByte(nextByte);
            if(len==2){
                if(bytes[1]==6 | bytes[1]==10){
                    byte[] result= Arrays.copyOf(bytes, len);
                    len=0;
                    return result;
                }
            }
            return null;
        } else {
            if (bytes[1] > 10 && bytes[1] < 1) {
                byte[] mes = new byte[5];
                mes[0] = 0;
                mes[1] = 5;
                mes[2] = 0;
                mes[3] = 4;
                mes[4] = 0;
                len = 0;
                return mes;
            }
            pushByte(nextByte);
            if (bytes[1] == 3) {
                if (len < 4) {
                    return null;
                } else {
                    byte[] b= {bytes[2], bytes[3]};
                    short s= byteToShort(b);
                    if (len < s + 6) {
                        return null;
                    } else {
                        byte[] result = Arrays.copyOf(bytes, len);
                        len = 0;
                        return result;
                    }
                }
            }
            if (bytes[1] == 4) {
                if (len < 4) {
                    return null;
                } else {
                    byte[] result = Arrays.copyOf(bytes, len);
                    len = 0;
                    return result;
                }
            }
            if (nextByte == 0) {
                byte[] result = Arrays.copyOf(bytes, len);
                len = 0;
                return result;
            }
            
            return null;
         }
        
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }
    public short byteToShort(byte[] b) {
        return (short) (((short) b[0]) << 8 | ((short) (b[1]) &0x00ff));
    }

}