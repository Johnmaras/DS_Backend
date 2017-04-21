import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Worker implements Runnable{

    private Socket con;
    private String ID;

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
        String h = Double.toString(query.hashCode() + Math.random());
        return h;
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
        if(con.isClosed()){
            while(con == null){
                try{
                    con.connect(con.getRemoteSocketAddress());
                }catch(IOException e){
                    System.err.println("Worker_sendToMaster: There was an IO error 1");
                }
            }
        }
        try{
            ObjectOutputStream Masterout = new ObjectOutputStream(con.getOutputStream());
            Masterout.writeObject(message);
            Masterout.flush();
        }catch (IOException e) {
            System.err.println("Worker_sendToMaster: There was an IO error 2");
        }
    }

    private void sendToReducer(String query, String data){
        Message message = new Message(7, query);
        message.setData(data);
        Socket Reducercon = null;
        while(Reducercon == null) {
            try {
                Reducercon = new Socket(InetAddress.getByName("127.0.0.1"), 4001);
                ObjectOutputStream ReducerOut = new ObjectOutputStream(Reducercon.getOutputStream());
                ReducerOut.writeObject(message);
                ReducerOut.flush();
                //TODO wait for ack
            }catch(UnknownHostException e){
                System.err.println("WM_Search: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println("WM_Search: There was an IO error on startClient()");
            }
        }
    }

    private void masterHandshake(){
        Socket handCon = null;
        while(handCon == null){
            try{
                handCon = new Socket(InetAddress.getByName("127.0.0.1"), 4000); //TODO get ip and port from the appropriate Functions' method
                ID = handCon.getLocalSocketAddress().toString();
                ObjectOutputStream out = new ObjectOutputStream(handCon.getOutputStream());
                Message message = new Message();
                message.setQuery("handshake"); //redundant
                out.writeObject(message);
                out.flush();
                ObjectInputStream in = new ObjectInputStream(handCon.getInputStream());
                String ack = ((Message)in.readObject()).getQuery();
                System.out.println(ack);
            }catch(NullPointerException e){
                System.err.println("Worker_masterHandshake: Null pointer occurred. Trying again");
            }catch(UnknownHostException e){
                System.err.println("Worker_masterHandshake: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println("Worker_masterHandshake: There was an IO error on openServer");
            } catch (ClassNotFoundException e) {
                System.err.println("Worker_masterHandshake: Class not found");
            }
        }
    }

    public static void main(String[] args){
        (new Worker(null)).masterHandshake();
        try{
            ServerSocket listenSocket = new ServerSocket(4002);
            while(true){
                try{
                    Socket connection = listenSocket.accept();
                    System.out.println("Connection accepted: " + connection.toString());
                    new Thread(new Worker(connection)).start();
                }catch(IOException e){
                    System.err.println("Worker_main: There was an IO error 1");
                }
            }
        }catch(IOException e){
            System.err.println("Worker_main: There was an IO error 2");
        }
    }
}
