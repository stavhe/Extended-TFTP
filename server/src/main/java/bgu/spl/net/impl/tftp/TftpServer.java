package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

public class TftpServer {

    public static void main(String[] args){
        if(args.length!=1)
        System.out.println("invalid port");
        else{
        Server.threadPerClient(
            Integer.parseInt(args[0]),
            () -> new TftpProtocol(),
            TftpEncoderDecoder::new
        ).serve();
        }
    }
    
}
