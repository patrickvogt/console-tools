package genredumpmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import genredumpmap.Main;

/**
 * extract Game ID and Redump Name (not always accurate) from duckstation gamedb.yaml file
 * might not extract all Game IDs due to the nature how several Game IDs are bundles/cross-references in the yaml file
 */
public class Main {
	
	public static void main(String[] args) throws IOException {
		// read yaml file (version 2025-12). probably updated when you read this 
		// url: https://github.com/stenzek/duckstation/blob/master/data/resources/gamedb.yaml
		byte[] bYaml = Main.class.getResourceAsStream("/resources/gamedb.yaml").readAllBytes();
		String sYaml = new String(bYaml, StandardCharsets.UTF_8);

		// split by entry
		Pattern p = Pattern.compile("(S[A-Z][A-Z][A-Z]\\-\\d\\d\\d\\d\\d:.*?)metadata:", Pattern.DOTALL);
		Matcher m = p.matcher(sYaml);
		while (m.find()) {
			String sEntry = m.group(1);

			// extract Game ID (also only works for Games/Software/Demos - Net Yaroze Discs like DTL* are ignored
			Pattern pId = Pattern.compile("(S[A-Z][A-Z][A-Z]\\-\\d\\d\\d\\d\\d):");
			Matcher mId = pId.matcher(sEntry);

			if (mId.find()) {
				String sId = mId.group(1);

				// extract redump name
				// does not always fit, some PAL games are listed as (Europe, Australia) in the Redump DAT file
				// whereas they are just named (Europe) in the duckstation yaml file. Wipeout games might also be
				// problematic due to different capitalization YAML(WipEout 2097 (Europe)) => REDUMP(Wipeout 2097 (Europe))
				// note: Game IDs in general might not be unique since games have several revisions (marked as (Rev 1/2/3/...) in the redump dat file
				Pattern pName = Pattern.compile("saveName:\\s*?\\\"(.*?)\\\"");
				Matcher mName = pName.matcher(sEntry);

				if (mName.find()) {
					String sName = mName.group(1);

					System.out.println(sId + "|" + sName);
				}
			}
		}

		System.out.println("## DONE ##");
	}
}
