import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import javafx.scene.shape.Polyline;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DataGather{
    public static void main(String[] args) {
        final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";

        final GeoApiContext context = new GeoApiContext()
                .setQueryRateLimit(3)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

        DirectionsApiRequest request = DirectionsApi.newRequest(context).origin("Toronto").destination("Montreal");
        DirectionsResult result = new DirectionsResult();

        try{
            result = request.await();
        }catch(InterruptedException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (ApiException e){
            e.printStackTrace();
        }

        if(result.routes != null && result.routes.length > 0){
            EncodedPolyline encPolyline = result.routes[0].overviewPolyline;
            Polyline polyline = new Polyline();
            for(LatLng point: encPolyline.decodePath()){

                //polyline.getPoints().;
            }
        }
    }
}
