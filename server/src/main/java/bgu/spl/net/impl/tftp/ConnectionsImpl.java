package bgu.spl.net.impl.tftp;


import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class ConnectionsImpl<T> implements Connections<T> {

    ConcurrentHashMap<Integer,ConnectionHandler<T> > con;
    public ConnectionsImpl(){
        con= new ConcurrentHashMap<>();
    }


    public void connect(int connectionId, ConnectionHandler<T> handler) {
        con.putIfAbsent(connectionId,handler);
    }

    public boolean send(int connectionId, T msg) {
        
        if(con.containsKey(connectionId)){

            con.get((Integer) connectionId).send(msg);

            return true;
        }
        return false;
    }

    public void disconnect(int connectionId) {

        if(con.containsKey(connectionId)){
            try {

                con.get((Integer) connectionId).close();
            } catch (IOException e) {
            }
            con.remove((Integer)connectionId);
        }

    }

}
