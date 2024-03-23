package bgu.spl.net.impl.tftp;


import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

class holder2 {
    static short blocknum = (short) 1;
    static short opcode = 6;
    static String Rrqfilename;
    static String Wrqfilename;
    static boolean error = false;
    static boolean lastAck = false;
}

public class sendThread implements Runnable {
    OutputStream out;
    InputStream in;
    TftpProtocol protocol;
    TftpEncoderDecoder encdec;
    DataInputStream disin;
    BlockingQueue<Boolean> th;

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);

        while (!protocol.shouldTerminate()) {


            String line = sc.nextLine();
            byte[] bytes = encdec.encode(line.getBytes());
            

           
            if (line.startsWith("WRQ")) {

                holder2.error = false;
                holder2.opcode = 2;
                String currdir = System.getProperty("user.dir");
                holder2.Wrqfilename = line.substring(4);
                Path mypath = Paths.get(currdir, holder2.Wrqfilename);

                if (Files.exists(mypath)) {
                    try {

                        out.write(bytes);
                        out.flush();
                    } catch (IOException ex) {
                    }
                    try {
                        th.take();
                    } catch (InterruptedException e) {
                    }
                    try {
                        disin = new DataInputStream(new FileInputStream(mypath.toFile()));
                    } catch (FileNotFoundException e) {
                    }
                    byte[] data = new byte[512 + 6];
                    int bytesRead;
                    holder2.blocknum = (short) 1;
                    try {
                        while (!holder2.error && ((bytesRead = disin.read(data, 0, 512)) != -1)) {
                            if (bytesRead < 512) {
                                holder2.error = true;
                            }
                            byte[] newdata = new byte[bytesRead + 6];
                            newdata[0] = 0;
                            newdata[1] = 3;
                            byte[] by = ShortToByte((short) bytesRead);
                            newdata[2] = by[0];
                            newdata[3] = by[1];
                            byte[] op = ShortToByte(holder2.blocknum);
                            newdata[4] = op[0];
                            newdata[5] = op[1];
                            int j = 0;
                            for (int i = 6; i < newdata.length; i++) {
                                newdata[i] = data[j];
                                j++;
                            }
                            try {
                                out.write(newdata);
                                out.flush();
                            } catch (IOException ex) {
                            }
                            try {
                                th.take();
                            } catch (InterruptedException e) {
                            }
                            holder2.blocknum++;
                        }
                    } catch (IOException e) {
                    }

                } else {
                    System.out.println("the file does not exist in the client side");
                }

            } else {

                if (line.startsWith("RRQ")) {
                    holder2.Rrqfilename = line.substring(4);
                    String currdir = System.getProperty("user.dir");
                    Path mypath = Paths.get(currdir, holder2.Rrqfilename);
                    if (Files.exists(mypath)) {
                        System.out.println("the file already exist in the client side");

                    } else {
                       
                        holder2.opcode = 1;
                        try {
                            out.write(bytes);
                            out.flush();
                        } catch (IOException ex) {
                        }
                        try {
                            th.take();
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    if (line.startsWith("LOGRQ")) {
                        String username = line.substring(6);
                        if (username.contains("0")) {
                            System.out.println("the user name is ilegal");
                        }
                        else{
                            try {
                                out.write(bytes);
                                out.flush();
                            } catch (IOException ex) {
                            }
                             
                            try {
                                th.take();
                            } catch (InterruptedException e) {
                            }
                        }

                    } else {

                        if (line.startsWith("DIRQ"))
                            holder2.opcode = 6;
                        if (line.startsWith("DISC"))
                            holder2.opcode = 10;

                        try {
                            out.write(bytes);
                            out.flush();
                        } catch (IOException ex) {
                        }
                        try {
                            th.take();
                        } catch (InterruptedException e) {
                        }

                    }
                }
            }

        }
        sc.close();

    }

    void start(OutputStream out, InputStream in, TftpProtocol protocol,
            TftpEncoderDecoder encdec, BlockingQueue<Boolean> th) {
        this.out = out;
        this.in = in;
        this.protocol = protocol;
        this.encdec = encdec;
        this.th = th;

    }

    public byte[] ShortToByte(short a) {
        return new byte[] { (byte) (a >> 8), (byte) (a & 0xff) };
    }
}
