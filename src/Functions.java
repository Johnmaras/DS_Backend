import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Functions{

    private Object node;

    private final File cache_file;
    private Hashtable<String, String> cache; //term(key) and hash(value)

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

    public static String getMasterIP(String config_file){
        String masterIP = null;
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            masterIP = file.filter(s -> s.trim().startsWith("masterIP")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            System.err.println(getTime() + "Functions_getMasterIP: IO Error");
        }
        return masterIP;
    }

    public static int getMasterPort(String config_file){
        String masterPort = "";
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            masterPort = file.filter(s -> s.trim().startsWith("masterPort")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            System.err.println(getTime() + "Functions_getMasterIP: IO Error");
        }
        return Integer.parseInt(masterPort);
    }

    public static void setReducer(String ip, String port, String config_file){
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            List<String> lines = file.collect(Collectors.toList());
            lines.removeIf(s -> s.startsWith("reducerIP"));
            lines.add("reducerIP " + ip);
            lines.removeIf(s -> s.startsWith("reducerPort"));
            lines.add("reducerPort " + port);

            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(config_file)));
            for(String s : lines){
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
        }catch(IOException e){
            System.err.println(getTime() + "Functions_getMasterIP: IO Error");
        }
    }

    public static String getReducerIP(String config_file){
        String reducerIP = null;
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            reducerIP = file.filter(s -> s.trim().startsWith("reducerIP")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            System.err.println(getTime() + "Functions_getMasterIP: IO Error");
        }
        return reducerIP;
    }

    public static int getReducerPort(String config_file){
        String reducerPort = "0";
        try{
            Stream<String> file = Files.lines(Paths.get(config_file));
            reducerPort = file.filter(s -> s.trim().startsWith("reducerPort")).map(s -> s.trim().substring(s.indexOf(" ")).trim()).findFirst().get();
        }catch(IOException e){
            System.err.println(getTime() + "Functions_getReducerPort: IO Error");
        }
        return Integer.parseInt(reducerPort);
    }

    public static String getTime(){
        return String.format("%d:%d:%d.%d ", LocalDateTime.now().getHour(), 
                                            LocalDateTime.now().getMinute(), 
                                            LocalDateTime.now().getSecond(), 
                                            LocalDateTime.now().getNano());
    }
}
