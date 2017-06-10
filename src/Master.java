import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.OptionalDouble;

//TODO handle the master's waiting for connection to reducer

//FIXME finish the refactoring
public class Master implements Runnable{

    private Socket connection;
    private static String ID = "192.168.1.70";

    private String config = "config_master";

    private static final Hashtable<Coordinates, PolylineAdapter> cache = new Hashtable<>(); //key = coordinates, value = directions

    private static final Hashtable<Integer, String> workers = new Hashtable<>(); // key = incremental int, value = ip#port

    private static String reducerIP;
    private static String reducerPort;

    public Master(Socket con){
        this.connection = con;
    }

    public Master(){}

    public static String hash(){
        return ID;
    }

    protected static Hashtable<Coordinates, PolylineAdapter> getCache(){ return cache;}

    protected static Hashtable<Integer, String> getWorkers(){ return workers;}

    private static boolean reducerConnected(){
        return reducerIP != null && reducerPort != null;
    }

    @Override
    public String toString() {
        return "Master";
    }

    public void setConnection(Socket con){
        this.connection = con;
        System.out.println(connection.getLocalSocketAddress());
    }

    @Override
    public void run() {
        try{
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            Message message = (Message)in.readObject();
            if(message.getRequestType() == 9){ // 9 means search for route
                Coordinates query;
                //FIXME each connection from the client accepts only one query. quit option must be removed
                do{
                    query = message.getQuery();
                    if(query.equals("quit")) break;
                    PolylineAdapter response = searchCache(query);
                    if(response == null){
                        Thread t = new Thread(new Master_Worker(query, 1));
                        t.start();
                        try{
                            t.join();
                        }catch(InterruptedException e){
                            System.err.println(Functions.getTime() + "Master_run: Interrupted!");
                            e.printStackTrace();
                            //TODO break if thread crashes
                        }
                        connectToReducer(query);
                        response = searchCache(query);
                    }
                    message = new Message();
                    message.setResults(response);

                    ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                    out.writeObject(message);
                    out.flush();
                    in = new ObjectInputStream(connection.getInputStream());
                    message = (Message)in.readObject();
                }while(true);
            }else if(message.getRequestType() == 0){ //0 means worker handshake
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());

                out.writeBoolean(true);
                out.flush();

                String worker_ip = in.readUTF();
                String worker_port = in.readUTF();

                String worker_id = worker_ip + "#" + worker_port;
                updateWorkers(worker_id);
                System.out.println(Functions.getTime() + "Worker " + worker_id + " added.");

                while(!reducerConnected()){
                    out.writeBoolean(false);
                    out.flush();
                }
                out.writeBoolean(true);
                out.flush();

                out.writeUTF(reducerIP);
                out.flush();

                out.writeUTF(reducerPort);
                out.flush();
            }else if(message.getRequestType() == 10){ // 10 means reducer handshake
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());

                out.writeBoolean(true); //inform the reducer that it can continue
                out.flush();

                reducerIP = in.readUTF();
                reducerPort = in.readUTF();

                out.writeBoolean(true);
                out.flush();

                Functions.setReducer(reducerIP, reducerPort, config);
            }
            connection.close();
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Master_run: IO Error");
            e.printStackTrace();
            //TODO handle the reset connection error on client connections
        }catch(ClassNotFoundException e){
            System.err.println(Functions.getTime() + "Master_run: Class not found.");
        }
    }

    private void connectToReducer(Coordinates query){
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
                            if(message.getResults().isEmpty()){
                                //join is needed to be sure that Master_Worker has updated the cache
                                Thread t = new Thread((new Master_Worker(message.getQuery(), 2)));
                                t.start();
                                try{
                                    t.join();
                                } catch (InterruptedException e) {
                                    System.err.println(Functions.getTime() + "Master_connectToReducer: Interrupted!");
                                    e.printStackTrace();
                                }
                            }else{
                                //TODO use Euclidean distance to determine the best result
                                ArrayList<String> data = message.getResults();
                                OptionalDouble max = data.parallelStream().filter(p -> p != null).mapToDouble(Double::parseDouble).max();
                                if(max.isPresent()) updateCache(message.getQuery(), Double.toString(max.getAsDouble()));
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

    //-----DATA RELATED METHODS-----
    public void updateCache(Coordinates query, PolylineAdapter h){
        synchronized (cache){
            cache.put(query, h);
        }
    }

    public PolylineAdapter searchCache(Coordinates query){
        return cache.get(query);
    }

    public void updateWorkers(String worker_id){
        synchronized (workers){
            if(!workers.contains(worker_id)){
                workers.put(workers.size(), worker_id);
            }
        }
    }

    public static void main(String[] args){
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
