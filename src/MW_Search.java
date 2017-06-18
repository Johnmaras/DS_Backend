import Messages.Message;
import PointAdapter.Coordinates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class MW_Search extends Master_Worker implements Runnable{

    private String worker_id;
    private Coordinates query;
    private int requestType;

    public MW_Search(String worker_id, Coordinates query, int requestType) {
        this.worker_id = worker_id;
        this.query = query;
        this.requestType = requestType;
    }
    @Override
    public void run() {
        Message request = new Message(requestType, query);
        try{
            String[] worker_creds = worker_id.trim().split("#");
            String worker_ip = worker_creds[0].trim();
            int worker_port = Integer.parseInt(worker_creds[1]);
            InetSocketAddress worker = new InetSocketAddress(InetAddress.getByName(worker_ip), worker_port);
            Socket WorkerCon = new Socket();
            WorkerCon.connect(worker, 3000);
            ObjectOutputStream out = new ObjectOutputStream(WorkerCon.getOutputStream());
            out.writeObject(request);
            out.flush();

            //waits for answer from the worker
            try{
                ObjectInputStream in = new ObjectInputStream(WorkerCon.getInputStream());
                request = (Message)in.readObject();
                if(request.getRequestType() == 5){
                    System.out.println(Functions.getTime() + " worker done " + worker_id);
                }else if(request.getRequestType() == 6){ //6 means get the results form the worker
                    updateCache(request.getQuery(), request.getResults().get(0)); //data.get(0) must not contain null
                }
            }catch(NullPointerException e){
                Functions.printErr(this.toString(), "Null Pointer!");
            }catch(SocketTimeoutException e){
                Functions.printErr(this.toString(), "Socket Time Out!");
            }catch(ClassNotFoundException e){
                Functions.printErr(this.toString(), "Class Not Found");
            }

            WorkerCon.close();
        }catch(UnknownHostException e){
            Functions.printErr(this.toString(), "You are trying to connect to an unknown host!");
        }catch(IOException e){
            Functions.printErr(this.toString(), "There was an IO error. Host " + worker_id + " seems to be down!");
        }catch(NullPointerException e){
            Functions.printErr(this.toString(), "NullPointer");
        }
    }
}
