package disk_index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;

/**
 * Given a set of classes of documents, Bayesian Classifier classifies the class
 * of each document whose class is unknown. While choosing the Bayesian
 * classifier in Disk engine give ALL directory as the index name to index.
 *
 */

public class BayesianClassifier {
	private DiskPositionalIndex diskIndex;
	private List<TermDiscriminatingPower> hamiltonClass = new ArrayList<TermDiscriminatingPower>();
	private List<TermDiscriminatingPower> madisonClass = new ArrayList<TermDiscriminatingPower>();
	private List<TermDiscriminatingPower> jayClass = new ArrayList<TermDiscriminatingPower>();
	private List<TermDiscriminatingPower> termsWithHighestScores;
	private List<String> discriminatingTerms;
	private HashMap<String, HashMap<String, Integer>> occurrenceClasses = new HashMap<String, HashMap<String, Integer>>();
	private HashMap<String, HashMap<String, Float>> termProbabilities = new HashMap<String, HashMap<String, Float>>();
	private HashMap<String, List<String>> classes;
	private float N11 = 0;
	private float N00 = 0;
	private float N10 = 0;
	private float N01 = 0;
	private float N1X = 0;
	private float NX1 = 0;
	private float N0X = 0;
	private float NX0 = 0;

	public BayesianClassifier(DiskPositionalIndex diskIndex,
			HashMap<String, List<String>> classes) {
		this.diskIndex = diskIndex;
		this.classes = classes;
	}

	public void findDiscriminatingTerms() {
		float N = diskIndex.getFileNames().size();
		HashMap<String, Integer> dictionary = diskIndex.getDictionary();
		PositionalPostingFromFile[] postings;
		TermDiscriminatingPower temp;
		float mutualInfo = 0;
		int hamilton = 0;
		int madison = 0;
		int jay = 0;
		for (Entry<String, Integer> entry : dictionary.entrySet()) {
			hamilton = 0;
			madison = 0;
			postings = diskIndex.GetPostingsWithPositions(entry.getKey());
			for (int i = 0; i < postings.length; i++) {
				if (classes.get("HAMILTON").contains(
						diskIndex.getFileNames().get(postings[i].getDocID()))) {
					hamilton++;
				} else if (classes.get("MADISON").contains(
						diskIndex.getFileNames().get(postings[i].getDocID()))) {
					madison++;
				} else if (classes.get("JAY").contains(
						diskIndex.getFileNames().get(postings[i].getDocID()))) {
					jay++;
				}
			}

			N01 = classes.get("HAMILTON").size() - hamilton;
			N11 = hamilton;
			N10 = postings.length - hamilton;
			N00 = N - classes.get("HAMILTON").size() - N10;
			N1X = postings.length;
			NX1 = N01 + N11;
			N0X = N00 + N01;
			NX0 = N00 + N10;
			mutualInfo = (float) ((N11 / N) * (Math.log10((N * N11)
					/ (N1X * NX1))))
					+ (float) ((N10 / N) * (Math.log10((N * N10) / (N1X * NX0))))
					+ (float) ((N01 / N) * (Math.log10((N * N01) / (N0X * NX1))))
					+ (float) ((N00 / N) * (Math.log10((N * N00) / (N0X * NX0))));
			temp = new TermDiscriminatingPower();
			temp.setTerm(entry.getKey());
			temp.setDiscriminatingPower(mutualInfo);
			if (!Float.isNaN(mutualInfo)) {
				hamiltonClass.add(temp);
			}

			N01 = classes.get("MADISON").size() - madison;
			N11 = madison;
			N10 = postings.length - madison;
			N00 = N - classes.get("MADISON").size() - N10;
			N1X = postings.length;
			NX1 = N01 + N11;
			N0X = N00 + N01;
			NX0 = N00 + N10;

			mutualInfo = (float) ((N11 / N) * (Math.log10((N * N11)
					/ (N1X * NX1))))
					+ (float) ((N10 / N) * (Math.log10((N * N10) / (N1X * NX0))))
					+ (float) ((N01 / N) * (Math.log10((N * N01) / (N0X * NX1))))
					+ (float) ((N00 / N) * (Math.log10((N * N00) / (N0X * NX0))));
			temp = new TermDiscriminatingPower();
			temp.setTerm(entry.getKey());
			temp.setDiscriminatingPower(mutualInfo);
			if (!Float.isNaN(mutualInfo)) {
				madisonClass.add(temp);
			}

			N01 = classes.get("JAY").size() - jay;
			N11 = jay;
			N10 = postings.length - jay;
			N00 = N - classes.get("JAY").size() - N10;
			N1X = postings.length;
			NX1 = N01 + N11;
			N0X = N00 + N01;
			NX0 = N00 + N10;

			mutualInfo = (float) ((N11 / N) * (Math.log10((N * N11)
					/ (N1X * NX1))))
					+ (float) ((N10 / N) * (Math.log10((N * N10) / (N1X * NX0))))
					+ (float) ((N01 / N) * (Math.log10((N * N01) / (N0X * NX1))))
					+ (float) ((N00 / N) * (Math.log10((N * N00) / (N0X * NX0))));
			temp = new TermDiscriminatingPower();
			temp.setTerm(entry.getKey());
			temp.setDiscriminatingPower(mutualInfo);
			if (!Float.isNaN(mutualInfo)) {
				jayClass.add(temp);
			}
		}
		Comparator<TermDiscriminatingPower> comparator = new Comparator<TermDiscriminatingPower>() {
			@Override
			public int compare(TermDiscriminatingPower arg0,
					TermDiscriminatingPower arg1) {
				if (arg1.getDiscriminatingPower() > arg0
						.getDiscriminatingPower()) {
					return 1;
				} else {
					return -1;
				}
			}

		};
		PriorityQueue<TermDiscriminatingPower> priorityQueue = new PriorityQueue<TermDiscriminatingPower>(
				1, comparator);
		termsWithHighestScores = new ArrayList<TermDiscriminatingPower>();
		termsWithHighestScores.addAll(hamiltonClass);
		termsWithHighestScores.addAll(madisonClass);
		termsWithHighestScores.addAll(jayClass);
		discriminatingTerms = new ArrayList<String>();
		priorityQueue.addAll(termsWithHighestScores);
		int count = 0;
		// The following line can be modified to get discriminating terms other
		// than 100.
		while (count < 1600) {
			temp = priorityQueue.poll();
			if (!discriminatingTerms.contains(temp.getTerm())) {
				discriminatingTerms.add(temp.getTerm());
				count++;
			}
		}
	}

	public void laplaceSmoothing() {
		List<String> filenames;
		Path path;
		FileReader fileReader;
		Scanner posReader;
		String next = null;
		int temp = 0;
		HashMap<String, Integer> occurrenceClass;
		for (Entry<String, List<String>> entry : classes.entrySet()) {
			filenames = classes.get(entry.getKey());
			path = Paths.get("").resolve(entry.getKey());
			occurrenceClass = new HashMap<String, Integer>();
			try {
				for (int i = 0; i < filenames.size(); i++) {
					fileReader = new FileReader(path.resolve(filenames.get(i))
							.toFile());
					posReader = new Scanner(fileReader);
					while (posReader.hasNext()) {
						next = posReader.next().replaceAll("\\W", "")
								.toLowerCase();
						next = PorterStemmer.processToken(next);
						if (occurrenceClass.containsKey(next)) {
							temp = occurrenceClass.get(next) + 1;
							occurrenceClass.put(next, temp);
						} else {
							occurrenceClass.put(next, 1);
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			occurrenceClasses.put(entry.getKey(), occurrenceClass);
		}
		HashMap<String, Integer> tempHash;
		HashMap<String, Float> probabilityScore;
		float probability;
		float ftc1 = 0;
		for (Entry<String, HashMap<String, Integer>> entry : occurrenceClasses
				.entrySet()) {
			probabilityScore = new HashMap<String, Float>();
			ftc1 = 0;
			tempHash = entry.getValue();
			for (String term : discriminatingTerms) {
				ftc1 = ftc1
						+ (tempHash.containsKey(term) ? tempHash.get(term) : 0);
			}
			for (String term : discriminatingTerms) {
				probability = ((tempHash.containsKey(term) ? tempHash.get(term)
						: 0) + 1) / (ftc1 + discriminatingTerms.size());
				probabilityScore.put(term, probability);
			}
			termProbabilities.put(entry.getKey(), probabilityScore);
		}
	}

	public void findClass() {
		Path path = Paths.get("").resolve("HAMILTON OR MADISON");
		File directory = path.toFile();
		FileReader fileReader;
		Scanner posReader;
		String next = null;
		BigDecimal[] probability;
		int max;
		float pc;
		System.out.println("Paper\t   \t\tMystery Author");
		System.out.println("---------------------------------------");
		for (File file : directory.listFiles()) {
			probability = new BigDecimal[3];
			for (int i = 0; i < probability.length; i++) {
				probability[i] = new BigDecimal(1);
			}
			try {
				fileReader = new FileReader(file);
				posReader = new Scanner(fileReader);
				while (posReader.hasNext()) {
					next = posReader.next().replaceAll("\\W", "").toLowerCase();
					next = PorterStemmer.processToken(next);
					if (termProbabilities.get("HAMILTON").containsKey(next)) {
						probability[0] = probability[0]
								.multiply(new BigDecimal(termProbabilities.get(
										"HAMILTON").get(next)));
						probability[1] = probability[1]
								.multiply(new BigDecimal(termProbabilities.get(
										"MADISON").get(next)));
						probability[2] = probability[2]
								.multiply(new BigDecimal(termProbabilities.get(
										"JAY").get(next)));
					}
				}
				// Here the probability of each class has been found using 71 as
				// the total number of documents, since 3 out of 74 documents
				// belong to Hamilton and Madison class.
				pc = ((float) 51) / 71;
				probability[0] = probability[0].multiply(new BigDecimal(pc));
				pc = ((float) 15) / 71;
				probability[1] = probability[1].multiply(new BigDecimal(pc));
				pc = ((float) 5) / 71;
				probability[2] = probability[2].multiply(new BigDecimal(pc));
				max = findMax(probability);
				if (max == 0) {
					System.out.println(file.getName() + "\t-->\t" + "HAMILTON");
				} else if (max == 1) {
					System.out.println(file.getName() + "\t-->\t" + "MADISON");
				} else {
					System.out.println(file.getName() + "\t-->\t" + "JAY");
				}

			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}

		}

	}

	private int findMax(BigDecimal probability[]) {
		BigDecimal max = new BigDecimal(0);
		int index = 0;
		for (int i = 0; i < probability.length; i++) {
			if (probability[i].compareTo(max) == 1) {
				max = probability[i];
				index = i;
			}
		}
		return index;
	}
}
