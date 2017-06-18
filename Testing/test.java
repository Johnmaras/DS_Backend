import PointAdapter.Coordinates;
import PointAdapter.PolylineAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class test {
    public static void main(String[] args) {

        try {
            File file = new File("worker_cache");
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);

            ArrayList<PolylineAdapter> temp = (ArrayList<PolylineAdapter>)in.readObject();

            fi.close();

            Hashtable<Coordinates, PolylineAdapter> c = new Hashtable<>();
            for(PolylineAdapter pl: temp){
                Coordinates co = new Coordinates(pl.getOrigin(), pl.getDestination());
                c.put(co.round(), pl);
            }

            FileOutputStream fo = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fo);

            out.writeObject(c);
            out.flush();

            /*for(Coordinates co: c.keySet()){
                System.out.println(c.get(co) + "\n");
            }*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
