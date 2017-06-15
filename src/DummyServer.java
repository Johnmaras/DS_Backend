import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class DummyServer{
    public static void main(String[] args){

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        try {
            ServerSocket server = new ServerSocket(4000);
            while(true) {
                Socket con = server.accept();

                System.out.println(Functions.getTime() + "Connection accepted: " + con.toString());

                ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());

                File file = new File("worker_cache");
                FileInputStream fi = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fi);

                ArrayList<PolylineAdapter> polylines = (ArrayList<PolylineAdapter>)in.readObject();

                String json = gson.toJson(polylines);

                out.writeObject(json); //writeUTF has a limit that is exceeded by json
                out.flush();

                /*out.writeLong(file.length());
                out.flush();

                byte[] fileOut = Files.readAllBytes(Paths.get(file.getPath()));
                out.write(fileOut);
                out.flush();*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
