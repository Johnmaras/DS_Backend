package PointAdapter;

import java.io.Serializable;

public class Coordinates implements Serializable{

    private static final long serialVersionUID = 5314160120198237281L;

    private LatLngAdapter origin;
    private LatLngAdapter destination;

    public Coordinates(){}

    public Coordinates(LatLngAdapter origin, LatLngAdapter destination){
        this.origin = origin;
        this.destination = destination;
    }

    public LatLngAdapter getOrigin(){
        return this.origin;
    }

    public LatLngAdapter getDestination() {
        return this.destination;
    }

    public Coordinates round(){
        double origin_lat = (int)(origin.getLatitude() * 100) / 100.0;
        double origin_lng = (int)(origin.getLongitude() * 100) / 100.0;

        double dest_lat = (int)(destination.getLatitude() * 100) / 100.0;
        double dest_lng = (int)(destination.getLongitude() * 100) / 100.0;

        return new Coordinates(new LatLngAdapter(origin_lat, origin_lng), new LatLngAdapter(dest_lat, dest_lng));
    }

    @Override
    public boolean equals(Object obj){
        Coordinates temp_this = this.round();
        obj = ((Coordinates)obj).round();
        return temp_this.getOrigin().equals(((Coordinates)obj).getOrigin()) && temp_this.getDestination().equals(((Coordinates)obj).getDestination());
    }

    @Override
    public int hashCode() {
        return origin.hashCode() + destination.hashCode();
    }

    @Override
    public String toString() {
        return "origin = " + origin + " destination = " + destination;
    }
}
