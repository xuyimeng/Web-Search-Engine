package edu.upenn.cis455.xpathengine;

import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author kejin fan
 * MyHandler extends DefaultHandler and has three methods startElement, EndElement, characters
 * 
 */
public class MyHandler extends DefaultHandler{
	public ArrayList<Node> checkList = new ArrayList<>();
	public QueryPool pool;
	public int level = 0;
	
	public MyHandler(){
		
	}
	
	public MyHandler(QueryPool pool){
		this.pool = pool;
	}
	
	// checking for validity
	
	public void startElement(String uriString, String local, String queryName, Attributes attrs){
		HashMap<String, ArrayList<Node>> candidates = this.pool.candidates;
		
		if (candidates.containsKey(queryName)){
			ArrayList<Node> nodes = candidates.get(queryName);
			for (Node node : nodes){
				if (node.level == level){			// levels must match
					
					HashMap<String, String> attrMap = node.attrMap;
					boolean proceed = true;
					
					/*
					for (Map.Entry<String, String> entry: attrMap.entrySet()){
						if (!attrs.getValue(entry.getKey()).equals(entry.getValue())){
							proceed = false;
						}
					}
					
					*/
					
					for (String key : attrMap.keySet()){			// if key value pairs in attrMap don't match attrs, no need to proceed bc
						
						try {
							String value = attrMap.get(key);			// it won't be valid

							//System.out.println(key);
							//if (attrs.getValue(key) == null) System.out.println("attrs.getValue(key) is null");
							if (attrs.getValue(key).equals(value)){
								continue;
							} else {
								proceed = false;
							}
						} catch (Exception e) {
							//System.out.println("exception in my handler");
						}
					}
					
					
					if (proceed){
						checkList.add(node);
					}
				}
			}
		}
		level++;
	}
	
	public void endElement(String uriString, String local, String queryName){
		HashMap<String, ArrayList<Node>> candidates = this.pool.candidates;
		
		if (candidates.containsKey(queryName)){
			ArrayList<Node> nodes = candidates.get(queryName);
			for (Node node : nodes){
				if (node.level == level){
					pool.removeChild(node);
					if (!node.isValid){
						node.setInvalid();
					}
				}
			}
		}
		
		level--;
	}
	
	//for a string starting from index start and has the length, if it appears in nodes' contains list/text list
	// it passes the check and setValid method should be called upon parent node.
	// multiple setValid method call may set parent node to valid
	public void characters(char[] chars, int start, int length){
		String text = new String(chars, start, length);
		
		ArrayList<String> containsList;
		ArrayList<String> textList;
		boolean testFail = false;
		
		for (Node node : checkList){				// check nodes in checklist against text
			containsList = node.contains;
			textList = node.texts;
			
			for (String string : containsList){
				if (!text.contains(string)) {
					testFail = true;
				}
			}
			
			for (String string : textList){
				if (!text.equals(string)) {
					testFail = true;
				}
			}
			
			if (testFail){
				
			} else {
				if (node.children.size() == 0){				//
					node.isValid = true;
					if (node.ancestorNode != null){
						node.ancestorNode.setValid();			// call ancestor's setValid bc this node is valid now
					}
				} else {
					pool.setChildren(node);
				}
			}
		}
		
		checkList = new ArrayList<>();
	}
	
	
}






