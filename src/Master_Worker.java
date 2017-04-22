import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

import static java.lang.Thread.sleep;

//TODO fix error messages
//TODO may add acknowledgements
public class Master_Worker extends Master implements Runnable{

    private String query;
    private int requestType;

    public Master_Worker(String query, int requestType){
        this.query = query;
        this.requestType = requestType;
    }

    public String getQuery(){ return query;}

    @Override
    public void run(){
        ArrayList<Thread> threads = new ArrayList<>();
        Functions functions = new Functions(this);
        //TODO threads must be joined
        if(requestType == 1){
            for(String worker_id: functions.getWorkers()){
                threads.add(new Thread(new MW_Search(worker_id, query)));
            }
            threads.forEach(Thread::start);
            /*try {
                sleep((int) (Math.random() * 5000));
            } catch (InterruptedException e) {
                System.err.println("Error on thread " + this.toString());
            }*/
            try{
                for(Thread t: threads){
                    t.join();
                    //System.out.println("Finished " + i);
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }else if(requestType == 2){
            //TODO find specific worker
            //TODO make connection
            //TODO send request
            //TODO get response
            //TODO update cache
        }
    }

    public static void main(String[] args){
        Functions functions = new Functions(new Master_Worker(null, 0));

    }
}
