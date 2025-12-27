package checkfilenames;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * checks that the content of a game folder (bin and cue files) is matching the
 * name of the parent folder
 * 
 * also does some other check -only one dot (.) in file name (which is used to
 * separate file name and extension), so X-Men vs Street Fighter and NOT X-Men
 * vs. Street Fighter
 * 
 * supports Single Disc,Single Track games (most of the later games) Single
 * Disc,Multi Track games (most of the early games) Multi Disc,Single Track
 * games (e.g. X-Files) Multi Disc,Multi Track games (e.g. Alone in the Dark)
 */
public class Main {

	private static boolean isFixModeOn = false;

	public static void main(String[] args) throws IOException {
		if (2 == args.length && args[1].equals("--fix")) {
			isFixModeOn = true;
		}

		File dir = new File(args[0]);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			checkFiles(directoryListing);
		} else {
			System.out.println("No directory given or directory non-existent, Usage: checkfilenames <dir>");
			System.exit(0);
		}

		System.out.println("## DONE ##");
	}

	private static void checkFiles(File[] directoryListing) throws IOException {
		for (File child : directoryListing) {
			if (child.isDirectory()) {
				checkFiles(child.listFiles());
			} else {
				// ignore sub-channel files
				if (child.getName().contains(".sbi")) {
					continue;
				}
				if (child.getName().contains(".bmp")) {
					if(!child.getName().equals("cover.bmp")) {
						//cover.bmp
						child.renameTo(new File(child.getParent() + "\\cover.bmp"));
						System.out.println("cover file renamed");
					}
					continue;
				}
				if (child.getName().contains(".zip")) {
					if(!child.getName().equals("pdx-patch.zip")) {
						//pdx-patch.zip
						child.renameTo(new File(child.getParent() + "\\pdx-patch.zip"));
						System.out.println("patch file renamed");
					}
					continue;
				}

				// check if filename contains only one .
				Pattern p = Pattern.compile(".*?\\..*?\\..*");
				Matcher m = p.matcher(child.getName());
				if (m.find()) {
					System.out.println("ERROR: Filename " + child.getName() + " appears to be wrong");
					System.exit(0);
				}

//				if (child.getName().contains("(Disc")) {
//					// deconstruct parent name
//					p = Pattern.compile("(.*?)(\\[.*?\\])\\s(\\(.*?\\))\\s(\\[S.*?-\\d\\d\\d\\d\\d\\])");
//					m = p.matcher(child.getParentFile().getName());
//
//					// Multitrack?
//					if (child.getName().contains("(Track")) {
//						if (m.find()) {
//							// Multi-Disc, Multi-Track
//							String s1 = m.group(1);
//							String s2 = m.group(2);
//							String s3 = m.group(3);
//							String s4 = m.group(4);
//
//							// check if filename fits parent name
//							Pattern p_multi = Pattern.compile(s1.replace("[", "\\[").replace("]", "\\]")
//									.replace("(", "\\(").replace(")", "\\)")
//									+ s2.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\s"
//									+ s3.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\s(\\(Track \\d\\d\\))\\s*?"
//									+ s4.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\.(cue|bin)");
//							Matcher m_multi = p_multi.matcher(child.getName());
//							if (m_multi.find()) {
//							} else {
//								Pattern p_track = Pattern.compile("\\(Track\\s(\\d{1,2})\\)");
//								Matcher m_track = p_track.matcher(child.getName());
//
//								if (m_track.find()) {
//									String sTrack = m_track.group(1);
//									sTrack = sTrack.substring(0, sTrack.length());
//									String sTrackLeadingZeros = "00".concat(sTrack);
//									sTrack = sTrackLeadingZeros.substring(sTrackLeadingZeros.length() - 2,
//											sTrackLeadingZeros.length());
//
//									Pattern p_ext = Pattern.compile("(\\.[a-zA-z][a-zA-z][a-zA-z])");
//									Matcher m_ext = p_ext.matcher(child.getName());
//
//									if (m_ext.find()) {
//										String sFilename = s1 + s2 + " " + s3 + " (Track " + sTrack + ") " + s4
//												+ m_ext.group(0);
//										System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
//												+ sFilename + "\"");
//										if (isFixModeOn) {
//											child.renameTo(new File(child.getParent() + "\\" + sFilename));
//											System.out.println("renamed");
//										}
//									}
//								}
//							}
//						}
//					} else {
//						// Multi-Disc, Single-Track
//						if (m.find()) {
//							String s1 = m.group(1);
//							String s2 = m.group(2);
//							String s3 = m.group(3);
//							String s4 = m.group(4);
//
//							// check if filename fits parent name
//							Pattern p_single = Pattern.compile(s1.replace("[", "\\[").replace("]", "\\]")
//									.replace("(", "\\(").replace(")", "\\)")
//									+ s2.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\s"
//									+ s3.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\s"
//									+ s4.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
//									+ "\\.(cue|bin)");
//							Matcher m_single = p_single.matcher(child.getName());
//							if (m_single.find()) {
//							} else {
//								Pattern p_ext = Pattern.compile("(\\.[a-zA-z][a-zA-z][a-zA-z])");
//								Matcher m_ext = p_ext.matcher(child.getName());
//
//								if (m_ext.find()) {
//									String sFilename = s1 + s2 + " " + s3 + " " + s4 + m_ext.group(0);
//									System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
//											+ sFilename + "\"");
//									if (isFixModeOn) {
//										child.renameTo(new File(child.getParent() + "\\" + sFilename));
//										System.out.println("renamed");
//									}
//								}
//							}
//						}
//					}
//				} else {
					// deconstruct parent name
					p = Pattern.compile("(.*?)\\[.*?\\]");
					m = p.matcher(child.getParentFile().getName());

					// Multitrack?
					if (child.getName().contains("(Track")) {
						if (m.find()) {
							// Single Disc, Multitrack
							String s1 = m.group(1).trim();
							
							String r_multi = s1.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
									+ "[^\\]\\[]*?(\\(Track.*?\\))[^\\]\\[]*?\\.(cue|bin)";
							// check if filename fits parent name
							Pattern p_multi = Pattern.compile(
									r_multi);
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
										String sFilename = s1 + " (Track " + sTrack + ")" + m_ext.group(0);
										System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
												+ sFilename + "\"");
										if (isFixModeOn) {
											child.renameTo(new File(child.getParent() + "\\" + sFilename));
											System.out.println("renamed");
										}
									}
								}
							}
						}
					} else {
						if (m.find()) {
							// Single Track, Single Disc
							String s1 = m.group(1).trim();

							// check if filename fits parent name
							Pattern p_single = Pattern.compile(
									s1.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
									+ "[^\\]\\[]*?\\.(cue|bin)");
							Matcher m_single = p_single.matcher(child.getName());
							if (m_single.find()) {
							} else {
								Pattern p_ext = Pattern.compile("(\\.[a-zA-z][a-zA-z][a-zA-z])");
								Matcher m_ext = p_ext.matcher(child.getName());

								if (m_ext.find()) {
									String sFilename = s1 + m_ext.group(0);
									System.out.println("ERROR: File \"" + child.getName() + "\" should be named \""
											+ sFilename + "\"");
									if (isFixModeOn) {
										child.renameTo(new File(child.getParent() + "\\" + sFilename));
										System.out.println("renamed");
									}
								}
							}
						}
					}
//				}
			}
		}
	}
}
