import PointAdapter.Coordinates;
import PointAdapter.PolylineAdapter;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{

	private static final long serialVersionUID = 2314160120198237281L;
	private int requestType;
	private Coordinates query;
	private ArrayList<PolylineAdapter> results = new ArrayList<>();

	public Message(int requestType, Coordinates query, ArrayList<PolylineAdapter> results){
		this.requestType = requestType;
		this.query = query;
		this.results = results;
	}

	public Message(int requestType, Coordinates query){
		this.requestType = requestType;
		this.query = query;
	}

	public Message(){
		this.requestType = 0;
		this.query = null;
	}

	public Coordinates getQuery() {
		return query;
	}

	public void setQuery(Coordinates query) {
		this.query = query;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		this.requestType = requestType;
	}

	public ArrayList<PolylineAdapter> getResults() {
		return results;
	}

	public void setResults(ArrayList<PolylineAdapter> results) {
		this.results = results;
	}

	public void setResults(PolylineAdapter results){
		this.results.add(results);
	}

	public String toString(){
		String rt = requestType + " - ";
		for(PolylineAdapter s: results){
			rt += s + "_";
		}
		return rt;
	}
}
