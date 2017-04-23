import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

public class Functions{

    private Object node;

    private final File cache_file;
    private Hashtable<String, String> cache; //term(key) and depth in the file(value)

    private final File workers_file; //only the master node has workers file
    private Hashtable<Integer, String> workers;

    public Functions(Object node){
        this.node = node;
        this.cache_file = new File(hash() + "_cache");
        this.cache = loadCache();
        this.workers_file = (this.node.toString().equals("Master") ? new File(hash() + "_workers") : null);
        this.workers = (this.node.toString().equals("Master") ? loadWorkers() : null);
    }

    public Hashtable<Integer, String> getWorkers(){
        return loadWorkers();
    }

    private Hashtable<String, String> loadCache(){
        createCache();
        try{
            synchronized(cache_file) {
                FileInputStream f = new FileInputStream(cache_file);
                return (Hashtable<String, String>) (new ObjectInputStream(f)).readObject();
            }
        }catch(IOException e){
            System.err.println(getTime() + "Master_loadCache: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println(getTime() + "Master_loadCache: ClassNotFoundException occurred");
            e.printStackTrace();
        }
        return new Hashtable<>();
    }

    private void createCache(){
        if(!checkFile(cache_file)){
            Hashtable<String, String> temp = new Hashtable<>();
            try{
                synchronized(cache_file) {
                    FileOutputStream f = new FileOutputStream(cache_file);
                    ObjectOutputStream out = new ObjectOutputStream(f);
                    out.writeObject(temp);
                    out.flush();
                }
            }catch(IOException e){
                System.err.println(getTime() + "Master_loadCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    public void updateCache(String query, String h){
        //String length = Integer.toString(query.length());
        cache.put(query, h);
        try{
            Hashtable<String, String> temp = loadCache();
            cache.putAll(temp);
            synchronized(cache_file){
                FileOutputStream c = new FileOutputStream(cache_file);
                ObjectOutputStream out = new ObjectOutputStream(c);
                out.writeObject(cache);
                out.flush();
                c.close();
                out.close();
            }
        }catch(FileNotFoundException e){
            System.err.println(getTime() + "Master_updateCache: File Not Found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println(getTime() + "Master_updateCache: There was an IO error");
            e.printStackTrace();
        }
    }

    public String searchCache(String query){
        cache = loadCache();
        return cache.get(query);
    }

    private Hashtable<Integer, String> loadWorkers(){
        createWorkers();
        try{
            synchronized(workers_file) {
                FileInputStream f = new FileInputStream(workers_file);
                return (Hashtable<Integer, String>) new ObjectInputStream(f).readObject();
            }
        }catch(IOException e){
            System.err.println(getTime() + "Master_loadWorkers: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println(getTime() + "Master_loadWorkers: ClassNotFoundException occurred");
            e.printStackTrace();
        }
        return new Hashtable<>();
    }

    private void createWorkers(){
        if(!checkFile(workers_file)){
            Hashtable<Integer, String> temp = new Hashtable<>();
            try{
                synchronized(workers_file){
                    FileOutputStream f = new FileOutputStream(workers_file);
                    ObjectOutputStream out = new ObjectOutputStream(f);
                    out.writeObject(temp);
                    out.flush();
                }
            }catch(IOException e){
                System.err.println(getTime() + "Master_createWorkers: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    public void updateWorkers(String worker_id){
        if(!workers.contains(worker_id)){
            workers.put(workers.size(), worker_id);
            try{
                Hashtable<Integer, String> temp = loadWorkers();
                temp.putAll(workers);
                synchronized (workers_file) {
                    FileOutputStream c = new FileOutputStream(workers_file);
                    ObjectOutputStream out = new ObjectOutputStream(c);
                    out.writeObject(temp);
                    out.flush();
                    c.close();
                    out.close();
                }
            }catch(FileNotFoundException e){
                System.err.println(getTime() + "Functions_updateWorkers: File not found");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println(getTime() + "Functions_updateWorkers: IO Error");
                e.printStackTrace();
            }
        }
    }

    public String hash(){
        String class_name = this.node.toString();
        if(class_name.equals("Master")){
            return "master_" + ((Master)node).hash();
        }else if(class_name.equals("Worker")){
            return "worker_" + ((Worker)node).hash();
        }
        return null;
    }

    public static boolean checkFile(File file){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine();
            return true;
        }catch(NullPointerException e){
            System.err.println(getTime() + "Functions_checkFile: File not found " + file.getName());
        }catch(FileNotFoundException e){
            System.err.println(getTime() + "Functions_checkFile: Error opening file " + file.getName());
        }catch (IOException e){
            System.out.println("Functions_checkFile: Sudden end. " + file.getName());
        }
        return false;
    }

    public static void createFile(File file){
        if(!checkFile(file)){
            try{
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("");
                writer.close();
            }catch(IOException e){
                System.err.println(getTime() + "Functions_createFile: IO Error" + file.getName());
            }
        }
    }

    public void clearFiles(){
        clearCache();
        clearWorkers();
    }

    public void clearCache(){
        Hashtable<String, String> master_cache = new Hashtable<>();
        try{
            synchronized(cache_file){
                FileOutputStream f = new FileOutputStream(cache_file);
                ObjectOutputStream out = new ObjectOutputStream(f);
                out.writeObject(master_cache);
                out.flush();
                out.close();
            }
        }catch(IOException e){
            System.err.println(getTime() + "Functions_clearCache: IOException occurred");
            e.printStackTrace();
        }
    }

    public void clearWorkers(){
        Hashtable<Integer, String> master_workers = new Hashtable<>();
        try{
            synchronized(workers_file){
                FileOutputStream f = new FileOutputStream(workers_file);
                ObjectOutputStream out = new ObjectOutputStream(f);
                out.writeObject(master_workers);
                out.flush();
                out.close();
            }
        }catch(IOException e){
            System.err.println(getTime() + "Functions_clearWorkers: IOException occurred");
            e.printStackTrace();
        }
    }

    public static String getTime(){
        return String.format("%d:%d:%d.%d ", LocalDateTime.now().getHour(), 
                                            LocalDateTime.now().getMinute(), 
                                            LocalDateTime.now().getSecond(), 
                                            LocalDateTime.now().getNano());
    }
}
