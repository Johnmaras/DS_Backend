import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

public class Master implements Runnable{

    private Socket connection;
    private String ID = getID();

    public Master(Socket con){
        this.connection = con;
    }

    public Master(){}

    public String hash(){
        return ID;
    }

    public void setConnection(Socket con){
        this.connection = con;
    }

    private String getID(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            System.err.println("Unknown Host");
        }
        return null;
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
                        Thread t = new Thread(new Master_Worker(query, 1));
                        t.start();
                        try {
                            t.join();
                        } catch (InterruptedException e) {
                            System.err.println("Master_run: Interrupted! 1");
                            e.printStackTrace();
                        }
                        Socket reducerCon = connectToReducer();
                        in = null;
                        while(in == null){
                            try{
                                in = new ObjectInputStream(reducerCon.getInputStream());
                                message = (Message)in.readObject();
                                if(message.getRequestType() == 8){
                                    if(message.getData().isEmpty()){
                                        //join is needed to be sure that Master_Worker has updated the cache
                                        t = new Thread((new Master_Worker(message.getQuery(), 2)));
                                        t.start();
                                        try{
                                            t.join();
                                        } catch (InterruptedException e) {
                                            System.err.println("Master_run: Interrupted! 2");
                                            e.printStackTrace();
                                        }
                                    }else{
                                        ArrayList<String> data = message.getData();
                                        String max = data.parallelStream().max(String::compareTo).get();
                                        functions.updateCache(message.getQuery(), max);
                                    }
                                }

                            }catch(NullPointerException e){
                                System.err.println("Master_run: Null Pointer!");
                            }catch(SocketTimeoutException e){
                                System.err.println("Master_run: Socket Time Out!");
                            }catch(ClassNotFoundException e){
                                System.err.println("Master_run: Class Not Found. in");
                            }
                        }
                        reducerCon.close();
                        response = functions.searchCache(query);
                    }
                    //System.out.println(query + " added.");
                    message = new Message();
                    message.setData(response);

                    ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                    out.writeObject(message);
                    out.flush();

                    message = (Message)in.readObject();
                }while(true);
            }else if(message.getRequestType() == 0){
                SocketAddress worker = connection.getRemoteSocketAddress();
                String worker_id = worker.toString();
                functions.updateWorkers(worker_id, worker);
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                out.writeUTF("Connection Done!");
                out.flush();
                System.out.println("Worker " + worker_id + " added.");
            }
        }catch(IOException e){
            System.err.println("Master_run: IO Error");
        }catch(ClassNotFoundException e){
            System.err.println("Master_run: Class not found. out");
        }
    }

    private Socket connectToReducer(){
        Socket ReducerCon = null;
        while(ReducerCon == null){
            try{
                ReducerCon = new Socket(InetAddress.getByName("127.0.0.1"), 4001); //TODO find creds via config file
                ObjectOutputStream out = new ObjectOutputStream(ReducerCon.getOutputStream());
                Message message = new Message();
                message.setRequestType(4);
                out.writeObject(message);
                out.flush();
                out.close();
            } catch (UnknownHostException e) {
                System.err.println("Master_connectToReducer: Unknown Host");
            } catch (IOException e) {
                System.err.println("Master_connectToReducer: IO Error");
            }
        }
        return ReducerCon;
    }

    public static void main(String[] args){
        try{
            ServerSocket listenSocket = new ServerSocket(4000);
            while(true){
                try{
                    //accepted connections are only come from the clients
                    Socket new_con = listenSocket.accept();
                    new Thread(new Master(new_con)).start();
                    System.out.println("Connection accepted: " + new_con.toString());
                }catch(IOException e){
                    System.err.println("Master_main: There was an IO error 1");
                }
            }
        }catch(IOException e){
            System.err.println("Master_main: There was an IO error 2");
        }
    }
}
