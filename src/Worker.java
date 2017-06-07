import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

//FIXME finish the refactoring
public class Worker implements Runnable{

    private Socket con;
    private String ID = "192.168.1.70";
    private static int port = (getPort() == 0 ? generatePort() : getPort()); //if the port is not assigned yet, set a random port number

    private String config = "config_worker";

    private static final Hashtable<String, String> cache = new Hashtable<>(); //term(key) and hash(value)


    public Worker(Socket con){
        this.con = con;
    }

    public void setID(String id){
        this.ID = id;
    }

    public String hash(){
        return this.ID;
    }

    @Override
    public String toString() {
        return "Worker";
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
                System.err.println(Functions.getTime() + "Worker_setPort: Port " + port + " is currently in use");
            }
        }
    }

    @Override
    public void run() {
        try{
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            Message message = (Message)in.readObject();
            if(message.getRequestType() == 1){
                String query = message.getQuery();
                String response = searchCache(query);
                sendToReducer(query, response);
                sendToMaster(null);
            }else if(message.getRequestType() == 2){

                String query = message.getQuery();
                String data = GoogleAPISearch(query);
                updateCache(query, data);
                sendToMaster(data, query);
            }
        }catch(IOException e){
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private String GoogleAPISearch(String query){
        return Double.toString(query.hashCode() * Math.random());
    }

    private void sendToMaster(String data, String... query ){
        Message message = new Message();
        if(data == null){
            message.setRequestType(5);
        }else{
            message.setRequestType(6);
            message.setQuery(query[0]);
            message.setResults(data);
        }
        try{
            ObjectOutputStream Masterout = new ObjectOutputStream(con.getOutputStream());
            Masterout.writeObject(message);
            System.out.println(Functions.getTime() + "Sent data: " + message.getResults());
            Masterout.flush();
        }catch (IOException e) {
            System.err.println(Functions.getTime() + "Worker_sendToMaster: There was an IO error");
        }
    }

    private void sendToReducer(String query, String data){
        Message message = new Message(7, query);
        message.setResults(data);
        Socket Reducercon = null;
        while(Reducercon == null) {
            try {
                Reducercon = new Socket(InetAddress.getByName("192.168.1.70"), 4001);
                ObjectOutputStream ReducerOut = new ObjectOutputStream(Reducercon.getOutputStream());
                ReducerOut.writeObject(message);
                ReducerOut.flush();
                System.out.print(Functions.getTime() + "Sent " + query + " " + data);
                ObjectInputStream in = new ObjectInputStream(Reducercon.getInputStream());
                if(in.readBoolean()) break;
            }catch(UnknownHostException e){
                System.err.println(Functions.getTime() + "Worker_sendToReducer: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Worker_sendToReducer: There was an IO error");
            }
        }
        System.out.println(Functions.getTime() + "Finished");
    }

    private void masterHandshake(){
        Socket handCon = null;
        while(handCon == null){
            try{
                handCon = new Socket(InetAddress.getByName(Functions.getMasterIP(config)), Functions.getMasterPort(config));
                ObjectOutputStream out = new ObjectOutputStream(handCon.getOutputStream());
                Message message = new Message();
                message.setQuery("Worker");
                ArrayList<String> data = new ArrayList<>();
                data.add(ID);
                data.add(Integer.toString(4002));
                message.setResults(data);
                out.writeObject(message);
                out.flush();
                ObjectInputStream in = new ObjectInputStream(handCon.getInputStream());
                String ack = in.readUTF();
                System.out.println(Functions.getTime() + ack);
            }catch(NullPointerException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: Null pointer occurred. Trying again");
            }catch(UnknownHostException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: There was an IO error");
            }
        }
    }

    //-----DATA RELATED METHODS-----
    public void updateCache(String query, String h){
        synchronized (cache){
            cache.put(query, h);
        }
    }

    public String searchCache(String query){
        //cache = loadCache();
        return cache.get(query);
    }

    public static void main(String[] args){
        (new Worker(null)).masterHandshake();
        try{
            ServerSocket listenSocket = new ServerSocket(getPort());
            while(true){
                try{
                    System.out.println(Functions.getTime() + "Waiting for connections...");
                    Socket connection = listenSocket.accept();
                    System.out.println(Functions.getTime() + "Connection accepted: " + connection.toString());
                    new Thread(new Worker(connection)).start();
                }catch(IOException e){
                    System.err.println(Functions.getTime() + "Worker_main: There was an IO error 1");
                }
            }
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Worker_main: There was an IO error 2");
        }
    }
}
