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

public class Master implements Runnable{

    private static String config = "config_master";

    private Socket connection;

    private static String ID = Functions.getMyIP(config);

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

    protected static Hashtable<Integer, String> getWorkers(){ return workers;}

    private static boolean reducerConnected(){
        return reducerIP != null && reducerPort != null;
    }

    @Override
    public String toString() {
        return "Master";
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

                Object omessage = in.readObject();

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
                }

                if(message.getRequestType() == 9){ // 9 means search for route

                    Coordinates query = message.getQuery(); //query must be rounded in order to make worker search for a wider range of possibly matching coordinates
                    PolylineAdapter response = searchCache(query);
                    if(response == null){
                        Thread t = new Thread(new Master_Worker(query, 1));
                        t.start();
                        try{
                            t.join();
                        }catch(InterruptedException e){
                            Functions.printErr(this.toString(), "Interrupted!");
                        }
                        connectToReducer(query);
                        response = searchCache(query);
                    }

                    System.out.println("Query is: " + query);
                    System.out.println("Results are: \n" + response);

                    String responseJson = gson.toJson(response);
                    out.writeObject(responseJson);
                    out.flush();
                }else if(message.getRequestType() == 0){ //0 means worker handshake

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
                Functions.printErr(this.toString(), "IO Error");
            }catch(ClassNotFoundException e){
                Functions.printErr(this.toString(), "Class not found.");
            }
        }
    }

    private PolylineAdapter bestRoute(Coordinates query, List<PolylineAdapter> results){
        double bestSumDist = Double.MAX_VALUE;
        int R = 6371;
        int index = -1;
        for(PolylineAdapter pla: results){
            double xlatPlaOrigin = pla.getOrigin().getLatitude();
            double ylngPlaOrigin = pla.getOrigin().getLongitude();

            double xPlaOrigin = R * Math.cos(xlatPlaOrigin) * Math.cos(ylngPlaOrigin);
            double yPlaOrigin = R * Math.cos(xlatPlaOrigin) * Math.sin(ylngPlaOrigin);


            double xlatQueryOrigin = query.getOrigin().getLatitude();
            double ylngQueryOrigin = query.getOrigin().getLongitude();

            double xQueryOrigin = R * Math.cos(xlatQueryOrigin) * Math.cos(ylngQueryOrigin);
            double yQueryOrigin = R * Math.cos(xlatQueryOrigin) * Math.sin(ylngQueryOrigin);

            double originDistance = Math.sqrt(Math.pow(xPlaOrigin - xQueryOrigin, 2) + Math.pow(yPlaOrigin - yQueryOrigin, 2));

            double xlatPlaDest = pla.getDestination().getLatitude();
            double ylngPlaDest = pla.getDestination().getLongitude();

            double xPlaDest = R * Math.cos(xlatPlaDest) * Math.cos(ylngPlaDest);
            double yPlaDest = R * Math.cos(xlatPlaDest) * Math.sin(ylngPlaDest);

            double xlatQueryDest = query.getDestination().getLatitude();
            double ylngQueryDest = query.getDestination().getLongitude();

            double xQueryDest = R * Math.cos(xlatQueryDest) * Math.cos(ylngQueryDest);
            double yQueryDest = R * Math.cos(xlatQueryDest) * Math.sin(ylngQueryDest);

            double destDistance = Math.sqrt(Math.pow(xPlaDest - xQueryDest, 2) + Math.pow(yPlaDest - yQueryDest, 2));

            double sumDistance = originDistance > destDistance ? originDistance / destDistance : destDistance / originDistance;

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
                                    Functions.printErr(this.toString(), "Interrupted!");
                                }
                            }else{
                                ArrayList<PolylineAdapter> results = message.getResults();
                                System.out.println("Query from reducer is: " + query);
                                System.out.println("Results from reducer are: \n" + results);
                                updateCache(query, bestRoute(query, results));
                            }
                        }

                    }catch(NullPointerException e){
                        Functions.printErr(this.toString(), "Null Pointer!");
                    }catch(SocketTimeoutException e){
                        Functions.printErr(this.toString(), "Socket Time Out!");
                    }catch(ClassNotFoundException e){
                        Functions.printErr(this.toString(), "Class Not Found. in");
                    }
                }
                ReducerCon.close();
            }catch(UnknownHostException e){
                Functions.printErr(this.toString(), "Unknown Host");
            }catch(IOException e){
                Functions.printErr(this.toString(), "IO Error");
            }
        }
    }

    private void userInterface(){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print(ID + "> ");
            //FIXME while waiting for new commands, messages may be printed. prompt should be printed again
            String input = scanner.nextLine();
            if(input.equals("cache")){
                for(Coordinates co: cache.keySet()){
                    //System.out.println(co);
                    System.out.println(cache.get(co) + "\n");
                }
            }else if(input.equals("exit")){
                System.exit(0);
            }
        }
    }

    //-----DATA RELATED METHODS-----
    public void updateCache(Coordinates query, PolylineAdapter h){
        synchronized (cache){
            cache.put(query.round(), h);
        }
    }

    /**
     * Searches the cache for routes with matching rounded coordinates(origin, destination)
     * <br> and then determines the best of them
     *
     * @param query :The coordinates the client sent
     * @return The corresponding route
     */
    public PolylineAdapter searchCache(Coordinates query){
        Coordinates queryRounded = query.round();
        List<PolylineAdapter> results = new ArrayList<>();
        //co is rounded
        for(Coordinates co: cache.keySet()){
            //query is sent rounded by the master
            if(queryRounded.equals(co)){
                results.add(cache.get(co));
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
                    Functions.printErr("Master", "There was an IO error 1");
                }
            }
        }catch(IOException e){
            Functions.printErr("Master", "There was an IO error 2");
        }
    }
}
