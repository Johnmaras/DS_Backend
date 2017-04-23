import java.util.ArrayList;

//TODO may add acknowledgements
public class Master_Worker extends Master implements Runnable{

    private String query;
    private int requestType;
    protected Functions functions;

    public Master_Worker(String query, int requestType, Functions functions){
        this.query = query;
        this.requestType = requestType;
        this.functions = functions;
    }

    public Master_Worker(){}

    protected Functions getFunctions(){return functions;}

    public String getQuery(){ return query;}

    @Override
    public void run(){
        ArrayList<Thread> threads = new ArrayList<>();
        if(requestType == 1){
            for(String worker_id: functions.getWorkers().values()){
                threads.add(new Thread(new MW_Search(worker_id, query, 1, functions)));
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
            int query_hash = Math.abs(query.hashCode()) % (functions.getWorkers().size());
            worker_id = functions.getWorkers().get(query_hash) ;
            Thread t = new Thread(new MW_Search(worker_id, query, 2, functions));
            t.start();
            try{
                t.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
