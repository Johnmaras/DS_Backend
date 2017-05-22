import java.util.ArrayList;

public class PolylineAdapter{
    private final ArrayList<LatLngAdapter> points = new ArrayList<>();

    public ArrayList<LatLngAdapter> getPoints() {
        return points;
    }

    public void addPoint(double latitude, double longitude){
        LatLngAdapter newPoint = new LatLngAdapter(latitude, longitude);
        points.add(newPoint);
    }

    public void addPoint(LatLngAdapter latlngPoint){
        points.add(latlngPoint);
    }
}
