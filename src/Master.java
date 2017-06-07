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

    private static final File cache_file = new File("master_" + hash() + "_cache");;
    private static final Hashtable<String, String> cache = new Hashtable<>(); //term(key) and hash(value)

    private static final File workers_file = new File("master_" + hash() + "_workers"); //only the master node has workers file
    private static final Hashtable<Integer, String> workers = new Hashtable<>();

    public Master(Socket con){
        this.connection = con;
    }

    public Master(){}

    public static String hash(){
        return ID;
    }

    protected static Hashtable<String, String> getCache(){ return cache;}

    protected static Hashtable<Integer, String> getWorkers(){ return workers;}

    @Override
    public String toString() {
        return "Master";
    }

    public void setConnection(Socket con){
        this.connection = con;
        System.out.println(connection.getLocalSocketAddress());
    }

    /*public void setID(String id){
        this.ID = id;
    }*/

    @Override
    public void run() {
        try{
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            Message message = (Message)in.readObject();
            if(message.getRequestType() == 9){
                String query;
                do{
                    query = message.getQuery();
                    if(query.equals("quit")) break;
                    String response = searchCache(query);
                    if(response == null){
                        //Thread t = new Thread(new Master_Worker(query, 1, functions));
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
            }else if(message.getRequestType() == 0){
                if(message.getQuery().equals("Worker")){
                    String worker_id = message.getResults().get(0);
                    updateWorkers(worker_id);
                    System.out.println(Functions.getTime() + "Worker " + worker_id + " added.");
                }else if(message.getQuery().equals("Reducer")){ //0 ip, 1 port
                    Functions.setReducer(message.getResults().get(0), message.getResults().get(1), config);
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

    private void connectToReducer(String query){
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
    public void updateCache(String query, String h){
        synchronized (cache){
            cache.put(query, h);
        }
        //redundant
        /*try{
            Hashtable<String, String> temp = loadCache();
            cache.putAll(temp);
            synchronized(cache_file){
                FileOutputStream c = new FileOutputStream(cache_file);
                ObjectOutputStream out = new ObjectOutputStream(c);
                out.writeObject(cache);
                out.flush();
                c.close();
                out.close();
            }
        }catch(FileNotFoundException e){
            System.err.println(getTime() + "Master_updateCache: File Not Found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println(getTime() + "Master_updateCache: There was an IO error");
            e.printStackTrace();
        }*/
    }

    public String searchCache(String query){
        //cache = loadCache();
        return cache.get(query);
    }

    public void updateWorkers(String worker_id){
        synchronized (workers){
            if(!workers.contains(worker_id)){
                workers.put(workers.size(), worker_id);
            /*try{
                Hashtable<Integer, String> temp = loadWorkers();
                temp.putAll(workers);
                synchronized (workers_file) {
                    FileOutputStream c = new FileOutputStream(workers_file);
                    ObjectOutputStream out = new ObjectOutputStream(c);
                    out.writeObject(temp);
                    out.flush();
                    c.close();
                    out.close();
                }
            }catch(FileNotFoundException e){
                System.err.println(Functions.getTime() + "Functions_updateWorkers: File not found");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println(Functions.getTime() + "Functions_updateWorkers: IO Error");
                e.printStackTrace();
            }*/
            }
        }
    }

    public static void main(String[] args){
        //new Functions(new Master()).clearFiles();
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
