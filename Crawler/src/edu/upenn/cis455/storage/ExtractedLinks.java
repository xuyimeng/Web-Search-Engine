package edu.upenn.cis455.storage;

import java.util.ArrayList;


import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class ExtractedLinks {
	@PrimaryKey
	String URL;
	ArrayList<String> links = new ArrayList<>();
	
	public ExtractedLinks(){
		
	}
	
	public ExtractedLinks(String url){
		this.URL = url;
	}
	
	public String getURL(){
		return URL;
	}
	
	public void setURL(String url){
		this.URL = url;
	}
	
	public ArrayList<String> getLinks(){
		return links;
	}
	
	public void setLinks(ArrayList<String> links){
		this.links = links;
	}
	
	public void addLink(String link){
		this.links.add(link);
	}
}
