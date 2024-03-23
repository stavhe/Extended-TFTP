package bgu.spl.net.impl.tftp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class readThread implements Runnable {
    private short expblocknum = (short) 1;
    private DataOutputStream disout;
    private String print = "";

    OutputStream out;
    InputStream in;
    TftpProtocol protocol;
    TftpEncoderDecoder encdec;
    BlockingQueue<Boolean> th;

    public void run() {
        while (!protocol.shouldTerminate()) {
            int read;
            byte[] ans = null;
            try {
                if ((read = in.read()) != -1)
                    ans = encdec.decodeNextByte((byte) read);
            } catch (IOException e) {
            }
            if (ans != null) {
                byte[] op = Arrays.copyOfRange(ans, 0, 2);
                short getopcode = byteToShort(op);
                if (getopcode == 5) {
                    holder2.error = true;
                    protocol.process(ans);
                    try {
                        th.put(true);
                    } catch (InterruptedException e) {
                    }

                }
                if (getopcode == 3) { // data
                    byte[] block = { ans[4], ans[5] };
                    short toblock = byteToShort(block);
                    if (holder2.opcode == 1) { // RRQ
                        if (expblocknum == toblock) {
                            if (expblocknum == 1) {
                                String currdir = System.getProperty("user.dir");
                                Path mypath = Paths.get(currdir, holder2.Rrqfilename);
                                try {
                                    Files.createFile(mypath);
                                } catch (IOException e) {
                                }
                                try {
                                    disout = new DataOutputStream(new FileOutputStream(mypath.toFile()));
                                } catch (FileNotFoundException e) {
                                }
                            }
                            byte[] data = Arrays.copyOfRange(ans, 6, ans.length);
                            try {
                                disout.write(data);
                            } catch (IOException e) {
                            }
                            byte[] ack = { 0, 4, ans[4], ans[5] };
                            expblocknum++;
                            try {
                                out.write(ack);
                                out.flush();
                            } catch (IOException e) {
                            }
                            if (data.length < 512) {
                                expblocknum = 1;
                                System.out.println("RRQ " + holder2.Rrqfilename + " complete");
                                try {
                                    th.put(true);
                                } catch (InterruptedException e) {
                                }
                            }
                        }

                    }
                    if (holder2.opcode == 6) { // DIRQ

                        if (expblocknum == toblock) {
                            byte[] dirq = Arrays.copyOfRange(ans, 6, ans.length);
                            List<Byte> addbyte = new ArrayList<>();
                            for (int i = 0; i < dirq.length; i++) {
                                if (dirq[i] != 0) {
                                    addbyte.add(dirq[i]);
                                }
                                if (dirq[i] == 0 | i == dirq.length - 1) {
                                    byte[] temp = new byte[addbyte.size()];
                                    int j = 0;
                                    for (byte b : addbyte) {
                                        temp[j] = b;
                                        j++;
                                    }
                                    String s = new String(temp, StandardCharsets.UTF_8);
                                    print = print + s + System.lineSeparator();
                                    addbyte.clear();
                                }
                            }
                            byte[] ack = { 0, 4, ans[4], ans[5] };
                            expblocknum++;
                            try {
                                out.write(ack);
                                out.flush();
                            } catch (IOException e) {
                            }
                            if (ans.length < 512) {
                                System.out.println(print);
                                print = "";
                                expblocknum = 1;
                                try {
                                    th.put(true);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }

                }
                if (getopcode == 4) { // ACK
                    int numOfPackets = 0;
                    if (ans[3] != 0 || ans[2] != 0) { // WRQ
                        byte[] block = { ans[2], ans[3] };
                        short s = byteToShort(block);
                        if (holder2.blocknum != s) {
                            System.out.println("not correct blocknumber");
                            holder2.error = true;
                        }
                        String currdir = System.getProperty("user.dir");
                    File file = new File(currdir + "/" + holder2.Wrqfilename);
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        byte[] fileBytes = new byte[(int) file.length()];
                        int numOfBytes = inputStream.read(fileBytes);
                        numOfPackets = (int) Math.ceil(numOfBytes / 512.0);
                        if (numOfPackets == numOfBytes / 512) {
                            numOfPackets = numOfPackets + 1;
                        }
                    }
                    
                     catch (IOException e) {
                    }
                }
                    protocol.process(ans);

                    if (ans[3] != 0 || ans[2] != 0) {
                        if (holder2.blocknum == (short) numOfPackets) {
                            System.out.println("WRQ " + holder2.Wrqfilename + " complete");
                        }
                    }

                    try {
                        th.put(true);
                    } catch (InterruptedException e) {
                    }

                }
                
                if (getopcode == 9) {
                    protocol.process(ans);

                }
                if (getopcode == 0) {
                    protocol.process(ans);
                    try {
                        th.put(true);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

    }

    void start(OutputStream out, InputStream in, TftpProtocol protocol,
            TftpEncoderDecoder encdec, BlockingQueue<Boolean> th) {
        this.out = out;
        this.in = in;
        this.protocol = protocol;
        this.encdec = encdec;
        this.th = th;

    }

    public short byteToShort(byte[] b) {
        return (short) (((short) b[0]) << 8 | ((short) (b[1]) & 0x00ff));
    }

}
