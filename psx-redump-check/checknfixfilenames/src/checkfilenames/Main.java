package checkfilenames;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	public static void main(String[] args) throws IOException {
		File dir = new File(args[0]);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			checkFiles(directoryListing);
		} else {
			System.out.println("No directory given or directory non-existent, Usage: checksum <dir>");
			System.exit(0);
		}
		
		System.out.println("## DONE ##");
	}

	private static void checkFiles(File[] directoryListing) throws IOException {
		for (File child : directoryListing) {
			if (child.isDirectory()) {
				checkFiles(child.listFiles());
			} else {
				if (child.getName().contains(".sbi")) {
					continue;
				}

				// check if filename contains only one .
				Pattern p = Pattern.compile(".*?\\..*?\\..*");
				Matcher m = p.matcher(child.getName());
				if (m.find()) {
					System.out.println("ERROR: Filename " + child.getName() + " appears to be wrong");
					System.exit(0);
				}

				// Todo multi disc and multi disc and multitrack

				// deconstruct parent name
				p = Pattern.compile("(.*?)(\\[.*?\\])\\s*?(\\[S.*?-\\d\\d\\d\\d\\d\\])");
				m = p.matcher(child.getParentFile().getName());

				// Multitrack?
				if (child.getName().contains("(Track")) {
					if (m.find()) {
						String s1 = m.group(1);
						String s2 = m.group(2);
						String s3 = m.group(3);

						Pattern p_multi = Pattern.compile(
								s1.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
								+ s2.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
								+ "\\s*?(\\(Track \\d\\d\\))\\s*?"
								+ s3.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
								+ "\\.(cue|bin)");
						Matcher m_multi = p_multi.matcher(child.getName());
						if (m_multi.find()) {
						} else {
							Pattern p_track = Pattern.compile("\\(Track\\s(\\d{1,2})\\)");
							Matcher m_track = p_track.matcher(child.getName());

							if (m_track.find()) {
								String sTrack = m_track.group(1);
								sTrack = sTrack.substring(0, sTrack.length());
								String sTrackLeadingZeros = "00".concat(sTrack);
								sTrack = sTrackLeadingZeros.substring(sTrackLeadingZeros.length() - 2,
										sTrackLeadingZeros.length());

								Pattern p_ext = Pattern.compile("(\\.[a-zA-z][a-zA-z][a-zA-z])");
								Matcher m_ext = p_ext.matcher(child.getName());

								if (m_ext.find()) {
									String sFilename = s1 + s2 + " (Track " + sTrack + ") " + s3 + m_ext.group(0);
									System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
											+ sFilename + "\"");
									child.renameTo(new File(child.getParent()+"\\"+sFilename));
									System.out.println("renamed");
								}
							}
						}
					}
				} else {
					// check if filename fits parent name
					if (m.find()) {
						String s1 = m.group(1);
						String s2 = m.group(2);
						String s3 = m.group(3);

						Pattern p_single = Pattern.compile(
								s1.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)") + s2.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
										+ "\\s*?" + s3.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)") + "\\.(cue|bin)");
						Matcher m_single = p_single.matcher(child.getName());
						if (m_single.find()) {
						} else {
							Pattern p_ext = Pattern.compile("(\\.[a-zA-z][a-zA-z][a-zA-z])");
							Matcher m_ext = p_ext.matcher(child.getName());

							if (m_ext.find()) {
								String sFilename = s1 + s2 + " " + s3 + m_ext.group(0);
								System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
										+ sFilename + "\"");
								child.renameTo(new File(child.getParent()+"\\"+sFilename));
								System.out.println("renamed");
							}
						}
					}
				}
			}
		}
	}
}
