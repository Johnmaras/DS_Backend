import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Hashtable;

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
        Functions functions = new Functions(this);
        if(requestType == 1){
            for(Socket worker: functions.getWorkers().values()){
                new Thread(new MW_Search(worker, query)).start();
            }
        }else if(requestType == 2){
            //TODO find specific worker
            //TODO make connection
            //TODO send request
            //TODO get response
            //TODO update cache
        }
    }
}
