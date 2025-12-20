package cuecheck;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Main {

	private static Map<String, List<String>> mCueContent = new HashMap<String, List<String>>();
	private static Map<String, List<String>> mFileContent = new HashMap<String, List<String>>();

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException,
			ParserConfigurationException, SAXException {
		System.out.println("== Traversing Directory ==");
		File dir = new File(args[0]);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			getCueData(directoryListing);

			if (mCueContent.size() > 0 && mFileContent.size() > 0) {
				// cue => files
				System.out.println("Checking cue content against files");
				Map<String, List<String>> mCueCopy = new HashMap<String, List<String>>(mCueContent);
				Map<String, List<String>> mFileCopy = new HashMap<String, List<String>>(mFileContent);

				for (String key : mCueCopy.keySet()) {
					System.out.println("==" + key);
					List<String> s1 = mCueCopy.get(key);

					if (!(!s1.get(0).contains("(Track") || s1.get(0).contains("(Track 01)"))) {
						System.out.println("ERROR/S: Order in " + key + " seems wrong");
					}

					if (mFileCopy.containsKey(key)) {
						List<String> s2 = mFileCopy.get(key);
						for (String key2 : s1) {
							if (!key2.contains(".bin")) {
								System.out.println("ERROR/X: Extension of " + key2 + " seems wrong");
							} else {
								if (!s2.contains(key2)) {
									System.out.println("ERROR/F: File " + key2 + " could not be found");
								}
							}
						}
					} else {
						System.out.println("ERROR/O: Folder " + key + " and its content could not be found");
					}
				}

				// files => cue
				System.out.println("Checking files against cue content");
				mCueCopy = new HashMap<String, List<String>>(mCueContent);
				mFileCopy = new HashMap<String, List<String>>(mFileContent);

				for (String key : mFileCopy.keySet()) {
					System.out.println("==" + key);
					List<String> s1 = mFileCopy.get(key);
					if (mCueCopy.containsKey(key)) {
						List<String> s2 = mCueCopy.get(key);
						for (String key2 : s1) {
							if (!s2.contains(key2)) {
								System.out.println("ERROR/C: Cue file is missing " + key2);
							}
						}
					} else {
						System.out.println("ERROR/M: Cue file " + key + " could not be found or might be invalid");
					}
				}
			} else {
				System.out.println("ABORT: Cue-Files could not be parsed or no Bin files found");
			}
		} else {
			System.out.println("No directory given or directory non-existent, Usage: checksum <dir>");
			System.exit(0);
		}

		System.out.println("## DONE ##");
	}

	private static void getCueData(File[] directoryListing) throws IOException {
		for (File child : directoryListing) {
			if (child.isDirectory()) {
				getCueData(child.listFiles());
			} else {
				if (child.getName().contains(".cue")) {
					byte[] data = Files.readAllBytes(Paths.get(child.toURI()));
					String sCueContent = new String(data, StandardCharsets.UTF_8);

					Pattern p = Pattern.compile("FILE.*?\"(.*?)\".*?BINARY");
					Matcher m = p.matcher(sCueContent);
					while (m.find()) {
						String sMatch = m.group(1);
						List<String> setCueContent;
						if (mCueContent.containsKey(child.getName())) {
							setCueContent = mCueContent.get(child.getName());
						} else {
							setCueContent = new ArrayList<String>();
						}

						setCueContent.add(sMatch);
						mCueContent.put(child.getName(), setCueContent);
					}

				} else if (child.getName().contains(".bin")) {
					List<String> setFileContent;
					if (mFileContent.containsKey(child.getParentFile().getName() + ".cue")) {
						setFileContent = mFileContent.get(child.getParentFile().getName() + ".cue");
					} else {
						setFileContent = new ArrayList<String>();
					}

					setFileContent.add(child.getName());
					mFileContent.put(child.getParentFile().getName() + ".cue", setFileContent);
				}
			}
		}
	}
}
