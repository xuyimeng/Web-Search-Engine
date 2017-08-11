package edu.upenn.cis455.xpathengine;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author kejin fan
 * QueryPool is used to contain all xpaths strings and maintain an array of queries and a map representing candidates node
 */
public class QueryPool {
	public String[] xpaths;
	public Query[] queries;
	public HashMap<String, ArrayList<Node>> candidates;
	
	public QueryPool(String[] s) throws NullPointerException {
		this.xpaths = s;
		this.queries = new Query[xpaths.length];
		this.candidates = new HashMap<>();
		
		for (int i = 0; i < queries.length; i++){
			try {
				this.queries[i] = new Query(0, i, xpaths[i]);
			} catch (Exception e) {
				
			}
		}
		
		setCandidates();
		
	}
	
	/**
	 * if candidates don't have startElem of a query, just put it in
	 */
	public void setCandidates(){

		for (int i = 0; i < queries.length; i++){
			try {
				Node headNode = queries[i].startElem;
				if (!candidates.containsKey(headNode.pathName)){
					candidates.put(headNode.pathName, new ArrayList<Node>());
				}
				candidates.get(headNode.pathName).add(headNode);
			} catch (Exception e) {
				
			}
		}
	}
	
	/**
	 * @param node
	 * if children don't have the children node of a node, just put it in
	 */
	public void setChildren(Node node){
		for (Node childNode : node.children){
			if (!candidates.containsKey(childNode.pathName)){
				candidates.put(childNode.pathName, new ArrayList<Node>());
			}
			candidates.get(childNode.pathName).add(childNode);
		}
	}
	
	/**
	 * @param node
	 * remove all children node of a node from candidates map.
	 */
	public void removeChild(Node node){
		for (Node childNode : node.children){
			if (candidates.containsKey(childNode.pathName)){
				candidates.get(childNode.pathName).remove(childNode);
			}
		}
	}
	
	
	public static void main(String[] args){
		String tmp1 = "a/b/edf/d[@att=\"123\"]";
		String tmp2 = "a/b/edf/d[text()=\"hhh\"]";
		String tmp3 = "/foo/bar/xyz";
		String tmp4 = "/xyz/abc[contains(text(),\"someSubstring\")] ";
		String tmp5 = "/a/b/c[text()=\"theEntireText\"] ";
		String tmp6 = "/blah[anotherElement]";
		String tmp7 = "/this/that[something/else]";
		String tmp8 = "/d/e/f[foo[text()=\"something\"]][bar]";
		String tmp9 = "/a/b/c[text()  =   \"whiteSpacesShouldNotMatter\"]";
		
		
		Query q1 = new Query(0, 1, tmp1);
		Query q2 = new Query(0, 2, tmp2);
		Query q3 = new Query(0, 3, tmp3);
		Query q4 = new Query(0, 4, tmp4);
		Query q5 = new Query(0, 5, tmp5);
		Query q6 = new Query(0, 6, tmp6);
		Query q7 = new Query(0, 7, tmp7);
		Query q8 = new Query(0, 8, tmp8);
		Query q9 = new Query(0, 9, tmp9);
		
		System.out.println(q1.queryString);
		System.out.println(q2.queryString);
		System.out.println(q3.queryString);
		System.out.println(q4.queryString);
		System.out.println(q5.queryString);
		System.out.println(q6.queryString);
		System.out.println(q7.queryString);
		System.out.println(q8.queryString);
		System.out.println(q9.queryString);
		
		/*
		for (String string : q1.startElem.contains){
			System.out.println(string);
		}
		
		for (String string : q2.startElem.texts){
			System.out.println(string);
		}
		*/
	}
	
}
