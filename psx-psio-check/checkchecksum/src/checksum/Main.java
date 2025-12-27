package checksum;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
	
	private static final String CSV_SEP = "\\|";

	private static Map<String, String> mCRC32 = new HashMap<String, String>();
	private static Map<String, String> mMD5 = new HashMap<String, String>();
	private static Map<String, String> mSHA1 = new HashMap<String, String>();
	private static Map<String, String> mNameMapping = new HashMap<String, String>();
	private static Map<String, String> mCueFilesCRC32 = new HashMap<String, String>();
	private static Map<String, String> mCueFilesMD5 = new HashMap<String, String>();
	private static Map<String, String> mCueFilesSHA1 = new HashMap<String, String>();
	private static Map<String, String> mRefCRC32 = new HashMap<String, String>();
	private static Map<String, String> mRefMD5 = new HashMap<String, String>();
	private static Map<String, String> mRefSHA1 = new HashMap<String, String>();
	private static Document mDoc;

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException,
			ParserConfigurationException, SAXException {
		System.out.println("== Reading Mapping File ==");
		byte[] bNameMapping = Main.class.getResourceAsStream("/resources/mapping.csv").readAllBytes();
		String sNameMapping = new String(bNameMapping, StandardCharsets.UTF_8);
		String[] sLines = sNameMapping.split("\n");
		for (String line : sLines) {
			if (line.startsWith("ID") || line.trim().equals("")) {
				continue;
			}
			line = line.replace("\r", "");
			String[] saMap = line.split(CSV_SEP);
			mNameMapping.put(saMap[0], saMap[1]);
		}

		System.out.println("== Reading Overwritten Cuefiles File ==");
		byte[] bCueFiles = Main.class.getResourceAsStream("/resources/cuefiles.csv").readAllBytes();
		String sCueFiles = new String(bCueFiles, StandardCharsets.UTF_8);
		sLines = sCueFiles.split("\n");
		for (String line : sLines) {
			if (line.startsWith("ID") || line.trim().equals("")) {
				continue;
			}
			line = line.replace("\r", "");
			String[] saMap = line.split(CSV_SEP);
			String sHex = "00000000".concat(saMap[1]);
			mCueFilesCRC32.put(saMap[0], sHex.substring(sHex.length() - 8, sHex.length()));
			mCueFilesMD5.put(saMap[0], saMap[2]);
			mCueFilesSHA1.put(saMap[0], saMap[3]);
		}

		System.out.println("== Reading DAT file ==");
		parseDATFile();

		System.out.println("== Traversing Directory ==");
		File dir = new File(args[0]);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			getChecksums(directoryListing);

			System.out.println("== Check ==");
			for (String key : mCRC32.keySet()) {
				if (key.contains(".sbi")) {
					continue;
				}

				boolean isCRC32 = false;
				String sRefCRC32 = "";
				// Extract ID from filename
				Pattern p = Pattern.compile("\\[(S?[A-Z][A-Z][A-Z]-[\\d|S]\\d\\d\\d\\d)\\]");
				Matcher m = p.matcher(key);
				if (m.find()) {
					String sMapped = mNameMapping.get(m.group(1));

					if (sMapped != null) {
						if(sMapped.equals("skip")) {
							System.out.println(key + " skipped");
							continue;
						}
						
						System.out.println(key + " => " + sMapped);
						sMapped = key.replaceAll(".*?\\.", sMapped + ".");

						if (mRefCRC32.containsKey(sMapped)) {
							if (sMapped.contains(".cue")) {
								if (mCueFilesCRC32.containsKey(m.group(1))) {
									sRefCRC32 = mCueFilesCRC32.get(m.group(1));
								} else {
									System.out.println("Add line to cuefiles.csv");
									System.out.println(m.group(1) + CSV_SEP.replace("\\", "") + mCRC32.get(key) + CSV_SEP.replace("\\", "") + mMD5.get(key) + CSV_SEP.replace("\\", "")
											+ mSHA1.get(key));
								}
							} else {
								sRefCRC32 = mRefCRC32.get(sMapped);
							}

							isCRC32 = mCRC32.get(key).equals(sRefCRC32);
						} else {
							// rom missing in DAT file or Multi Bin/Cue

							// Multitrack
							if (key.contains("(Track ")) {
								// Extract Track no. from key
								System.out.println(key + " => Multi-Track check");

								sMapped = mNameMapping.get(m.group(1));
								p = Pattern.compile("(\\(Track.*?\\))");
								m = p.matcher(key);

								if (m.find()) {
									String sTrack = m.group(1);
									String sRomId = sMapped + " " + sTrack + ".bin";

									if (mRefCRC32.containsKey(sRomId)) {
										sRefCRC32 = mRefCRC32.get(sRomId);
										isCRC32 = mCRC32.get(key).equals(sRefCRC32);
									} else {
										// Check for single track no. (1 instead of 01)
										sRomId = sRomId.replaceAll("Track 01", "Track 1");
										sRomId = sRomId.replaceAll("Track 02", "Track 2");
										sRomId = sRomId.replaceAll("Track 03", "Track 3");
										sRomId = sRomId.replaceAll("Track 04", "Track 4");
										sRomId = sRomId.replaceAll("Track 05", "Track 5");
										sRomId = sRomId.replaceAll("Track 06", "Track 6");
										sRomId = sRomId.replaceAll("Track 07", "Track 7");
										sRomId = sRomId.replaceAll("Track 08", "Track 8");
										sRomId = sRomId.replaceAll("Track 09", "Track 9");

										if (mRefCRC32.containsKey(sRomId)) {
											sRefCRC32 = mRefCRC32.get(sRomId);
											isCRC32 = mCRC32.get(key).equals(sRefCRC32);
										} else {
											System.out.println("ABORT: rom " + sRomId + " not in DAT file");
											System.exit(0);
										}
									}
								}
							} else {
								System.out.println("ABORT: rom " + sMapped
										+ " not in DAT file (Multi-Bin but download is Single-Bin?)");
								System.exit(0);
							}
						}
					} else {
						System.out.println("ABORT: Mapping for " + m.group() + " does not exist in mapping file");
						System.exit(0);
					}
				} else {
					System.out.println("ABORT: File " + key + " does not contain Playstation-ID");
					System.exit(0);
				}

				if (isCRC32) {
					System.out.println(key + ": valid");
				} else {
					System.out.println(
							key + ": not valid (dat_crc = " + sRefCRC32 + ", act_crc = " + mCRC32.get(key) + ")");
					System.exit(0);
				}
			}
		} else {
			System.out.println("No directory given or directory non-existent, Usage: checksum <dir>");
		}

		System.out.println("## DONE ##");
	}

	private static void getChecksums(File[] directoryListing) throws IOException, NoSuchAlgorithmException {
		for (File child : directoryListing) {
			if (!child.isDirectory()) {
				byte[] data = Files.readAllBytes(Paths.get(child.toURI()));

				String sCRC32 = getCRC32(data);
				mCRC32.put(child.getName(), sCRC32);
				String sMD5 = getMD5(data);
				mMD5.put(child.getName(), sMD5);
				String sSHA1 = getSHA1(data);
				mSHA1.put(child.getName(), sSHA1);

				data = null;
			} else {
				getChecksums(child.listFiles());
			}
		}
	}

	private static String getCRC32(byte[] b) throws IOException {
		CRC32 crc = new CRC32();

		ByteBuffer bb = ByteBuffer.wrap(b);
		crc.update(bb);

		String sHex = "00000000".concat(Long.toHexString(crc.getValue()));
		return sHex.substring(sHex.length() - 8, sHex.length());
	}

	private static String getMD5(byte[] data) throws NoSuchAlgorithmException {
		byte[] hash = MessageDigest.getInstance("MD5").digest(data);
		return new BigInteger(1, hash).toString(16);
	}

	private static String getSHA1(byte[] data) throws NoSuchAlgorithmException {
		byte[] hash = MessageDigest.getInstance("SHA-1").digest(data);
		return new BigInteger(1, hash).toString(16);
	}

	private static void parseDATFile() throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//			factory.setValidating(true);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		mDoc = builder.parse(Main.class.getResourceAsStream("/resources/psx.dat"));

		traverse(mDoc.getElementsByTagName("game"));
	}

	private static void traverse(NodeList nl) {
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName().equals("rom")) {
				String sHex = "00000000".concat(n.getAttributes().getNamedItem("crc").getNodeValue());
				mRefCRC32.put(n.getAttributes().getNamedItem("name").getNodeValue(),
						sHex.substring(sHex.length() - 8, sHex.length()));
				mRefMD5.put(n.getAttributes().getNamedItem("name").getNodeValue(),
						n.getAttributes().getNamedItem("md5").getNodeValue());
				mRefSHA1.put(n.getAttributes().getNamedItem("name").getNodeValue(),
						n.getAttributes().getNamedItem("sha1").getNodeValue());
			}
			traverse(n.getChildNodes());
		}
	}

}
