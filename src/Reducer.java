import Messages.Message;
import PointAdapter.Coordinates;
import PointAdapter.PolylineAdapter;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reducer implements Runnable{

    private static String config = "config_reducer";

    private Socket con;
    private String ID = Functions.getMyIP(config);

    private static int port = (getPort() == 0 ? generatePort() : getPort()); //if the port is not assigned yet, set a random port number
    //TODO keep only the temp_cache, file is redundant
    private final File temp_file = new File("reducer_" + hash() + "_temp");
    private final ArrayList<Tuple> temp_cache = loadCache();

    public Reducer(Socket con){
        this.con = con;
    }

    public String hash(){
        return this.ID;
    }

    @Override
    public String toString() {
        return "Reducer";
    }

    private static int getPort(){
        return port;
    }

    //returns a random port number that is not currently in use
    private static int generatePort(){
        Random r = new Random(); //creates random object
        r.setSeed(System.currentTimeMillis()); //set the seed
        int port;
        while(true){
            port = r.nextInt(20000); //gets a random int from 0 to 20000
            if(port < 4001) continue; //if the number is less than 4001, picks another one
            try{
                ServerSocket listen = new ServerSocket(port); //tries to listen to this port to check if it's not in use
                listen.close(); //closes the ServerSocket(we don't need to keep it open)
                return port; //returns the number
            }catch(IOException e){
                Functions.printErr("Reducer", "Port " + port + " is currently in use");
            }
        }
    }

    @Override
    public void run(){
        try{
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            Message request = (Message)in.readObject();
            if(request.getRequestType() == 7){ //7 means get the results
                //query is full precision
                updateCache(request.getQuery().round(), request.getResults());
                ObjectOutputStream o = new ObjectOutputStream(con.getOutputStream());
                o.writeBoolean(true);
                o.flush();
                o.close();
            }else if(request.getRequestType() == 4){ //start the reduce process
                ArrayList<PolylineAdapter> results = new ArrayList<>();
                //query is full precision
                Coordinates query = request.getQuery().round();
                try{
                    synchronized(temp_file){
                        FileInputStream fi = new FileInputStream(temp_file);
                        ObjectInputStream input = new ObjectInputStream(fi);

                        ArrayList<Tuple> temp_data = (ArrayList<Tuple>)input.readObject();

                        Stream stream = temp_data.parallelStream().filter(s -> s.getKey().equals(query)).map(Tuple::getValue);
                        try{
                            results = (ArrayList<PolylineAdapter>)stream.collect(Collectors.toList());
                        }catch(NullPointerException e){
                            System.err.println(Functions.getTime() + con.getLocalSocketAddress() + " Stream is empty!");
                        }
                        System.out.print(System.nanoTime() + " ");
                        results.forEach(System.out::println);
                    }
                }catch(FileNotFoundException e){
                    System.err.println(Functions.getTime() + "Reducer_run: File not found");
                }
                //sends the full precision query
                sendToMaster(request.getQuery(), results);
                clearFile(temp_file);
            }
            con.close();
        }catch(NullPointerException e){
            Functions.printErr(this.toString(), "Null pointer occurred");
        }catch(FileNotFoundException e){
            Functions.printErr(this.toString(), "File not found!");
        }catch(IOException e){
            Functions.printErr(this.toString(), "IOException occurred");
        }catch(ClassNotFoundException e){
            Functions.printErr(this.toString(), "Class not found occurred");
        }
    }

    private void sendToMaster(Coordinates query, ArrayList<PolylineAdapter> results){
        Message message = new Message(8, query, results);
        ObjectOutputStream out = null;
        while(out == null){
            try{
                out = new ObjectOutputStream(con.getOutputStream());
                out.writeObject(message);
                out.flush();
                System.out.println("Sent to Master " + message.getResults());
            }catch(IOException e){
                Functions.printErr(this.toString(), "IOException occurred");
            }
        }
    }

    private void masterHandshake(){
        Socket handCon = null;
        while(handCon == null){
            try{
                handCon = new Socket(InetAddress.getByName(Functions.getMasterIP(config)), Functions.getMasterPort(config));
                ObjectOutputStream out = new ObjectOutputStream(handCon.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(handCon.getInputStream());
                Message message = new Message();
                message.setRequestType(10);

                in.readBoolean();

                out.writeObject(message);
                out.flush();

                if(!in.readBoolean()){
                    handCon = null;
                    continue; //just wait for the master. false input means unwanted event happened
                }

                out.writeUTF(ID);
                out.flush();

                out.writeUTF(Integer.toString(getPort()));
                out.flush();

                if(!in.readBoolean()){
                    handCon = null;
                    continue; //just wait for the master. false input means unwanted event happened
                }

                System.out.println("Handshake Done!");
            }catch(NullPointerException e){
                Functions.printErr(this.toString(), "Null pointer occurred. Trying again");
            }catch(UnknownHostException e){
                Functions.printErr(this.toString(), "You are trying to connect to an unknown host!");
            }catch(IOException e){
                Functions.printErr(this.toString(), "There was an IO error");
            }
        }
    }

    //-----DATA RELATED METHODS-----
    private ArrayList<Tuple> loadCache(){
        createCache();
        try{
            synchronized(temp_file) {
                FileInputStream f = new FileInputStream(temp_file);
                return (ArrayList<Tuple>) (new ObjectInputStream(f)).readObject();
            }
        }catch(IOException e){
            Functions.printErr(this.toString(), "IOException occurred");
        }catch(ClassNotFoundException e){
            Functions.printErr(this.toString(), "ClassNotFoundException occurred");
        }
        return new ArrayList<>();
    }

    private void createCache(){
        if(!Functions.checkFile(temp_file)){
            ArrayList<Tuple> temp = new ArrayList<>();
            try{
                synchronized(temp_file) {
                    FileOutputStream f = new FileOutputStream(temp_file);
                    ObjectOutputStream out = new ObjectOutputStream(f);
                    out.writeObject(temp);
                    out.flush();
                }
            }catch(IOException e){
                Functions.printErr(this.toString(), "IOException occurred");
            }
        }
    }

    private void updateCache(Coordinates query, ArrayList<PolylineAdapter> results){
        synchronized(temp_cache){
            for(PolylineAdapter pla : results){
                temp_cache.add(new Tuple(query, pla));
            }

            try {
                ArrayList<Tuple> temp = loadCache();
                temp_cache.addAll(temp);
                synchronized (temp_file) { //works fine
                    FileOutputStream fo = new FileOutputStream(temp_file);
                    ObjectOutputStream out = new ObjectOutputStream(fo);
                    out.writeObject(temp_cache);
                    out.flush();
                    fo.close();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                Functions.printErr(this.toString(), "File Not Found");
            } catch (IOException e) {
                Functions.printErr(this.toString(), "There was an IO error");
            }
        }
    }

    private void clearFile(File file){
        if(Functions.checkFile(file)){
            ArrayList<Tuple> temp = new ArrayList<>();
            try{
                synchronized(temp_file){
                    FileOutputStream f = new FileOutputStream(temp_file);
                    ObjectOutputStream out = new ObjectOutputStream(f);
                    out.writeObject(temp);
                    out.flush();
                    out.close();
                }
            }catch(IOException e){
                Functions.printErr(this.toString(), "IOException occurred");
            }
        }
    }

    public static void main(String[] args){
        System.out.println("Port = " + getPort());
        new Reducer(null).masterHandshake();
        try{
            ServerSocket listenSocket = new ServerSocket(getPort());
            while(true){
                try{
                    System.out.println("Waiting for new connection...");
                    Socket connection = listenSocket.accept();
                    new Thread(new Reducer(connection)).start();
                }catch(IOException e){
                    Functions.printErr("Reducer", "There was an IO error 1");
                }
            }
        }catch(IOException e){
            Functions.printErr("Reducer", "There was an IO error 2");
        }
    }
}
