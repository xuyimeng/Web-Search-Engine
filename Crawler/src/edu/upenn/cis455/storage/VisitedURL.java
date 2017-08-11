package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class VisitedURL {
	
	@PrimaryKey
	String URL;
	
	long lastVisited;
	
	public VisitedURL(){
		
	}
	
	public VisitedURL(String url, long lastVisited){
		this.URL = url;
		this.lastVisited = lastVisited;
	}
	
	public String getURL(){
		return URL;
	}
	
	public long getLastVisited(){
		return lastVisited;
	}
	
	public void setURL(String url){
		this.URL = url;
	}
	
	public void setLastVisited(long l){
		this.lastVisited = l;
	}
}
