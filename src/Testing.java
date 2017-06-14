import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;

public class Testing {
    public static void main(String[] args){

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterDeserializer());
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        /*LatLngAdapter point = new LatLngAdapter(3.123, 535.1154);

        String json = gson.toJson(point);
        System.out.println(json);*/


        try {
            File file = new File("worker_cache");
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);
            ArrayList<PolylineAdapter> polylines = (ArrayList<PolylineAdapter>)in.readObject();

            String json = gson.toJson(polylines);

            PolylineAdapter pl = gson.fromJson(json, PolylineAdapter.class);

            System.out.println(pl.getOrigin());
            System.out.println(pl.getDestination());

            for(LatLngAdapter point: pl.getPoints()){
                System.out.println(point);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        /*JsonObject o = new JsonObject();
        JsonArray co1 = new JsonArray();
        co1.add(3.4);
        co1.add(5.32);

        JsonArray co2 = new JsonArray();
        co2.add(1.43);
        co2.add(764.960);

        JsonArray outter = new JsonArray();
        outter.add(co1);
        outter.add(co2);

        o.add("Coordinates", outter);

        System.out.println(o);*/
    }
}
