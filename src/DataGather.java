import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import pointInPolygon.Point;
import pointInPolygon.Polygon;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

//getting points inside the London center is not working as expected. Does not end in the appropriate time
public class DataGather{

    private static final File london_file = new File("london.json");
    private static final File cache_file = new File("worker_cache");
    private static final Polygon.Builder londonBounds = new Polygon.Builder();
    private static final Polygon.Builder londonCenterBounds = new Polygon.Builder();


    private static LatLng toLatLng(LatLngAdapter latLngAdapter){
        return new LatLng(latLngAdapter.getLatitude(), latLngAdapter.getLongitude());
    }

    private static LatLngAdapter toLatLngAdapter(LatLng latLng){
        return new LatLngAdapter(latLng.lat, latLng.lng);
    }

    private static void getLondon(){
        synchronized (london_file){
            try{
                JsonParser parser = new JsonParser();
                FileReader fr = new FileReader(london_file);
                Object obj = parser.parse(fr);

                JsonObject obj2 = (JsonObject)obj;
                JsonArray geometries = (JsonArray)obj2.get("geometries");
                JsonObject geo2 = (JsonObject)geometries.get(0);
                JsonArray coordinates = (JsonArray)geo2.get("coordinates"); //has two arrays
                JsonArray array = (JsonArray) coordinates.get(0);
                JsonArray array0 = (JsonArray) array.get(0);

                Iterator it_inner = array0.iterator();
                while(it_inner.hasNext()){
                    JsonArray tuple = (JsonArray)it_inner.next();
                    double longitude = tuple.get(0).getAsDouble();
                    double latitude = tuple.get(1).getAsDouble();

                    londonBounds.addVertex(new Point((float)latitude, (float)longitude));
                }
            } catch (FileNotFoundException e) {
                System.err.println("File not found");
            }
        }
    }

    private static void getLondonCenter(){
        Point londonUpperLeft = new Point(51.53395f, -0.19149f);
        Point londonUpperRight = new Point(51.52306f, -0.07373f);
        Point londonDownLeft = new Point(51.49720f, -0.18771f);
        Point londonDownRight = new Point(51.50447f, -0.06858f);

        londonCenterBounds.addVertex(londonUpperLeft);
        londonCenterBounds.addVertex(londonUpperRight);
        londonCenterBounds.addVertex(londonDownRight);
        londonCenterBounds.addVertex(londonDownLeft);
    }

    public static void main(String[] args) {
        final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";

        final GeoApiContext context = new GeoApiContext()
                .setQueryRateLimit(3)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

        getLondonCenter();

        ArrayList<PolylineAdapter> polylines = new ArrayList<>();

        for(int i = 0; i < 5; i++) {

            Point dest;
            Point origin;

            synchronized(londonBounds) {
                do {
                    double lat = Math.random() * 100;
                    double lng = Math.random() * 100;
                    lng *= Math.random() >= 0.5 ? -1 : 1; //50% chance of getting a negative longitude
                    origin = new Point((float) lat, (float) lng);
                } while (!londonCenterBounds.build().contains(origin));
                //} while (!londonBounds.build().contains(origin));

                do {
                    double lat = Math.random() * 100;
                    double lng = Math.random() * 100;
                    lng *= Math.random() >= 0.5 ? -1 : 1; //50% chance of getting a negative longitude
                    dest = new Point((float) lat, (float) lng);
                } while (!londonCenterBounds.build().contains(dest));
                //} while (!londonBounds.build().contains(dest));
            }

            PolylineAdapter polyline = new PolylineAdapter();

            //return Double.toString(query.hashCode() * Math.random());
            LatLng o = new LatLng(origin.x, origin.y);
            LatLng d = new LatLng(dest.x, dest.y);
            DirectionsApiRequest request = DirectionsApi.newRequest(context).origin(o).destination(d);

            DirectionsResult result;
            try {
                result = request.await();

                if (result != null) {
                    if (result.routes.length > 0) {
                        EncodedPolyline encPolyline = result.routes[0].overviewPolyline;

                        for (LatLng point : encPolyline.decodePath()) {
                            polyline.addPoint(toLatLngAdapter(point));
                        }
                    }
                }

                polylines.add(polyline);
            } catch (ApiException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        synchronized(cache_file){
            try {
                FileOutputStream fo = new FileOutputStream(cache_file);
                ObjectOutputStream out = new ObjectOutputStream(fo);
                out.writeObject(polylines);
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
