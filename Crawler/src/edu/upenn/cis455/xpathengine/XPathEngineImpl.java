package edu.upenn.cis455.xpathengine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;



/**
 * @author kejin fan
 * XPathEngineImpl class implements XPathEngine interface and it uses SAX to parse XPATH queries
 * and return boolean to indicate if it matches a file.
 */
public class XPathEngineImpl implements XPathEngine {

	private String[] xpaths;
	private QueryPool pool;
	private SAXParser parser;
	private Query[] queries;
	
	final String regexText = "\\s*text\\s*\\(\\s*\\)\\s*=\\s*\"(.*?)\"\\s*";
	final String regexAttr = "\\s*@\\s*([a-zA-Z_:]+[a-zA-Z0-9_\\-\\/]*)\\s*=\\s*\"(.*?)\"\\s*";
	final String regexContain = "\\s*contains\\s*\\(\\s*text\\s*\\(\\s*\\)\\s*\\,\\s*\"(.*?)\"\\s*\\)\\s*";
	private SAXParserFactory factory = SAXParserFactory.newInstance();
	
    public XPathEngineImpl() {
    
    }
	

	@Override
	public void setXPaths(String[] expressions) {
		this.xpaths = expressions;
		this.pool = new QueryPool(xpaths);
		this.queries = this.pool.queries;
	}


	@Override
	public boolean isValid(int i) {
		if (xpaths[i] == null || xpaths[i].equals("")){
			return false;
		}
		
		try {
			if (i < 0 && i > queries.length - 1){
				return false;
			} else {
				return queries[i].startElem.isValid;			// due to my hierarchical design, as long as the startElement is valid 
																// the whole path is valid
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isSAX() {
		return true;
	}

	@Override
	public boolean[] evaluate(Document d) {
		return null;
	}


	@Override
	public boolean[] evaluateSAX(InputStream document, DefaultHandler handler) {
		boolean[] res;
		MyHandler myHandler = (MyHandler)handler;
		myHandler.pool = pool;
		
		try {
			parser = factory.newSAXParser();
			parser.parse(document, myHandler);
		} catch (Exception e) {
			//e.printStackTrace();
			//System.out.println("evaluate SAX EXCEPTION");
		}
		
		res = new boolean[pool.queries.length];
		
		for (int i = 0; i < res.length; i++){
			res[i] = isValid(i);
		}
		return res;
	}

	public static void main(String[] args) throws FileNotFoundException{
		InputStream inputStream = new FileInputStream("./Science.xml");
	    XPathEngineImpl xPathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
	    DefaultHandler handler = XPathEngineFactory.getSAXHandler();
	    
	    String string0 = "/rss/channel/image[link[text()= \"http://www.nytimes.com/pages/science/index.html\"]]";
	    String string1 = "/ rss/  channel  /image/link[   contains(text(),  \"nytimes\")]"; 
	    String string2 = "/rss/channel/image/url";
	    String string3 = "/rss/ channel / item[author[text()  =\"KENNETH CHANG\"]]";				
	    String string4 = "/rss/channel / title";
	    String string5 = "/rss/channel/item[@title=\"Icy Ball Is Larger Than Pluto. So, Is It a Planet?\"]";			// >????? fuck
	    String string6 = "/rss/channel[image/url]";
	    String string7 = "/rss/channel/image[link[text()= \"http://www.nytimes.com/pages/science/index.html\"]][url]";
	    String string8 = "/rss";
	    String string9 = "/rss/channel[description]";
	    String string10 = "/";
	    String string11 = "";
	    
	    String[] expressions = new String[]{string0, string1, string2, string3,
	    		 string4, string5, string6, string7, string8, string9, string10, string11};
	    xPathEngine.setXPaths(expressions);
	    boolean[] res = xPathEngine.evaluateSAX(inputStream, handler);
	    
	    System.out.println(Arrays.toString(res));
	}
	
        
}
