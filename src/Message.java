import java.io.Serializable;
import java.util.ArrayList;

//TODO use Generics
public class Message implements Serializable {

	private static final long serialVersionUID = 2314160120198237281L;
	private int requestType;
	private String query;
	private ArrayList<String> data = new ArrayList<>();

	public Message(int requestType, String query, ArrayList<String> data){
		this.requestType = requestType;
		this.query = query;
		this.data = data;
	}

	public Message(int requestType, String query){
		this.requestType = requestType;
		this.query = query;
	}

	public Message(){
		this.requestType = 0;
		this.query = null;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		this.requestType = requestType;
	}

	public ArrayList<String> getData() {
		return data;
	}

	public void setData(ArrayList<String> data) {
		this.data = data;
	}

	public void setData(String data){
		this.data.add(data);
	}

	public String toString() {
		String rt = requestType + " - ";
		for(String s: data){
			rt += s + "_";
		}
		return rt;
	}
}
