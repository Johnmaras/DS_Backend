//import PointAdapter.PointAdapter.PolylineAdapter;

import Messages.Message;
import PointAdapter.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

//TODO handle the master's waiting for connection to reducer

//FIXME finish the refactoring

//TODO client sends full precision PointAdapter.Coordinates -> master clones and rounds the PointAdapter.Coordinates ->
//TODO master sends to workers -> workers find and return Tuples of full precision LatLngAdapters(as keys) and PolylineAdapters(as values)
//TODO ... -> master finds the closest of the full precision Tuples to the client's PointAdapter.Coordinates
public class Master implements Runnable{

    private Socket connection;
    private static String ID = "192.168.1.67";

    private String config = "config_master";

    /**
     * stores the directions the workers and the reducer send. The PointAdapter.Coordinates are rounded precision
     */
    private static final Hashtable<Coordinates, PolylineAdapter> cache = new Hashtable<>(); //key = coordinates, value = directions

    private static final Hashtable<Integer, String> workers = new Hashtable<>(); // key = incremental int, value = ip#port

    private static String reducerIP;
    private static String reducerPort;

    private int option;

    public Master(Socket con){
        this.connection = con;
    }

    public Master(){}

    public void setOption(int option){
        this.option = option;
    }

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
    public void run(){
        if(option == 1) {
            userInterface();
        }else{
            try{
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeBoolean(true);
                out.flush();
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                //Messages.Message message = (Messages.Message)in.readObject();

                Object omessage = in.readObject();

                //System.out.println("Entered");

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterDeserializer());
                gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterSerializer());
                gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterDeserializer());
                gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterSerializer());
                gsonBuilder.registerTypeAdapter(Coordinates.class, new CoordinatesDeserializer());
                gsonBuilder.registerTypeAdapter(Coordinates.class, new CoordinatesSerializer());
                Gson gson = gsonBuilder.create();

                Message message = new Message();
                if(omessage.getClass() == String.class){
                    message = gson.fromJson((String)omessage, Message.class);
                }else if(omessage.getClass() == Message.class){
                    message = (Message)omessage;
                }/*else if(omessage.getClass() == Messages.Message.class){
                requestType = ((Messages.Message) omessage).getRequestType();
                query = ((Messages.Message) omessage).getQuery();
            }*/
                if(message.getRequestType() == 9){ // 9 means search for route
                    //FIXME PointAdapter.Coordinates cant be rounded. Reducer will end up pairing rounded coordinates
                    //FIXME with the PolylineAdapters and master wont be able to decide the best route
                    Coordinates query = message.getQuery(); //query must be rounded in order to make worker search for a wider range of possibly matching coordinates
                    PolylineAdapter response = searchCache(query);
                    if(response == null){
                        Thread t = new Thread(new Master_Worker(query, 1));
                        t.start();
                        try{
                            t.join();
                        }catch(InterruptedException e){
                            System.err.println(Functions.getTime() + "Master_run: Interrupted!");
                            e.printStackTrace();
                            //TODO break if thread crashes(loop removed, no break needed)
                        }
                        connectToReducer(query);
                        response = searchCache(query);
                    }

                    message = new Message();
                    message.setResults(response);

                    System.out.println("Query is: " + query);
                    System.out.println("Results are: \n" + response);

                    //out = new ObjectOutputStream(connection.getOutputStream());
                    out.writeObject(message);
                    out.flush();
                }else if(message.getRequestType() == 0){ //0 means worker handshake
                    //out = new ObjectOutputStream(connection.getOutputStream());

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
                    //out = new ObjectOutputStream(connection.getOutputStream());

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
                //TODO (after removing the multiple requests functionality this error might not be thrown)
            }catch(ClassNotFoundException e){
                System.err.println(Functions.getTime() + "Master_run: Class not found.");
            }
        }
    }

    private PolylineAdapter bestRoute(Coordinates query, List<PolylineAdapter> results){
        //OptionalDouble max = results.parallelStream().filter(p -> p != null).mapToDouble(Double::parseDouble).max();
        double bestSumDist = Double.MAX_VALUE;
        int index = -1;
        for(PolylineAdapter pla: results){
            double xlatPlaOrigin = pla.getOrigin().getLatitude();
            double ylngPlaOrigin = pla.getOrigin().getLongitude();

            double xlatQueryOrigin = query.getOrigin().getLatitude();
            double ylngQueryOrigin = query.getOrigin().getLongitude();

            double originDistance = Math.sqrt(Math.pow(xlatPlaOrigin - xlatQueryOrigin, 2) + Math.pow(ylngPlaOrigin - ylngQueryOrigin, 2));

            double xlatPlaDest = pla.getDestination().getLatitude();
            double ylngPlaDest = pla.getDestination().getLongitude();

            double xlatQueryDest = query.getDestination().getLatitude();
            double ylngQueryDest = query.getDestination().getLongitude();

            double destDistance = Math.sqrt(Math.pow(xlatPlaDest - xlatQueryDest, 2) + Math.pow(ylngPlaDest - ylngQueryDest, 2));

            double sumDistance = originDistance + destDistance;

            if(sumDistance < bestSumDist){
                bestSumDist = sumDistance;
                index = results.indexOf(pla);
            }
        }
        return results.get(index);
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
                        if(message.getRequestType() == 8){ //8 means get the results
                            if(message.getResults().isEmpty()){
                                //join is needed to be sure that Master_Worker has updated the cache
                                Thread t = new Thread((new Master_Worker(message.getQuery(), 2)));
                                t.start();
                                try{
                                    t.join();
                                }catch(InterruptedException e){
                                    System.err.println(Functions.getTime() + "Master_connectToReducer: Interrupted!");
                                    e.printStackTrace();
                                }
                            }else{
                                //TODO use Euclidean distance to determine the best result
                                ArrayList<PolylineAdapter> results = message.getResults();
                                System.out.println("Query from reducer is: " + query);
                                System.out.println("Results from reducer are: \n" + results);
                                updateCache(query, bestRoute(query, results));
                                /*OptionalDouble max = data.parallelStream().filter(p -> p != null).mapToDouble(Double::parseDouble).max();
                                if(max.isPresent()) updateCache(message.getQuery(), Double.toString(max.getAsDouble()));*/
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
            }catch(UnknownHostException e){
                System.err.println(Functions.getTime() + "Master_connectToReducer: Unknown Host");
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Master_connectToReducer: IO Error");
                e.printStackTrace();
            }
        }
    }

    private void userInterface(){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print(ID + "> ");
            //FIXME while waiting for new commands, messages may be printed. prompt should be printed again
            String input = scanner.nextLine();
            /*if(input.equals("help")){
                help();
            }else */if(input.equals("cache")){
                for(Coordinates co: cache.keySet()){
                    System.out.println(co);
                    System.out.println(cache.get(co));
                }
            }/*else if(input.startsWith("get")){
                String filename = input.trim().substring(input.indexOf(" ")).trim(); //get the filename from the search command
                if(get(filename)){
                    System.out.println(Functions.getTime() + "Peer_userInterface: File " + filename + " has been downloaded");
                }else{
                    System.out.println(Functions.getTime() + "Peer_userInterface: Failed in downloading " + filename);
                }
            }else{
                System.out.println("Unknown command: " + input + " is not recognised as a command.");
            }*/
        }
    }

    //-----DATA RELATED METHODS-----
    public void updateCache(Coordinates query, PolylineAdapter h){
        synchronized (cache){
            cache.put(query.round(), h);
        }
    }

    public PolylineAdapter searchCache(Coordinates query){
        //TODO must compare the rounded PointAdapter.Coordinates in the cache
        //return cache.get(query);

        Coordinates queryRounded = query.round();
        //System.out.println(query);
        List<PolylineAdapter> results = new ArrayList<>();
        //co is rounded
        for(Coordinates co: cache.keySet()){
            //query is sent rounded by the master
            if(queryRounded.equals(co)){
                results.add(cache.get(co));
                //System.out.println(new Gson().toJson(cache.get(co)));
            }
        }

        return !results.isEmpty() ? bestRoute(query, results) : null;
    }

    public void updateWorkers(String worker_id){
        synchronized (workers){
            if(!workers.contains(worker_id)){
                workers.put(workers.size(), worker_id);
            }
        }
    }

    public static void main(String[] args){
        Master uiMaster = new Master();
        uiMaster.setOption(1);
        new Thread(uiMaster).start();
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
