import java.io.*;
import java.util.Hashtable;

public class Handler{
    public static void main(String[] args){
        File file = new File("master_cache");
        //File file = new File("master_workers");
        try{
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fi);
            Hashtable<String, String> cache = (Hashtable<String, String>) in.readObject();
            for(String key: cache.keySet()){
                System.out.println(key + " = " + cache.get(key));
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        } catch (IOException e) {
            System.err.println("IO Error");
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found");
        }
    }
}
