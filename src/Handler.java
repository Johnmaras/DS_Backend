import java.io.*;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;

public class Handler{
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        while(!input.equals("quit")){
            if(input.equals("master.cache")){
                File file = new File("master_192.168.1.67_cache");
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ObjectInputStream in = new ObjectInputStream(fi);
                    Hashtable<String, String> cache = (Hashtable<String, String>) in.readObject();
                    for(String key : cache.keySet()){
                        System.out.println("key = " + key + " value = " + cache.get(key));
                    }
                    fi.close();
                    in.close();
                } catch (FileNotFoundException e) {
                    System.err.println("File not found");
                } catch (IOException e) {
                    System.err.println("IO Error");
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found");
                }
            }else if(input.equals("master.workers")){
                File file = new File("master_192.168.1.67_workers");
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ObjectInputStream in = new ObjectInputStream(fi);
                    Hashtable<Integer, String> cache = (Hashtable<Integer, String>) in.readObject();
                    for(Integer key : cache.keySet()){
                        System.out.println("key = " + key + " value = " + cache.get(key));
                    }
                    fi.close();
                    in.close();
                } catch (FileNotFoundException e) {
                    System.err.println("File not found");
                } catch (IOException e) {
                    System.err.println("IO Error");
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found");
                }
            }else if(input.equals("worker")){
                File file= new File("worker_192.168.1.67_cache");
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ObjectInputStream in = new ObjectInputStream(fi);
                    Hashtable<String, String> cache = (Hashtable<String, String>) in.readObject();
                    for(String key : cache.keySet()){
                        System.out.println("key = " + key + " value = " + cache.get(key));
                    }
                    fi.close();
                    in.close();
                } catch (FileNotFoundException e) {
                    System.err.println("File not found");
                } catch (IOException e) {
                    System.err.println("IO Error");
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found");
                }
            }else if(input.equals("reducer")){
                File file= new File("reducer_192.168.1.67_temp");
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ObjectInputStream in = new ObjectInputStream(fi);
                    ArrayList<Tuple> cache = (ArrayList<Tuple>)in.readObject();
                    for(Object key: cache){
                        System.out.println(key);
                    }
                    fi.close();
                    in.close();
                } catch (FileNotFoundException e) {
                    System.err.println("File not found");
                } catch (IOException e) {
                    System.err.println("IO Error");
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found");
                }
            }
            input = scanner.nextLine();
        }
    }
}
