import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    public static void main(String[] args){
        Socket MasterCon = null;
        String ID;
        while(MasterCon == null){
            try{
                MasterCon = new Socket(InetAddress.getByName("127.0.0.1"), 4000);
                ID = MasterCon.getLocalSocketAddress().toString();
            }catch(NullPointerException e){
                System.err.println("Client_main: Null pointer occurred. Trying again");
            }catch(UnknownHostException e){
                System.err.println("Client_main: You are trying to connect to an unknown host!");
            }catch(IOException e){
                System.err.println("Client_main: There was an IO error");
            }
        }
        while(true){
            //TODO check if connection is still valid
            Scanner scanner = new Scanner(System.in);
            System.out.print("Give the string you want to search: ");
            String query = scanner.nextLine();
            Message request = new Message(9, query);
            try{
                ObjectOutputStream out = new ObjectOutputStream(MasterCon.getOutputStream());
                out.writeObject(request);
                out.flush();
                if(query.equals("quit")) break;
                ObjectInputStream in = null;
                while(in == null){
                    try{
                        in = new ObjectInputStream(MasterCon.getInputStream());
                        request = (Message)in.readObject();
                        ArrayList<String> data = request.getData();
                        data.forEach(System.out::println);
                    }catch(NullPointerException e){
                        System.err.println("Null Pointer!");
                    }catch(SocketTimeoutException e){
                        System.err.println("Socket Time Out!");
                    }catch(ClassNotFoundException e){
                        System.err.println("Class Not Found");
                    }
                }
            }catch(IOException e){
                System.err.println("Worker_connectToMaster: There was an IO error on openServer");
            }
        }
    }
}
