import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reducer implements Runnable{

    private Socket con;
    private String ID = "10.25.199.229";
    private final File temp_file = new File("reducer_" + hash() + "_temp");
    private ArrayList<Tuple> temp_cache = loadCache();
    private String config = "config_reducer";

    public Reducer(Socket con){
        this.con = con;
    }

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
                ArrayList<String> data = new ArrayList<>(); //there is always only one item in the data coming from the workers
                String query = request.getQuery();
                try{
                    synchronized(temp_file){
                        FileInputStream fi = new FileInputStream(temp_file);
                        ObjectInputStream input = new ObjectInputStream(fi);

                        ArrayList<Tuple> temp_data = (ArrayList<Tuple>)input.readObject();

                        Stream stream = temp_data.parallelStream().filter(s -> s.getKey().equals(query)).map(s -> !s.getValue().equals("null") ? s.getValue() : null);
                        try{
                            data = (ArrayList<String>)stream.collect(Collectors.toList());
                        }catch(NullPointerException e){
                            System.err.println(Functions.getTime() + con.getLocalSocketAddress() + " Stream is empty!");
                        }
                        System.out.print(System.nanoTime() + " ");
                        data.forEach(System.out::println);
                    }
                }catch(FileNotFoundException e){
                    System.err.println(Functions.getTime() + "Reducer_run: File not found");
                    e.printStackTrace();
                }
                sendToMaster(request.getQuery(), data);
                clearFile(temp_file);
            }
        }catch(NullPointerException e){
            System.err.println(Functions.getTime() + "Reducer_run: Null pointer occurred");
        }catch(FileNotFoundException e){
            System.err.println(Functions.getTime() + "Reducer_run: File not found!");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Reducer_run: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println(Functions.getTime() + "Reducer_run: Class not found occurred");
        }
    }

    private void sendToMaster(String query, ArrayList<String> data){
        Message message = new Message(8, query, data);
        ObjectOutputStream out = null;
        while(out == null){
            try{
                out = new ObjectOutputStream(con.getOutputStream());
                out.writeObject(message);
                out.flush();
                System.out.println("Sent to Master " + message);
            }catch(IOException e){
                System.err.println(Functions.getTime() + "Reducer_sendToMaster: IOException occurred");
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
            System.err.println(Functions.getTime() + "Master_loadCache: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println(Functions.getTime() + "Master_loadCache: ClassNotFoundException occurred");
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
                System.err.println(Functions.getTime() + "Master_createCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    private void updateCache(String query, String h){
        temp_cache.add(new Tuple(query, h));
        try{
            ArrayList<Tuple> temp = loadCache();
            temp_cache.addAll(temp);
            synchronized(temp_file){ //works fine
                FileOutputStream fo = new FileOutputStream(temp_file);
                ObjectOutputStream out = new ObjectOutputStream(fo);
                out.writeObject(temp_cache);
                out.flush();
                fo.close();
                out.close();
            }
        }catch(FileNotFoundException e){
            System.err.println(Functions.getTime() + "Master_updateCache: File Not Found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Master_updateCache: There was an IO error");
            e.printStackTrace();
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
                System.err.println(Functions.getTime() + "Master_loadCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    private void masterHandshake(){
        Socket handCon = null;
        while(handCon == null){
            try{
                handCon = new Socket(InetAddress.getByName(Functions.getMasterIP(config)), Functions.getMasterPort(config));
                ObjectOutputStream out = new ObjectOutputStream(handCon.getOutputStream());
                Message message = new Message();
                message.setQuery("Reducer");
                ArrayList<String> data = new ArrayList<>();
                data.add(ID);
                data.add(Integer.toString(4001));
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
        new Reducer(null).masterHandshake();
        try{
            ServerSocket listenSocket = new ServerSocket(4001);
            while(true){
                try{
                    System.out.println("Waiting for new connection...");
                    Socket connection = listenSocket.accept();
                    new Thread(new Reducer(connection)).start();
                }catch(IOException e){
                    System.err.println(Functions.getTime() + "Reducer_main: There was an IO error 1");
                }
            }
        }catch(IOException e){
            System.err.println(Functions.getTime() + "Reducer_main: There was an IO error 2");
        }
    }
}
