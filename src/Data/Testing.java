package Data;

import PointAdapter.*;
import com.google.gson.*;
import com.google.maps.internal.LatLngAdapter;

import java.io.*;
import java.util.ArrayList;

public class Testing {
    public static void main(String[] args){

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterDeserializer());
        gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterSerializer());
        gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterDeserializer());
        gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterSerializer());
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        /*PointAdapter.PointAdapter.LatLngAdapter point = new PointAdapter.PointAdapter.LatLngAdapter(3.123, 535.1154);

        String json = gson.toJson(point);
        System.out.println(json);*/


        try {
            File file = new File("worker_cache");
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);
            ArrayList<PolylineAdapter> polylines = (ArrayList<PolylineAdapter>)in.readObject();

            String json = gson.toJson(polylines);

            //System.out.println(json);

            JsonParser parser = new JsonParser();

            Object object = parser.parse(json);
            JsonArray objects = ((JsonElement)object).getAsJsonArray();

            for(JsonElement jo: objects){
                System.out.println(jo);
            }

            /*PointAdapter.PointAdapter.PolylineAdapter pl = gson.fromJson(json, PointAdapter.PointAdapter.PolylineAdapter.class);

            System.out.println(pl.getOrigin());
            System.out.println(pl.getDestination());

            for(PointAdapter.PointAdapter.LatLngAdapter point: pl.getPoints()){
                System.out.println(point);
            }*/
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

        o.add("PointAdapter.Coordinates", outter);

        System.out.println(o);*/
    }
}
