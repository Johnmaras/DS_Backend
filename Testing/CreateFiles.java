import PointAdapter.Coordinates;
import PointAdapter.PolylineAdapter;

import java.io.*;
import java.util.Hashtable;

public class CreateFiles{
    public static void main(String[] args) {

        try {
            File file = new File("worker_cache");
            File file1 = new File("worker_cache1");
            File file2 = new File("worker_cache2");
            File file3 = new File("worker_cache3");
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);

            Hashtable<Coordinates, PolylineAdapter> temp = (Hashtable<Coordinates, PolylineAdapter>)in.readObject();

            fi.close();

            int size = temp.size();

            Hashtable<Coordinates, PolylineAdapter> c = new Hashtable<>();
            int i = 0;
            for(Coordinates co: temp.keySet()){

                c.put(co.round(), temp.get(co));

                if(i == size / 3){
                    FileOutputStream fo = new FileOutputStream(file1);
                    ObjectOutputStream out = new ObjectOutputStream(fo);

                    out.writeObject(c);
                    out.flush();
                    c.clear();
                }else if(i == 2 * size / 3){
                    FileOutputStream fo = new FileOutputStream(file2);
                    ObjectOutputStream out = new ObjectOutputStream(fo);

                    out.writeObject(c);
                    out.flush();
                    c.clear();
                }else if(i == size - 1){
                    FileOutputStream fo = new FileOutputStream(file3);
                    ObjectOutputStream out = new ObjectOutputStream(fo);

                    out.writeObject(c);
                    out.flush();
                    c.clear();
                }
                i++;
            }



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

