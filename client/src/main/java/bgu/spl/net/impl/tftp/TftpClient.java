package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class TftpClient {

    public static void main(String[] args) {

        if (args.length == 0) {
            args = new String[] { "localhost", "hello" };
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }

        // BufferedReader and BufferedWriter automatically using UTF-8 encoding
        try (Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
                InputStream in = sock.getInputStream();
                OutputStream out =((sock.getOutputStream()))) {

            TftpProtocol protocol = new TftpProtocol();
            TftpEncoderDecoder encdec = new TftpEncoderDecoder();
            BlockingQueue<Boolean> th= new LinkedBlockingQueue<>();

            readThread read = new readThread();
            read.start(out, in, protocol, encdec, th);
            Thread readth = new Thread(read);
            readth.start();

            sendThread send = new sendThread();
            send.start(out, in, protocol, encdec, th);
            Thread sendth = new Thread(send);
            sendth.start();

            try {
                readth.join();
                sendth.join();
            } catch (InterruptedException e) {}
 
        } catch (IOException e) {
        }
    }
}
