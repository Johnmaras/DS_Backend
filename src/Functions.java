import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class Functions{
    //TODO files seem to not be created
    private Object node;

    private final File cache_file;
    private Hashtable<String, String> cache; //term(key) and depth in the file(value)

    private final File workers_file; //only the master node has workers file
    private Hashtable<String, Socket> workers;

    public Functions(Object node){
        this.node = node;
        this.cache_file = new File(hash() + "_cache");
        this.cache = loadCache();
        this.workers_file = (this.node.getClass().getName().equals("Master") ? new File(hash() + "_workers") : null);
        this.workers = (this.node.getClass().getName().equals("Master") ? loadWorkers() : null);
    }

    public Hashtable<String, Socket> getWorkers(){ return workers;}

    private Hashtable<String, String> loadCache(){
        createCache();
        try{
            synchronized(cache_file) {
                FileInputStream f = new FileInputStream(cache_file);
                return (Hashtable<String, String>) (new ObjectInputStream(f)).readObject();
            }
        }catch(IOException e){
            System.err.println("Master_loadCache: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println("Master_loadCache: ClassNotFoundException occurred");
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
                System.err.println("Master_loadCache: IOException occurred");
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
            System.err.println("Master_updateCache: File Not Found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("Master_updateCache: There was an IO error on openServer");
            e.printStackTrace();
        }
    }

    public String searchCache(String query){
        return cache.get(query);
    }

    private Hashtable<String, Socket> loadWorkers(){
        createWorkers();
        try{
            FileInputStream f = new FileInputStream(workers_file);
            return (Hashtable<String, Socket>) new ObjectInputStream(f).readObject();
        }catch(IOException e){
            System.err.println("Master_loadWorkers: IOException occurred");
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            System.err.println("Master_loadWorkers: ClassNotFoundException occurred");
            e.printStackTrace();
        }
        return new Hashtable<>();
    }

    private void createWorkers(){
        if(!checkFile(workers_file)){
            Hashtable<String, Socket> temp = new Hashtable<>();
            try{
                synchronized(workers_file){
                    FileOutputStream f = new FileOutputStream(workers_file);
                    ObjectOutputStream out = new ObjectOutputStream(f);
                    out.writeObject(temp);
                    out.flush();
                }
            }catch(IOException e){
                System.err.println("Master_loadCache: IOException occurred");
                e.printStackTrace();
            }
        }
    }

    public void updateWorkers(String worker_id, Socket worker_con){
        workers.put(worker_id, worker_con);
        try{
            Hashtable<String, Socket> temp = loadWorkers();
            workers.putAll(temp);
            synchronized(workers_file){
                FileOutputStream c = new FileOutputStream(workers_file);
                ObjectOutputStream out = new ObjectOutputStream(c);
                out.writeObject(temp);
                out.flush();
                c.close();
                out.close();
            }
        }catch(FileNotFoundException e){
            System.err.println("File not found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("IO Error");
            e.printStackTrace();
        }
    }

    public String hash(){
        //TODO not finished
        //TODO check inheritance problems on class_name
        String class_name = node.getClass().getName();
        if(class_name.equals("Master")){
            return ((Master)node).hash();
        }else if(class_name.equals("Worker")){
            return ((Worker)node).hash();
        }else if(class_name.equals("Reducer")){
            //TODO reducer
            //return ((Reducer)node).hash();
        }
        return null;
    }

    public static boolean checkFile(File file){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine();
            return true;
        }catch(NullPointerException e){
            System.err.println("Functions_checkFile: File not found");
            e.printStackTrace();
        }catch(FileNotFoundException e){
            System.err.println("Functions_checkFile: Error opening file");
            e.printStackTrace();
        }catch (IOException e){
            System.out.println("Functions_checkFile: Sudden end.");
            e.printStackTrace();
        }
        return false;
    }

    public static void createFile(File file){
        if(!checkFile(file)){
            try{
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("");
            }catch(IOException e){
                System.err.println("IO Error");
                e.printStackTrace();
            }
        }
    }
}
