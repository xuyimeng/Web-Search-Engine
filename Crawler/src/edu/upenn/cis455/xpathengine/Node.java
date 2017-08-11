package edu.upenn.cis455.xpathengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author kejin fan
 * Node class is used to store information about a step/node in a xpath
 */
public class Node {
	public String pathString;
	public String pathName;
	public int queryID;
	public int level;
	public ArrayList<Node> children;
	public ArrayList<String> tests;
	public ArrayList<String> contains;
	public ArrayList<String> texts;
	public HashMap<String, String> attrMap;
	final String CONTAINS = "^contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*,\\s*\"(.*)\"\\s*\\)";			// regex 
	final String TEXT = "^text\\s*\\(\\)\\s*=\\s*\"(.*)\"";
	final String ATTR = "^@([^=\\s]+)\\s*=\\s*\"(.*)\"";
	final String PATHNAME = "([^\\[]+)(\\[.+?\\])*";
	Pattern p;
	Matcher m;
	
	public boolean isValid = false;
	public int validChildren = 0;
	public Node ancestorNode;
	
	/**
	 * @param path
	 * @param id
	 * @param level
	 * constructor will parse the path string and populate children list
	 */
	public Node(String path, int id, int level){

		this.pathString = path;
		this.queryID = id;
		this.level = level;
		
		children = new ArrayList<>();
		tests = new ArrayList<>();
		contains = new ArrayList<>();
		texts = new ArrayList<>();
		attrMap = new HashMap<>();
		
		p = Pattern.compile(PATHNAME);			// regex to check if it is a pattern
		m = p.matcher(path.trim());
		
		while (m.find()){
			this.pathName = m.group(1);
			break;
		}
		
		int start = 0;
		int unmatchedLeftBracket = 0;			// parse the string for pairs of brackets
		for (int i = 0; i < path.length(); i++){
			char c = path.charAt(i);
			if (c == '['){
				unmatchedLeftBracket++;
				if (unmatchedLeftBracket == 1){
					start = i;
				}
			} else if (c == ']'){
				if (unmatchedLeftBracket == 1){
					tests.add(path.substring(start+1, i));
					unmatchedLeftBracket = 0;
				} else {
					unmatchedLeftBracket--;
				}
			}
		}
		
		if (tests.size() > 0){
			//System.out.println("start printing tests");
			/*
			for (int i = 0; i < tests.size(); i++){
				System.out.println(tests.get(i));
			}
			*/
			
			for (int i = 0; i < tests.size(); i++){
				if (isInThreeFormats(tests.get(i))){			// checking for special checks
					continue;
				} else {
					Query q = new Query(level + 1, id, tests.get(i));
					children.add(q.startElem);
				}
			}
			
		}
		
		
	}
	
	/**
	 * set this node to invalid
	 */
	public void setInvalid(){			// if this node is invalid, all its children are invalid
		isValid = false;
		validChildren = 0;
		for (Node node : children){
			node.setInvalid();
		}
	}
	
	/**
	 * increase validChildren
	 */
	public void setValid(){					// if a child reports valid, validChildren increases. 
		if (!isValid){
			validChildren++;
			if (validChildren == children.size()){
				isValid = true;
				if (ancestorNode != null){			// check validity of ancestor
					ancestorNode.setValid();
				}
			}
		}
	}
	
	public boolean isInThreeFormats(String step){
		return isContains(step) || isAttr(step) || isText(step);
	}
	
	/**
	 * @param step
	 * @return a boolean, if step is a contains check
	 */
	public boolean isContains(String step){
		p = Pattern.compile(CONTAINS);
		m = p.matcher(step.trim());
		while (m.find()){
			//System.out.println("print out contains: ");
			//System.out.println(m.group(1));
			contains.add(m.group(1));
			return true;
		}
		return false;
	}
	
	
	/**
	 * @param step
	 * @return a boolean, if step is a text check
	 */
	public boolean isText(String step){
		p = Pattern.compile(TEXT);
		m = p.matcher(step.trim());
		while (m.find()){
			//System.out.println("print out text: ");
			//System.out.println(m.group(1));
			texts.add(m.group(1));
			return true;
		}
		return false;
	}
	
	/**
	 * @param step
	 * @return a boolean, if step is a attr check
	 */
	public boolean isAttr(String step){
		p = Pattern.compile(ATTR);
		m = p.matcher(step.trim());
		while (m.find()){
			//System.out.println("print out attribute: ");
			//System.out.println(m.group(1));
			//System.out.println(m.group(2));
			attrMap.put(m.group(1), m.group(2));
			//System.out.println(m.group(1));
			//System.out.println(m.group(2));
			return true;
		}
		return false;
	}
	
}
