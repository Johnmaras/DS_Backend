package Data;

import PointAdapter.LatLngAdapter;
import PointAdapter.PolylineAdapter;

import java.io.*;
import java.util.ArrayList;

public class ShowData{
    public static void main(String[] args){
        try{
            FileInputStream fi = new FileInputStream(new File("worker_cache"));
            ObjectInputStream in = new ObjectInputStream(fi);

            ArrayList<PolylineAdapter> polylines = (ArrayList<PolylineAdapter>)in.readObject();
            for(PolylineAdapter p: polylines){
                for(LatLngAdapter point: p.getPoints()){
                    System.out.println(point);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
