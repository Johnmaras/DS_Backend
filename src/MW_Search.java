import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class MW_Search implements Runnable{

    private String worker_id;
    private String query;

    public MW_Search(String worker_id, String query) {
        this.worker_id = worker_id;
        this.query = query;
    }
    @Override
    public void run() {
        Message request = new Message(1, query);
        try{
            //System.out.println(worker_id);
            //TODO take long to throw exception. make it faster. DONE
            InetSocketAddress worker = new InetSocketAddress(InetAddress.getByName(worker_id), 4002);
            Socket WorkerCon = new Socket();
            WorkerCon.connect(worker, 1000);
            ObjectOutputStream out = new ObjectOutputStream(WorkerCon.getOutputStream());
            out.writeObject(request);
            out.flush();

            //waits for answer from the worker
            ObjectInputStream in = null;
            while(in == null){
                try{
                    in = new ObjectInputStream(WorkerCon.getInputStream());
                    request = (Message)in.readObject();
                    if(request.getRequestType() == 5){
                        System.out.print(System.nanoTime() + " worker done.");
                    }
                }catch(NullPointerException e){
                    System.err.println("MW_Search_run: Null Pointer!");
                }catch(SocketTimeoutException e){
                    System.err.println("MW_Search_run: Socket Time Out!");
                }catch(ClassNotFoundException e){
                    System.err.println("MW_Search_run: Class Not Found");
                }
            }
        }catch(UnknownHostException e){
            System.err.println("MW_Search_run: You are trying to connect to an unknown host!");
            e.printStackTrace();
        }catch(IOException e){
            System.err.println("MW_Search_run: There was an IO error. Host " + worker_id + " seems to be down!");
        }catch(NullPointerException e){
            System.err.println("MW_Search_run: NullPointer");
            e.printStackTrace();
        }

    }
}
