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
        boolean flag = false;
        //String ID;
        while(true){
            while(MasterCon == null){
                try {
                    MasterCon = new Socket(InetAddress.getByName("192.168.1.67"), 4000);
                    //ID = MasterCon.getLocalSocketAddress().toString();
                } catch (NullPointerException e) {
                    System.err.println("Client_main: Null pointer occurred. Trying again");
                } catch (UnknownHostException e) {
                    System.err.println("Client_main: You are trying to connect to an unknown host!");
                } catch (IOException e) {
                    System.err.println("Client_main: There was an IO error");
                }
            }
            while(true){
                //TODO check if connection is still valid

                Scanner scanner = new Scanner(System.in);
                System.out.print("Give the string you want to search: ");
                String query = scanner.nextLine();
                if(MasterCon.isClosed()){
                    flag = true;
                    break;
                }
                Message request = new Message(9, query);
                try{
                    ObjectOutputStream out = new ObjectOutputStream(MasterCon.getOutputStream());
                    out.writeObject(request);
                    out.flush();
                    if(query.equals("quit")) {
                        flag = true;
                        break;
                    }
                    System.out.println("Waiting for the answer");
                    ObjectInputStream in = new ObjectInputStream(MasterCon.getInputStream());
                    request = (Message)in.readObject();
                    ArrayList<String> data = request.getData();
                    data.forEach(System.out::println);
                }catch(IOException e){
                    System.err.println("There was an IO error on openServer");
                    e.printStackTrace();
                }catch(NullPointerException e){
                    System.err.println("Null Pointer!");
                }catch(ClassNotFoundException e){
                    System.err.println("Class Not Found");
                }
            }
            if(flag) break;
        }

    }
}
