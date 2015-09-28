package disk_index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class handles the creation of k-grams and processes wildcard queries.
 *
 */

public class KGramIndex {

	public HashMap<String, List<String>> gramIndex1;
	public HashMap<String, List<String>> gramIndex2;
	public HashMap<String, List<String>> gramIndex3;
	public HashMap<String, List<String>> gramIndex = new HashMap<String, List<String>>();

	/**
	 * fetches the hashmaps saved on disk into corresponding hashmap objects,
	 * and if not found assigns memory for new hashmaps.
	 * 
	 * @param directory
	 */

	public KGramIndex(String directory) {
		try {
			final Path currentWorkingPath = Paths.get("").resolve(directory)
					.toAbsolutePath();
			File f = new File(currentWorkingPath.toString(), "KGramIndex.ser");
			if (f.exists()) {
				FileInputStream fileIpStream = new FileInputStream(f);

				ObjectInputStream ObjectIpStream = new ObjectInputStream(
						fileIpStream);
				try {
					gramIndex1 = (HashMap<String, List<String>>) ObjectIpStream
							.readObject();
					gramIndex2 = (HashMap<String, List<String>>) ObjectIpStream
							.readObject();
					gramIndex3 = (HashMap<String, List<String>>) ObjectIpStream
							.readObject();

					ObjectIpStream.close();
					fileIpStream.close();

				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				gramIndex1 = new HashMap<String, List<String>>();
				gramIndex2 = new HashMap<String, List<String>>();
				gramIndex3 = new HashMap<String, List<String>>();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Builds 3-, 2-, and 1-grams and places the grams and the words in their
	 * respective hash maps. This method takes type as a parameter.
	 * 
	 * @param type
	 */

	public void buildKGrams(String type) {
		type = type.toLowerCase();
		String kgramString = "$" + type + "$";
		String kgram3;
		String kgram2;
		String kgram1;
		for (int i = 0; i < kgramString.length(); i++) {
			if (i < (kgramString.length() - 2) && type.length() > 1) {
				kgram3 = kgramString.substring(i, i + 3);
				addToIndex(type, kgram3, gramIndex3);
			}
			if (i < (kgramString.length() - 1) && type.length() > 0) {
				kgram2 = kgramString.substring(i, i + 2);
				addToIndex(type, kgram2, gramIndex2);
			}
			if (type.length() > 0) {
				kgram1 = kgramString.replace("$", "");
				kgram1 = String.valueOf(kgramString.charAt(i));
				if (!kgram1.equals("$")) {
					addToIndex(type, kgram1, gramIndex1);
				}
			}
		}
	}

	/**
	 * This method adds the type and its k-gram to their respective indices. It
	 * takes type, k-gram and k-gram index as parameters. k-gram index is passed
	 * to specify the index to which the passed k-gram is to be added.
	 * 
	 * @param type
	 * @param kgram
	 * @param kgramIndex
	 */
	private void addToIndex(String type, String kgram,
			HashMap<String, List<String>> kgramIndex) {
		List<String> words = new ArrayList<String>();
		if (kgramIndex.containsKey(kgram)) {
			List<String> wordsIndexed = kgramIndex.get(kgram);
			if (!wordsIndexed.contains(type)) {
				kgramIndex.get(kgram).add(type);
			}
		} else {
			words.add(type);
			kgramIndex.put(kgram, words);
		}
	}

	/**
	 * This method takes a wildcard as parameter, builds its k-grams, intersects
	 * the words associated with the k-grams generated and returns the list of
	 * words that satisfies the wildcard after performing post-processing.
	 * 
	 * @param wildcard
	 * @return
	 */
	public List<String> getWords(String wildcard) {
		wildcard = wildcard.toLowerCase();
		String kgramString = "$" + wildcard + "$";
		String kgram3;
		String kgram2;
		String kgram1;
		String strings[] = kgramString.split("\\*");
		List<String> grams = new ArrayList<String>();

		for (int i = 0; i < strings.length; i++) {
			for (int j = 0; j < strings[i].length(); j++) {
				if (j < (strings[i].length() - 2)) {
					kgram3 = strings[i].substring(j, j + 3);
					queryIndex(kgram3);
					grams.add(kgram3);
				} else if (j < (strings[i].length() - 1)
						&& (strings[i].length() == 2)) {
					kgram2 = strings[i].substring(j, j + 2);
					queryIndex(kgram2);
					grams.add(kgram2);
				} else if (strings[i].length() == 1) {
					kgram1 = strings[i].replace("$", "");
					kgram1 = String.valueOf(strings[i].charAt(j));
					if (!kgram1.equals("$")) {
						queryIndex(kgram1);
						grams.add(kgram1);
					}
				}
			}
		}

		List<String> result = intersectGrams(grams);

		List<String> resultFinal;
		resultFinal = postProcessing(strings, result);

		return resultFinal;

	}

	/**
	 * Intersects the lists of words pointed by the k-grams.
	 * 
	 * @param grams
	 * @return
	 */
	private List<String> intersectGrams(List<String> grams) {
		List<String> list1 = new ArrayList<String>();
		List<String> list2;
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < grams.size(); i++) {
			if (i == 0) {
				list1 = gramIndex.get(grams.get(i));
			} else {
				if (list1 != null) {
					list1.clear();
				}
				if (result.size() >= 1) {
					list1.addAll(result);
				}
			}
			if (i < grams.size() - 1) {
				result.clear();
				list2 = gramIndex.get(grams.get(i + 1));
				if (list1 != null) {
					for (String word : list1) {
						if (list2.contains(word)) {
							result.add(word);
						}
					}
				}
			}
		}
		if (grams.size() == 1) {
			result = gramIndex.get(grams.get(0));
		}
		return result;
	}

	/**
	 * Performs post-processing to remove the unwanted words.
	 * 
	 * @param grams
	 * @param intermediateResult
	 * @return
	 */
	private List<String> postProcessing(String[] grams,
			List<String> intermediateResult) {
		List<String> result = new ArrayList<String>();
		int count = 0;
		String gramString;
		for (String word : intermediateResult) {
			gramString = "$" + word + "$";
			count = 0;
			for (int i = 0; i < grams.length - 1; i++) {
				if (gramString.indexOf(grams[i]) < gramString
						.indexOf(grams[i + 1])
						&& (!grams[i].equals("$") || !grams[i + 1].equals("$"))) {
					count++;
				}
				if (!grams[i].equals("$") && grams[i + 1].equals("$")) {
					count++;
				}
			}
			if (count == grams.length - 1) {
				result.add(word);
			}

		}
		return result;
	}

	/**
	 * Adds the k-grams of the query in a hashmap.
	 * 
	 * @param kgram
	 */
	private void queryIndex(String kgram) {
		if (gramIndex1.containsKey(kgram)) {
			gramIndex.put(kgram, gramIndex1.get(kgram));
		} else if (gramIndex2.containsKey(kgram)) {
			gramIndex.put(kgram, gramIndex2.get(kgram));
		} else if (gramIndex3.containsKey(kgram)) {
			gramIndex.put(kgram, gramIndex3.get(kgram));
		}

	}

	/**
	 * Saves the hashmaps created for the corpus to disk.
	 * 
	 * @param directory
	 */

	public void saveIndexToDisk(String directory) {
		try {
			final Path currentWorkingPath = Paths.get("").resolve(directory)
					.toAbsolutePath();
			FileOutputStream fileOpStream = new FileOutputStream(new File(
					currentWorkingPath.toString(), "KGramIndex.ser"));

			ObjectOutputStream ObjectOpStream = new ObjectOutputStream(
					fileOpStream);
			ObjectOpStream.writeObject(gramIndex1);
			ObjectOpStream.writeObject(gramIndex2);
			ObjectOpStream.writeObject(gramIndex3);
			ObjectOpStream.close();
			fileOpStream.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}