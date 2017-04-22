import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
//TODO each thread creates a connection to a worker, gets the data
//TODO and stores it into the file(common for all instances).
//TODO When the main Reducer gets the signal form the master,
//TODO reads the file and creates a list of all data.

//TODO manage connections' close
public class Reducer implements Runnable{

    private Socket con;
    private String ID = "192.168.1.67";
    private final File temp_file = new File("reducer_" + hash() + "_temp");
    private ArrayList<Tuple> temp_cache = loadCache();

    public Reducer(Socket con){
        this.con = con;
        createFile(temp_file);
    }

    /*public String getID(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            System.err.println("Reducer_getID: Host not found.");
        }
        return null;
    }*/

    public String hash(){
        return this.ID;
    }

    @Override
    public String toString() {
        return "Reducer";
    }

    @Override
    public void run(){
        try{
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            Message request = (Message)in.readObject();
            if(request.getRequestType() == 7){
                //TODO use reduce method
                updateCache(request.getQuery(), request.getData().get(0));
                ObjectOutputStream o = new ObjectOutputStream(con.getOutputStream());
                o.writeUTF("Done");
                o.flush();
                o.close();
            }else if(request.getRequestType() == 4){
                ArrayList<String> data = new ArrayList<>(); //there is always just one item in the data coming from the workers
                String query = request.getQuery();
                try{
                    synchronized(temp_file){
                        FileInputStream fi = new FileInputStream(temp_file);
                        ObjectInputStream input = new ObjectInputStream(fi);

                        ArrayList<Tuple> temp_data = (ArrayList<Tuple>)input.readObject();

                        data = (ArrayList<String>)temp_data.parallelStream().filter(s -> s.getKey().equals(query)).map(Tuple::getValue).collect(Collectors.toList());
                        System.out.print(System.nanoTime() + " ");
                        data.forEach(System.out::println);
                    }
                }catch(FileNotFoundException e){
                    System.err.println("Reducer_run: File not found");
                    e.printStackTrace();
                }
                sendToMaster(request.getQuery(), data);
                clearFile(temp_file);
            }
        }catch(NullPointerException e) {
            System.err.println("Reducer_run: Null pointer occurred");
        }catch(FileNotFoundException e){
            System.err.println("Reducer_run: File not found!");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("Reducer_run: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e) {
            System.err.println("Reducer_run: Class not found occurred");
        }
    }

    private void sendToMaster(String query, ArrayList<String> data){
        Message message = new Message(8, query, data);
        ObjectOutputStream out = null;
        while(out == null){
            try{
                //TODO check if connection is still alive
                out = new ObjectOutputStream(con.getOutputStream());
                out.writeObject(message);
                out.flush();
                System.out.println("Sent to Master " + message);
            }catch(IOException e){
                System.err.println("Reducer_sendToMaster: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    private ArrayList<Tuple> loadCache(){
        createCache();
        try{
            synchronized(temp_file) {
                FileInputStream f = new FileInputStream(temp_file);
                return (ArrayList<Tuple>) (new ObjectInputStream(f)).readObject();
            }
        }catch(IOException e){
            System.err.println("Master_loadCache: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println("Master_loadCache: ClassNotFoundException occurred");
            e.printStackTrace();
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
                System.err.println("Master_loadCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    private void updateCache(String query, String h){
        temp_cache.add(new Tuple(query, h));
        try{
            ArrayList<Tuple> temp = loadCache();
            //Tuple tuple = new Tuple(request.getQuery(), request.getData().get(0));
            temp_cache.addAll(temp);
            synchronized(temp_file){ //works fine
                //TODO merge the existing data
                FileOutputStream fo = new FileOutputStream(temp_file);
                ObjectOutputStream out = new ObjectOutputStream(fo);
                out.writeObject(temp_cache);
                out.flush();
                fo.close();
                out.close();
            }
        }catch(FileNotFoundException e){
            System.err.println("Master_updateCache: File Not Found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("Master_updateCache: There was an IO error on openServer");
            e.printStackTrace();
        }
    }

    private void createFile(File file){
        if(!Functions.checkFile(file)){
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
                System.err.println("Master_loadCache: IOException occurred");
                e.printStackTrace();
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
                System.err.println("Master_loadCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        try{
            ServerSocket listenSocket = new ServerSocket(4001);
            while(true){
                try{
                    System.out.println("Waiting for new connection...");
                    Socket connection = listenSocket.accept();
                    new Thread(new Reducer(connection)).start();
                }catch(IOException e){
                    System.err.println("Reducer_main: There was an IO error");
                }
            }
        }catch(IOException e){
            System.err.println("Reducer_main: There was an IO error");
        }
    }
}
