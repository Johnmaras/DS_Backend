import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.OptionalDouble;

//TODO handle the master's waiting for connection to reducer
public class Master implements Runnable{

    private Socket connection;
    private String ID = "192.168.1.67";
    private String config = "config_master";

    public Master(Socket con){
        this.connection = con;
    }

    public Master(){}

    public String hash(){
        return this.ID;
    }

    @Override
    public String toString() {
        return "Master";
    }

    public void setConnection(Socket con){
        this.connection = con;
        System.out.println(connection.getLocalSocketAddress());
    }

    public void setID(String id){
        this.ID = id;
    }

    @Override
    public void run() {
        Functions functions = new Functions(this);
        try{
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            Message message = (Message)in.readObject();
            if(message.getRequestType() == 9){
                String query;
                do{
                    query = message.getQuery();
                    if(query.equals("quit")) break;
                    String response = functions.searchCache(query);
                    if(response == null){
                        Thread t = new Thread(new Master_Worker(query, 1, functions));
                        t.start();
                        try{
                            t.join();
                        }catch(InterruptedException e){
                            System.err.println(Functions.getTime() + "Master_run: Interrupted!");
                            e.printStackTrace();
                            //TODO break if thread crashes
                        }
                        connectToReducer(functions, query);
                        response = functions.searchCache(query);
                    }
                    message = new Message();
                    message.setData(response);

                    ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                    out.writeObject(message);
                    out.flush();
                    in = new ObjectInputStream(connection.getInputStream());
                    message = (Message)in.readObject();
                }while(true);
            }else if(message.getRequestType() == 0){
                if(message.getQuery().equals("Worker")){
                    String worker_id = message.getData().get(0);
                    functions.updateWorkers(worker_id);
                    System.out.println(Functions.getTime() + "Worker " + worker_id + " added.");
                }else if(message.getQuery().equals("Reducer")){ //0 ip, 1 port
                    Functions.setReducer(message.getData().get(0), message.getData().get(1), config);
                }
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeUTF("Connection Done!");
                out.flush();
            }
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Master_run: IO Error");
            e.printStackTrace();
            //TODO handle the reset connection error on client connections
        }catch(ClassNotFoundException e){
            System.err.println(Functions.getTime() + "Master_run: Class not found.");
        }
    }

    private void connectToReducer(Functions functions, String query){
        Socket ReducerCon = null;
        while(ReducerCon == null){
            try{
                ReducerCon = new Socket(InetAddress.getByName(Functions.getReducerIP(config)), Functions.getReducerPort(config));
                ObjectOutputStream out = new ObjectOutputStream(ReducerCon.getOutputStream());
                Message message = new Message(4, query);
                out.writeObject(message);
                out.flush();
                System.out.println(Functions.getTime() + " Sent to Reducer");
                ObjectInputStream in = null;
                while(in == null){
                    try{
                        in = new ObjectInputStream(ReducerCon.getInputStream());
                        message = (Message)in.readObject();
                        if(message.getRequestType() == 8){
                            if(message.getData().isEmpty()){
                                //join is needed to be sure that Master_Worker has updated the cache
                                Thread t = new Thread((new Master_Worker(message.getQuery(), 2, functions)));
                                t.start();
                                try{
                                    t.join();
                                } catch (InterruptedException e) {
                                    System.err.println(Functions.getTime() + "Master_connectToReducer: Interrupted!");
                                    e.printStackTrace();
                                }
                            }else{
                                ArrayList<String> data = message.getData();
                                OptionalDouble max = data.parallelStream().filter(p -> p != null).mapToDouble(Double::parseDouble).max();
                                if(max.isPresent()) functions.updateCache(message.getQuery(), Double.toString(max.getAsDouble()));
                            }
                        }

                    }catch(NullPointerException e){
                        System.err.println(Functions.getTime() + "Master_connectToReducer: Null Pointer!");
                        e.printStackTrace();
                    }catch(SocketTimeoutException e){
                        System.err.println(Functions.getTime() + "Master_connectToReducer: Socket Time Out!");
                    }catch(ClassNotFoundException e){
                        System.err.println(Functions.getTime() + "Master_connectToReducer: Class Not Found. in");
                    }
                }
                ReducerCon.close();
            } catch (UnknownHostException e) {
                System.err.println(Functions.getTime() + "Master_connectToReducer: Unknown Host");
            } catch (IOException e) {
                System.err.println(Functions.getTime() + "Master_connectToReducer: IO Error");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        new Functions(new Master()).clearFiles();
        try{
            ServerSocket listenSocket = new ServerSocket(4000);
            while(true){
                try{
                    Socket new_con = listenSocket.accept();
                    new Thread(new Master(new_con)).start();
                    System.out.println(Functions.getTime() + "Connection accepted: " + new_con.toString());
                }catch(IOException e){
                    System.err.println(Functions.getTime() + "Master_main: There was an IO error 1");
                }
            }
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Master_main: There was an IO error 2");
        }
    }
}
