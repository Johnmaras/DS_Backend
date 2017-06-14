import java.io.Serializable;
import java.util.ArrayList;

public class PolylineAdapter implements Serializable{

    private static final long serialVersionUID = 7314160120198237281L;
    //TODO assign values
    /**
     * full precision values
     */
    private LatLngAdapter origin;
    private LatLngAdapter destination;
    private static boolean first = true;

    private final ArrayList<LatLngAdapter> points = new ArrayList<>();

    public ArrayList<LatLngAdapter> getPoints() {
        return points;
    }

    public void addPoint(double latitude, double longitude){
        LatLngAdapter newPoint = new LatLngAdapter(latitude, longitude);
        points.add(newPoint);
        if(first) {
            origin = newPoint;
            first = false;
        }
        destination = newPoint;
    }

    public LatLngAdapter getOrigin(){
        return this.origin;
    }

    public LatLngAdapter getDestination() {
        return this.destination;
    }

    public void addPoint(LatLngAdapter latlngPoint){
        points.add(latlngPoint);
        origin = points.get(0);
        destination = points.get(points.size() - 1);
    }
}
