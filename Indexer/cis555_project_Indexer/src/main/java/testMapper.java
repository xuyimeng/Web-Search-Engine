

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class testMapper {
	static BufferedReader reader;
	public static void map(String url, String doc){
		//Extract page content from xml/html file with Jsoup
		
	}
	public static void parse(String file){
		Document doc = Jsoup.parse(file);
		// extract title and text from doc
		String title = doc.title();
		String text = doc.body().text();
		// Concatenate the title and the text of the page
		String allText = title +" "+ text;
		// Lowercase all words
		allText = allText.toLowerCase().trim();
		// parse all tokens
		String[] tokens = allText.split("\\s+");
		
		
	}
	
	public static void main(){
		File file = new File("/Users/yimengxu/Documents/workspace/HW3/store/webPage/nyTime.xml");
		String url = "http://crawltest.cis.upenn.edu/nytimes/Africa.xml";
		try {
			reader = new BufferedReader(new FileReader(file));
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
