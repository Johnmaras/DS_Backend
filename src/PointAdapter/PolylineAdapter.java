package PointAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

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

    public void addAllPoint(Iterable<LatLngAdapter> points){
        this.points.addAll((Collection<? extends LatLngAdapter>) points);
        origin = this.points.get(0);
        destination = this.points.get(this.points.size() - 1);
    }

    @Override
    public boolean equals(Object obj) {
        return this.origin.equals(((PolylineAdapter)obj).getOrigin()) && this.destination.equals(((PolylineAdapter)obj).getDestination());
    }

    @Override
    public String toString() {
        return "origin = " + origin + " destination = " + destination + "\npoints:\n" + points;
    }

    public boolean isEmpty(){
        return points.isEmpty();
    }
}
