import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class DocParser {
	private String url;
	private String file; //document file
	private String title;
	private String text;
	private String allText;//title + text
	private List<String> tokenList = new ArrayList<String>();
	private List<String> links = new ArrayList<String>(); // record all links the page point out
	
	public DocParser(String url,String file){
		this.url = url;
		this.file = file;
		// use Jsoup to parse file
		Document doc = Jsoup.parse(file);
		/*
		 *  extract title and text from doc
		 *  add more if needed...
		 */
		this.title = doc.title();
		this.text = doc.body().text();
		// Concatenate the title and the text of the page
		this.allText = title +" "+ text;
		// Lowercase all words
		this.allText = allText.toLowerCase().trim();
		parseTokens(allText);
	}
	
	private void parseTokens(String text){
		String[] rawTokens = text.split("\\s+");
		for(String token : rawTokens){
			// if token is link, skip it
			if(token.startsWith("http://")||token.startsWith("https://")){
				links.add(token);
			}else{
			// Check if token contains non character
				String regex = "[^A-Za-z0-9]+"; //exclude character
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(token);
				if(matcher.find()){//token contains special character
					System.out.println("token contains special character");
					String[] subToken = token.split("[^a-z0-9]+");
					System.out.println(subToken.length);
					for(String t : subToken){
						System.out.println(t);
						tokenList.add(processToken(t));
					}
				}else{
					tokenList.add(processToken(token));
				}		
			}
		}
	}
	
	private String processToken(String token){
		//filter out all tokens in stoplist 
		
		return token;
	}
	
	public String getUrl(){
		return url;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getText(){
		return text;
	}
	
	public List<String> getTokens(){
		return tokenList;
	}
	
	public void main(String[] args){
		String str1 = "good+bad&&like hi";
		parseTokens(str1);
		
	}
	
}
