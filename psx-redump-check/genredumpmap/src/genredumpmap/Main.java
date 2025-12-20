package genredumpmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import genredumpmap.Main;

public class Main {
	public static void main(String[] args) throws IOException { 
	byte[] bYaml = Main.class.getResourceAsStream("/resources/gamedb.yaml").readAllBytes();
	String sYaml = new String(bYaml, StandardCharsets.UTF_8);
	
	Pattern p = Pattern.compile("(S[A-Z][A-Z][A-Z]\\-\\d\\d\\d\\d\\d:.*?)metadata:", Pattern.DOTALL);
	Matcher m = p.matcher(sYaml);
	while (m.find()) {
		String sEntry = m.group(1);
		
		Pattern pId = Pattern.compile("(S[A-Z][A-Z][A-Z]\\-\\d\\d\\d\\d\\d):");
		Matcher mId = pId.matcher(sEntry);
		
		if(mId.find()) {
			String sId = mId.group(1);
			
			Pattern pName = Pattern.compile("saveName:\\s*?\\\"(.*?)\\\"");
			Matcher mName = pName.matcher(sEntry);
			
			if(mName.find()) {
				String sName = mName.group(1);
				
				System.out.println(sId+"|"+sName);
			}
		}
	}
	
	System.out.println("## DONE ##");
}
}
