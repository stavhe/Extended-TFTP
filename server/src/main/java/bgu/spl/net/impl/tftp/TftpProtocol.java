package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

import java.util.concurrent.ConcurrentHashMap;

class holder {
    static ConcurrentHashMap<Integer, Boolean> idsLogin = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, String> usernames = new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private boolean disc = false;

    private int connectionId;
    private Connections<byte[]> connections;
    private short blocknum = -1;
    private DataInputStream disin;
    private DataOutputStream disout;
    private byte[] filename;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;

    }

    @Override
    public void process(byte[] message) {

        if (message[1] == 5) {
            connections.send(connectionId, message);
        } else {
            if (message[1] == 7) {
                byte[] username = Arrays.copyOfRange(message, 2, message.length);
                String user = new String(username, StandardCharsets.UTF_8);

                if (!holder.idsLogin.containsKey(connectionId)) {
                    if (holder.usernames.containsValue(user)) {
                        byte[] mes = error(0);
                        connections.send(this.connectionId, mes);
                    } else {
                        holder.usernames.put(connectionId, user);
                        holder.idsLogin.put(this.connectionId, true);
                        byte[] ack = { 0, 4, 0, 0 };
                        connections.send(connectionId, ack);
                    }
                } else {
                    byte[] mes = error(7);
                    connections.send(this.connectionId, mes);
                }

            } else {
                if (message[1] == 10) {
                    if (holder.idsLogin.containsKey(connectionId)) {
                        byte[] ack = { 0, 4, 0, 0 };
                        connections.send(connectionId, ack);
                        shouldTerminate = true;
                    } else {
                        byte[] ack = { 0, 0};
                        connections.send(connectionId, ack);
                        shouldTerminate = true;
                        disc = true;
                    }
                } else {
                    if (!holder.idsLogin.containsKey(connectionId)) {
                        byte[] mes = error(6);
                        connections.send(this.connectionId, mes);
                    } else {
                        if (message[1] == 8) {
                            filename = Arrays.copyOfRange(message, 2, message.length - 1);
                            String stfilename = new String(filename, StandardCharsets.UTF_8);
                            String uploadDirectory = "./Files";
                            Path fullPath = Paths.get(uploadDirectory, stfilename);
                            if (Files.exists(fullPath)) {
                                try {
                                    Files.delete(fullPath);
                                } catch (IOException e) {
                                }
                                byte[] ack = { 0, 4, 0, 0 };
                                connections.send(connectionId, ack);
                                byte[] mes = new byte[4 + filename.length];
                                int j = 0;
                                for (int i = 3; i < mes.length - 1; i++) {
                                    mes[i] = filename[j];
                                    j++;
                                }
                                mes[0] = 0;
                                mes[1] = 9;
                                mes[2] = 0;
                                mes[mes.length - 1] = 0;
                                for (Integer k : holder.idsLogin.keySet()) {
                                    connections.send(k, mes);
                                }
                            } else {
                                byte[] mes = error(1);
                                connections.send(this.connectionId, mes);
                            }
                        }

                        if (message[1] == 1) {

                            filename = Arrays.copyOfRange(message, 2, message.length - 1);
                            String stfilename = new String(filename, StandardCharsets.UTF_8);
                            String myDirectory = "./Files";

                           
                            Path myPath = Paths.get(myDirectory, stfilename);

                            try {
                                disin = new DataInputStream(new FileInputStream(myPath.toFile()));
                            } catch (FileNotFoundException e) {
                            }
                            if (Files.exists(myPath)) {
                                byte[] data = new byte[512];
                                int bytesRead;
                                blocknum = 1;
                                try {
                                    if ((bytesRead = disin.read(data, 0, 512)) != -1) {
                                        byte[] newdata = new byte[bytesRead + 6];
                                        newdata[0] = 0;
                                        newdata[1] = 3;
                                        byte[] by = ShortToByte((short) bytesRead);
                                        newdata[2] = by[0];
                                        newdata[3] = by[1];
                                        byte[] b = ShortToByte(blocknum);
                                        newdata[4] = b[0];
                                        newdata[5] = b[1];
                                        int j = 0;
                                        for (int i = 6; i < newdata.length; i++) {
                                            newdata[i] = data[j];
                                            j++;
                                        }
                                        connections.send(connectionId, newdata);
                                    }
                                } catch (IOException e) {
                                }

                            } else {
                                byte[] mes = error(1);
                                connections.send(this.connectionId, mes);
                            }
                        }
                        if (message[1] == 4) {
                            byte[] o = { message[2], message[3] };
                            short s = byteToShort(o);
                            if (s == blocknum) {
                                blocknum++;
                                byte[] data = new byte[512];
                                int bytesRead;
                                try {
                                    if ((bytesRead = disin.read(data, 0, 512)) != -1) {
                                        byte[] newdata = new byte[bytesRead + 6];
                                        newdata[0] = 0;
                                        newdata[1] = 3;
                                        byte[] by = ShortToByte((short) bytesRead);
                                        newdata[2] = by[0];
                                        newdata[3] = by[1];
                                        byte[] b = ShortToByte(blocknum);
                                        newdata[4] = b[0];
                                        newdata[5] = b[1];
                                        int j = 0;
                                        for (int i = 6; i < newdata.length; i++) {
                                            newdata[i] = data[j];
                                            j++;
                                        }
                                        connections.send(connectionId, newdata);
                                    }
                                } catch (IOException e) {
                                }
                            } else {
                                byte[] mes = error(0);
                                connections.send(this.connectionId, mes);
                            }
                        }
                        
                        if (message[1] == 2) {

                            filename = Arrays.copyOfRange(message, 2, message.length - 1);
                            String stfilename = new String(filename, StandardCharsets.UTF_8);
                            String myDirectory = "./Files";
                            String tempDirectory = "./TempFiles";
                            
                            Path mypath = Paths.get(myDirectory, stfilename);

                            
                            if (Files.exists(mypath)) {
                                byte[] mes = error(5);
                                connections.send(this.connectionId, mes);
                            } else {
                                Path tempPath1 = Paths.get(tempDirectory);
                                Path tempPath2 = Paths.get(tempDirectory, stfilename);
                                if (!Files.exists(tempPath1)) {
                                    try {
                                        Files.createDirectory(tempPath1);
                                    } catch (IOException e) {
                                    }
                                }
                                if (!Files.exists(tempPath2)) {
                                    try {
                                        Files.createFile(tempPath2);
                                    } catch (IOException e) {
                                    }
                                }

                                try {
                                    disout = new DataOutputStream(new FileOutputStream(tempPath2.toFile()));
                                } catch (FileNotFoundException e) {
                                    System.err.println("Error: File not found: " + e.getMessage());
                                    e.printStackTrace();
                                }
                                byte[] ack= {0,4,0,0};
                                connections.send(connectionId, ack);
                            }
                        }
                        if (message[1] == 3) {

                            byte[] data = Arrays.copyOfRange(message, 6, message.length);
                        
                            byte[] block = { message[4], message[5] };
                            blocknum = byteToShort(block);
                           
                            try {
                                disout.write(data);
                            } catch (IOException e) {
                            }
                            byte[] ack = { 0, 4, message[4], message[5] };
                            connections.send(connectionId, ack);
                            byte[] by = { message[2], message[3] };
                            short sh = byteToShort(by);
                            if (sh < 512) {
                                String stfilename = new String(filename, StandardCharsets.UTF_8);
                                String myDirectory = "./Files";
                                String tempDirectory = "./TempFiles";
                                Path mypath = Paths.get(myDirectory, stfilename);
                                Path tempPath2 = Paths.get(tempDirectory, stfilename);
                                try {
                                    Files.move(tempPath2, mypath);
                                } catch (IOException e) {
                                }
                                byte[] mes = new byte[4 + filename.length];
                                int j = 0;
                                for (int i = 3; i < mes.length - 1; i++) {
                                    mes[i] = filename[j];
                                    j++;
                                }
                                mes[0] = 0;
                                mes[1] = 9;
                                mes[2] = 1;
                                mes[mes.length - 1] = 0;
                                for (Integer k : holder.idsLogin.keySet()) {
                                    connections.send(k, mes);
                                }
                            }
                        }
                        if (message[1] == 6) {
                            String directoryPath = "./Files";
                            File directory = new File(directoryPath);
                            File[] files = directory.listFiles();
                            List<Byte> bytes = new ArrayList<>();
                            if (files != null) {
                                for (File file : files) {
                                    String currentfilename = file.getName();
                                    byte[] curr = currentfilename.getBytes();
                                    for (byte b : curr) {
                                        bytes.add(b);
                                    }
                                    bytes.add((byte) 0);
                                }
                            }
                            byte[] mes = new byte[bytes.size()];
                            Iterator<Byte> iter = bytes.iterator();
                            int i = 0;
                            while (iter.hasNext()) {
                                mes[i] = iter.next();
                                i++;
                            }
                            disin = new DataInputStream(new ByteArrayInputStream(mes));
                            byte[] data;
                            int index;
                            if (mes.length < 512) {
                                data = new byte[mes.length + 6];
                                index = mes.length;
                            } else {
                                data = new byte[512 + 6];
                                index = 512;
                            }
                            int bytesRead;
                            blocknum = 1;

                            try {
                                if ((bytesRead = disin.read(data, 6, index)) != -1) {
                                    byte[] s = ShortToByte((short) bytesRead);
                                    data[0] = 0;
                                    data[1] = 3;
                                    data[2] = s[0];
                                    data[3] = s[1];
                                    byte[] b = ShortToByte(blocknum);
                                    data[4] = b[0];
                                    data[5] = b[1];

                                    connections.send(connectionId, data);
                                } else {
                                    if (mes.length == 0) {
                                        data[0] = 0;
                                        data[1] = 3;
                                        data[2] = 0;
                                        data[3] = 0;
                                        byte[] b = ShortToByte(blocknum);
                                        data[4] = b[0];
                                        data[5] = b[1];

                                        connections.send(connectionId, data);
                                    }
                                }
                            } catch (IOException e) {
                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldTerminate() {
        if (shouldTerminate) {
            if (!disc) {
                holder.idsLogin.remove(this.connectionId);
                holder.usernames.remove(connectionId);
            }
            this.connections.disconnect(this.connectionId);
        }
        return shouldTerminate;
    }

    public byte[] error(int errorcode) {
        byte[] mes = new byte[5];

        mes[0] = 0;
        mes[1] = 5;
        byte[] b = ShortToByte((short) errorcode);
        mes[2] = b[0];
        mes[3] = b[1];
        mes[4] = 0;
        return mes;
    }

    public short byteToShort(byte[] b) {
        return (short) (((short) b[0]) << 8 | ((short) (b[1]) & 0x00ff));
    }

    public byte[] ShortToByte(short a) {
        return new byte[] { (byte) (a >> 8), (byte) (a & 0xff) };
    }

}
