import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
//TODO each thread creates a connection to a worker, gets the data
//TODO and stores it into the file(common for all instances).
//TODO When the main Reducer gets the signal form the master,
//TODO reads the file and creates a list of all data.

//TODO manage connections' close
public class Reducer implements Runnable{

    private Socket con;
    private String ID = getID();
    private final File temp = new File("reducer_" + hash() + "_temp");

    public Reducer(Socket con){
        this.con = con;
    }

    public String getID(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            System.err.println("Reducer_getID: Host not found.");
        }
        return null;
    }

    public String hash(){
        return this.ID;
    }

    @Override
    public void run(){
        try{
            //ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            //Message request = (Message)in.readObject();
            String string = Double.toString(Math.random()) + "test";
            Message request = new Message();
            request.setRequestType(7);
            request.setData(string);
            if(request.getRequestType() == 7){
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                for(String s: request.getData()){
                    synchronized(temp){ //TODO works fine
                        writer.write(s);
                        writer.newLine();
                        writer.flush();
                    }
                }
                writer.close();
            }else if(request.getRequestType() == 4){
                ArrayList<String> data = new ArrayList<>();
                synchronized(temp){
                    BufferedReader reader = new BufferedReader(new FileReader(temp));
                    String line = reader.readLine();
                    while(line != null){
                        data.add(line);
                        line = reader.readLine();
                    }
                }
                sendToMaster(request.getQuery(), data);
            }
            con.close();
        }catch(NullPointerException e) {
            System.err.println("Reducer_run: Null pointer occurred");
        }catch(FileNotFoundException e){
            System.err.println("Reducer_run: File not found!");
        }catch(IOException e){
            System.err.println("Reducer_run: IOException occurred");
        }/*catch(ClassNotFoundException e) {
            System.err.println("Reducer_run: Class not found occurred");
        }*/
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
            }catch(IOException e){
                System.err.println("Reducer_sendToMaster: IOException occurred");
            }
        }
    }

    public static void main(String[] args){
        for(int i = 0; i < 100; i++){
            new Thread(new Reducer(null)).start();
        }
        /*try{
            ServerSocket listenSocket = new ServerSocket(4001);
            while(true){
                try{
                    Socket connection = listenSocket.accept();
                    new Thread(new Reducer(connection)).start();
                }catch(IOException e){
                    System.err.println("Reducer_main: There was an IO error");
                }
            }
        }catch(IOException e){
            System.err.println("Reducer_main: There was an IO error");
        }*/
    }
}
