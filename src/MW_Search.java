import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class MW_Search implements Runnable{

    private SocketAddress worker_id;
    private String query;

    public MW_Search(SocketAddress worker_id, String query) {
        this.worker_id = worker_id;
        this.query = query;
    }
    @Override
    public void run() {
        Message request = new Message(1, query);
        Socket WorkerCon = new Socket();
        while(!WorkerCon.isConnected()){
            try{
                WorkerCon.connect(worker_id);
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
                            break;
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
                System.err.println("MW_Search_run: There was an IO error on openServer");
                e.printStackTrace();
            }catch(NullPointerException e){
                System.err.println("MW_Search_run: NullPointer");
                e.printStackTrace();
            }
        }
    }
}
