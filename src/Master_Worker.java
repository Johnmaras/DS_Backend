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
        //functions = new Functions(this);
        if(requestType == 1){
            for(String worker_id: functions.getWorkers().values()){
                threads.add(new Thread(new MW_Search(worker_id, query, 1, functions)));
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
            String worker_id;
            int query_hash = Math.abs(query.hashCode()) % (functions.getWorkers().size());
            //System.out.println(query + " = " + query.hashCode());
            //System.out.println("query_hash " + " = " + query_hash);
            worker_id = functions.getWorkers().get(query_hash) ;
            //System.out.println("worker_id = " + worker_id);
            Thread t = new Thread(new MW_Search(worker_id, query, 2, functions));
            t.start();
            try{
                t.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    /*public static void main(String[] args){
        Functions functions = new Functions(new Master_Worker(null, 0));

    }*/
}
