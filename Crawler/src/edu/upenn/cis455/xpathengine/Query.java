package edu.upenn.cis455.xpathengine;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author kejin fan
 * Query class is for parsing a single xpath query and populating relevant variables
 */
public class Query {
	public String queryString;
	public Node startElem;
	public Node endElem;
	public int queryID;
	public ArrayList<String> nodeNames;			// has all sections of info (usually seperated by "/")
	public int unmatchedLeftBracket = 0;
	
	public Query(int level, int id, String queryString){
		this.queryID = id;
		this.queryString = queryString;
		this.nodeNames = new ArrayList<>();
		
		int start = 0;
		if (queryString.charAt(0) == '/'){
			start = 1;
		}
		
		StringBuffer sb = new StringBuffer();
		
		for (int i = start; i < queryString.length(); i++){				// populate nodenames arraylist by slicing queryString
			char c = queryString.charAt(i);
			if (unmatchedLeftBracket == 0 && c == '/'){
				nodeNames.add(sb.toString());
				sb = new StringBuffer();
				continue;
			} else if (c == '['){
				unmatchedLeftBracket++;
			} else if (c == ']'){
				unmatchedLeftBracket--;
			} 
			sb.append(c);
		}
		nodeNames.add(sb.toString());
		
		for (String names : nodeNames){
			if (!"".equals(names)){
				Node node = new Node(names, queryID, level++);			// for every nodeName, create a node and contain its info
				//System.out.println("path name is " + node.pathName);
				//System.out.println(names);
				/*
				for (String string : node.contains){
					System.out.println("contains " + string);
				}
				
				for (String string : node.texts){
					System.out.println("texts " + string);
				}
				
				for (String string : node.tests){
					System.out.println("tests " + string);
				}
				*/
				
				if (startElem == null){
					startElem = node;
					endElem = node;
				} else {
					endElem.children.add(node);
					endElem = node;
				}
			}
		}
		
		Queue<Node> q = new LinkedList<Node>();
		q.offer(startElem);
		

		while (!q.isEmpty()){				/// set the ancestor nodes for this chain of nodes
			Node node = q.poll();
			if (node == null) continue;
			for (Node childNode : node.children){
				q.add(childNode);
				childNode.ancestorNode = node;
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
