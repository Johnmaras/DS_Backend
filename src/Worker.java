import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Worker implements Runnable{

    private Socket con;
    private String ID = "192.168.1.67";
    private String config = "config_worker";

    public Worker(Socket con){
        this.con = con;
    }

    public void setID(String id){
        this.ID = id;
    }

    public String hash(){
        return this.ID;
    }

    /*private String getID(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            System.err.println(Functions.getTime() + "Unknown Host");
        }
        return null;
    }*/

    @Override
    public String toString() {
        return "Worker";
    }

    @Override
    public void run() {
        Functions functions = new Functions(this);
        try{
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            Message message = (Message)in.readObject();
            if(message.getRequestType() == 1){
                String query = message.getQuery();
                String response = functions.searchCache(query);
                sendToReducer(query, response);
                sendToMaster(null);
            }else if(message.getRequestType() == 2){

                String query = message.getQuery();
                String data = GoogleAPISearch(query);
                functions.updateCache(query, data);
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
            message.setData(data);
        }
        /*if(con.isClosed()){
            while(con == null){
                try{
                    con.connect(con.getRemoteSocketAddress());
                }catch(IOException e){
                    System.err.println(Functions.getTime() + "Worker_sendToMaster: There was an IO error 1");
                }
            }
        }*/
        try{
            ObjectOutputStream Masterout = new ObjectOutputStream(con.getOutputStream());
            Masterout.writeObject(message);
            System.out.println("At " + System.nanoTime() + " sent data: " + message.getData());
            Masterout.flush();
        }catch (IOException e) {
            System.err.println(Functions.getTime() + "Worker_sendToMaster: There was an IO error");
        }
    }

    private void sendToReducer(String query, String data){
        Message message = new Message(7, query);
        message.setData(data);
        Socket Reducercon = null;
        while(Reducercon == null) {
            try {
                Reducercon = new Socket(InetAddress.getByName("192.168.1.67"), 4001);
                ObjectOutputStream ReducerOut = new ObjectOutputStream(Reducercon.getOutputStream());
                ReducerOut.writeObject(message);
                ReducerOut.flush();
                System.out.print(System.nanoTime() + " Sent " + query + " " + data);
                ObjectInputStream in = new ObjectInputStream(Reducercon.getInputStream());
                String string = in.readUTF();
                if(string.equals("Done")){
                    break;
                }
            }catch(UnknownHostException e){
                System.err.println(Functions.getTime() + "Worker_sendToReducer: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Worker_sendToReducer: There was an IO error");
            }
        }
        System.out.println("Finished");
    }

    private void masterHandshake(){
        Socket handCon = null;
        while(handCon == null){
            try{
                handCon = new Socket(InetAddress.getByName(Functions.getMasterIP(config)), Functions.getMasterPort(config)); //TODO get ip and port from the appropriate Functions' method
                //ID = ID.substring(ID.indexOf('/') + 1);
                ObjectOutputStream out = new ObjectOutputStream(handCon.getOutputStream());
                Message message = new Message();
                message.setQuery("Worker");
                ArrayList<String> data = new ArrayList<>();
                data.add(ID);
                data.add(Integer.toString(4002));
                message.setData(data);
                out.writeObject(message);
                out.flush();
                ObjectInputStream in = new ObjectInputStream(handCon.getInputStream());
                String ack = in.readUTF();
                System.out.println(ack);
            }catch(NullPointerException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: Null pointer occurred. Trying again");
            }catch(UnknownHostException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Worker_masterHandshake: There was an IO error");
            }
        }
    }

    public static void main(String[] args){
        (new Worker(null)).masterHandshake();
        try{
            ServerSocket listenSocket = new ServerSocket(4002);
            while(true){
                try{
                    System.out.println("Waiting for connections...");
                    Socket connection = listenSocket.accept();
                    System.out.println("Connection accepted: " + connection.toString());
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
