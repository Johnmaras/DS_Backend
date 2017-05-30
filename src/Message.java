import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{

	private static final long serialVersionUID = 2314160120198237281L;
	private int requestType;
	private String query;
	private ArrayList<String> results = new ArrayList<>();

	public Message(int requestType, String query, ArrayList<String> results){
		this.requestType = requestType;
		this.query = query;
		this.results = results;
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

	public ArrayList<String> getResults() {
		return results;
	}

	public void setResults(ArrayList<String> results) {
		this.results = results;
	}

	public void setResults(String results){
		this.results.add(results);
	}

	public String toString(){
		String rt = requestType + " - ";
		for(String s: results){
			rt += s + "_";
		}
		return rt;
	}
}
