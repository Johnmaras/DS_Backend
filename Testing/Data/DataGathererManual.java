package Data;

import PointAdapter.LatLngAdapter;
import PointAdapter.PolylineAdapter;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DataGathererManual implements Runnable{

    private static final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";

    private static final GeoApiContext context = new GeoApiContext()
            .setQueryRateLimit(3)
            .setConnectTimeout(1, TimeUnit.SECONDS)
            .setReadTimeout(1, TimeUnit.SECONDS)
            .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

    private static final File cache_file = new File("worker_cache");
    private static final ArrayList<PolylineAdapter> cache = new ArrayList<>();

    private static final File jsonFile = new File("cache.json");
    private static final ArrayList<String> jsonCache = new ArrayList<String>();

    private Socket con;

    public DataGathererManual(Socket con){
        this.con = con;
    }

    private static LatLngAdapter toLatLngAdapter(LatLng latLng){
        return new LatLngAdapter(latLng.lat, latLng.lng);
    }

    @Override
    public void run(){
        try{
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());

            double lat = in.readDouble();
            double lng = in.readDouble();
            LatLng origin = new LatLng(lat, lng);

            out.writeBoolean(true);
            out.flush();

            lat = in.readDouble();
            lng = in.readDouble();
            LatLng dest = new LatLng(lat, lng);

            out.writeBoolean(true);
            out.flush();

            PolylineAdapter polyline = new PolylineAdapter();

            DirectionsApiRequest request = DirectionsApi.newRequest(context).origin(origin).destination(dest);

            DirectionsResult result;

            result = request.await();

            if(result != null){
                if(result.routes.length > 0){
                    EncodedPolyline encPolyline = result.routes[0].overviewPolyline;
                    System.out.println(encPolyline);

                    for(LatLng point : encPolyline.decodePath()){
                        polyline.addPoint(toLatLngAdapter(point));
                    }
                }
            }

            synchronized(cache){
                cache.add(polyline);
                synchronized(cache_file){
                    FileOutputStream fo = new FileOutputStream(cache_file);
                    ObjectOutputStream o = new ObjectOutputStream(fo);

                    o.writeObject(cache);
                    o.flush();
                    o.close();
                }
            }

            synchronized(jsonCache){
                jsonCache.add(new Gson().toJson(result));
                synchronized(jsonFile){
                    FileOutputStream fo = new FileOutputStream(jsonFile);
                    ObjectOutputStream o = new ObjectOutputStream(fo);

                    o.writeObject(jsonCache);
                    o.flush();
                    o.close();
                }
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }catch(ApiException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        try{
            ServerSocket listen = new ServerSocket(4000);
            while(true){
                try{
                    Socket new_con = listen.accept();
                    new Thread(new DataGathererManual(new_con)).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




























