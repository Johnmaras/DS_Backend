import java.io.Serializable;

public class Tuple implements Serializable{

    private static final long serialVersionUID = 1314160120198237281L;
    private Coordinates key; //the value is rounded
    private PolylineAdapter value;

    public Tuple(Coordinates key, PolylineAdapter value){
        this.key = key;
        this.value = value;
    }

    public Tuple(){}

    public Coordinates getKey() {
        return key;
    }

    public void setKey(Coordinates key) {
        this.key = key;
    }

    public PolylineAdapter getValue() {
        return value;
    }

    public void setValue(PolylineAdapter value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "key = " + key + ", value = " + value;
    }
}
