import PointAdapter.PolylineAdapter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class test {
    public static void main(String[] args) {

        try {
            File file = new File("worker_cache");
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);

            HashSet<PolylineAdapter> cache = (HashSet<PolylineAdapter>)in.readObject();

            fi.close();

            ArrayList<PolylineAdapter> cache_array = new ArrayList<>();
            cache_array.addAll(cache);

            FileOutputStream fo = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fo);

            out.writeObject(cache_array);
            out.flush();
            fo.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
