import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Master implements Runnable{

    private Socket connection;

    public Master(Socket con){
        this.connection = con;
    }

    @Override
    public void run() {
        Functions functions = new Functions();
        try{
            Message message;
            String query;
            do{
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                message = (Message)in.readObject();
                query = message.getQuery();
                if(query.equals("quit")) break;
                functions.updateCache(query);
                System.out.println(query + " added.");
                message = new Message();
                message.setData(functions.searchCache(query));
                out.writeObject(message);
                out.flush();
            }while(true);
            System.out.println("Connection finished");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
                    System.err.println("Master_main: There was an IO error");
                }
            }
        }catch(IOException e){
            System.err.println("Master_main: There was an IO error");
        }
    }
}
