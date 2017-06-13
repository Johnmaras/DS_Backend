import java.util.ArrayList;

public class PolylineAdapter{

    //TODO assign values
    /**
     * full precision values
     */
    private LatLngAdapter origin;
    private LatLngAdapter destination;

    private final ArrayList<LatLngAdapter> points = new ArrayList<>();

    public ArrayList<LatLngAdapter> getPoints() {
        return points;
    }

    public void addPoint(double latitude, double longitude){
        LatLngAdapter newPoint = new LatLngAdapter(latitude, longitude);
        points.add(newPoint);
    }

    public LatLngAdapter getOrigin(){
        return this.origin;
    }

    public LatLngAdapter getDestination() {
        return this.destination;
    }

    public void addPoint(LatLngAdapter latlngPoint){
        points.add(latlngPoint);
    }
}
