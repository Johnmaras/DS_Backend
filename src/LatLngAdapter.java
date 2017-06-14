import java.io.Serializable;

public class LatLngAdapter implements Serializable{

    private static final long serialVersionUID = 8314160120198237281L;
    private double latitude;
    private double longitude;

    public LatLngAdapter(){
        this.latitude = 0;
        this.longitude = 0;
    }

    public LatLngAdapter(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object obj){
        double lat2 = ((LatLngAdapter)obj).getLatitude();
        double lng2 = ((LatLngAdapter)obj).getLongitude();

        return latitude == lat2 && longitude == lng2;
    }

    @Override
    public String toString() {
        return "latitude = " + latitude + " longitude = " + longitude;
    }
}
