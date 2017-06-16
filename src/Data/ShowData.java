package Data;

import com.google.gson.Gson;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;

import java.io.*;
import java.util.ArrayList;

public class ShowData{
    public static void main(String[] args){
        try{
            FileInputStream fi = new FileInputStream(new File("cache.json"));
            ObjectInputStream in = new ObjectInputStream(fi);

            ArrayList<String> polylines = (ArrayList<String>)in.readObject();
            for(String p: polylines){
                /*for(LatLngAdapter point: p.getPoints()){
                    System.out.println(point);
                }*/
                DirectionsResult result = new Gson().fromJson(p, DirectionsResult.class);
                if(result != null){
                    if(result.routes.length > 0){
                        EncodedPolyline encPolyline = result.routes[0].overviewPolyline;
                        System.out.println(encPolyline);

                        for(LatLng point : encPolyline.decodePath()){
                            System.out.print(point);
                        }
                        System.out.println();
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
