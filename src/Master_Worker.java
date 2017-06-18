import PointAdapter.Coordinates;

import java.util.ArrayList;

public class Master_Worker extends Master implements Runnable{

    private Coordinates query;
    private int requestType;

    public Master_Worker(Coordinates query, int requestType){
        this.query = query;
        this.requestType = requestType;
    }

    public Master_Worker(){}

    @Override
    public void run(){

        ArrayList<Thread> threads = new ArrayList<>();
        if(requestType == 1){
            for(String worker_id: getWorkers().values()){
                threads.add(new Thread(new MW_Search(worker_id, query, 1)));
            }
            threads.forEach(Thread::start);
            try{
                for(Thread t: threads){
                    t.join();
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }else if(requestType == 2){
            String worker_id;
            int query_hash = Math.abs(query.hashCode()) % (getWorkers().size());
            worker_id = getWorkers().get(query_hash) ;
            Thread t = new Thread(new MW_Search(worker_id, query, 2));
            t.start();
            try{
                t.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
