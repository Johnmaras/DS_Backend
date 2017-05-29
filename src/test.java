import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import pointInPolygon.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class test {
    public static void main(String[] args){
        //try {
            /*JsonParser parser = new JsonParser();
            FileReader fr = new FileReader(new File("london.json"));
            Object obj = parser.parse(fr);

            JsonObject obj2 = (JsonObject)obj;
            JsonArray geometries = (JsonArray)obj2.get("geometries");
            JsonObject geo2 = (JsonObject)geometries.get(0);
            JsonArray coordinates = (JsonArray)geo2.get("coordinates"); //has two arrays
            JsonArray array = (JsonArray)coordinates.get(0);
            JsonArray array0 = (JsonArray) array.get(0);

            Iterator it_inner = array0.iterator();
            while(it_inner.hasNext()){
                JsonArray tuple = (JsonArray)it_inner.next();
                System.out.println(tuple);
            }*/

            String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";

            GeoApiContext context = new GeoApiContext().setQueryRateLimit(3)
                    .setConnectTimeout(1, TimeUnit.SECONDS)
                    .setReadTimeout(1, TimeUnit.SECONDS)
                    .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

            try {
                GeocodingResult[] l = GeocodingApi.newRequest(context).address("London").region("uk").await();
                System.out.println(l[0].geometry.location);
            } catch (ApiException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*for(JsonElement array: coordinates.getAsJsonArray()){
                System.out.println(array.getAsDouble());
            }
            System.out.println(coordinates);*/
        /*} catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    private boolean isInPolygon(LatLng point, Polygon polygon){
        Polygon newPolygon;
        for(LatLng p: polygon.getPoints()) {
            newPolygon = Polygon.Builder().addVertex(new Point((float)p.latitude, (float)p.longitude)).build();
        }
    }
}
