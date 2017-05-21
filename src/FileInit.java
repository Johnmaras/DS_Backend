import java.io.*;
import java.util.Hashtable;

public class FileInit{
    public static void main(String[] args){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(new File("worker_cache_init")));
            Hashtable<String, String> data = new Hashtable<>();
            String line = reader.readLine();
            while(line != null){
                Tuple t = new Tuple(line, Double.toString(line.hashCode() * Math.random()));
                data.put(t.getKey(), t.getValue());
                line = reader.readLine();
            }

            reader.close();

            FileOutputStream fo = new FileOutputStream(new File("worker_" + new Worker(null).hash() + "_cache"));
            ObjectOutputStream out = new ObjectOutputStream(fo);
            out.writeObject(data);
            out.flush();
            out.close();

            //ArrayList<Tuple> data = (ArrayList<Tuple>)temp_data.parallelStream().collect(Collectors.toList());
            for(String key : data.keySet()){
                System.out.println("key = " + key + " value = " + data.get(key));
            }
        }catch(FileNotFoundException e){
            System.err.println("File not found");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("IO Error");
            e.printStackTrace();
        }
    }
}
