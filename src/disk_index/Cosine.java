package disk_index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

/**
 * This class finds out the author names of disputed papers by calculating
 * cosine similarity for each of the disputed documents.
 * 
 */

public class Cosine {
	float[][] vectors;
	int vocabularySize;
	DiskPositionalIndex diskIndex;
	int[] disputed = new int[] { 43, 45, 46, 47, 48, 49, 50, 51, 52, 58, 59 };

	public Cosine(DiskPositionalIndex diskIndex) {
		this.diskIndex = diskIndex;
	}

	public void buildVectors() {
		HashMap<String, Integer> dictionary = diskIndex.getDictionary();
		vocabularySize = dictionary.size();
		vectors = new float[diskIndex.getFileNames().size()][vocabularySize];
		DefaultRetrieval defaultRetrieval = new DefaultRetrieval();
		PositionalPostingFromFile[] postings;
		Accumulator[] docWeights = defaultRetrieval
				.getDocumentWeights(diskIndex);
		for (Entry<String, Integer> entry : dictionary.entrySet()) {
			postings = diskIndex.GetPostingsWithPositions(entry.getKey());
			for (int i = 0; i < postings.length; i++) {
				vectors[postings[i].getDocID()][entry.getValue()] = defaultRetrieval
						.getWdt(entry.getKey(), diskIndex, postings[i])
						/ docWeights[RankedRetrieval.isDocumentPresent(
								docWeights, postings[i].getDocID())]
								.getDocumentScore();
			}
		}
	}

	public void findAuthors(HashMap<String, List<String>> classes) {

		List<Similarity> sim = null;
		List<Similarity> simMax = new ArrayList<Similarity>();

		Comparator<Similarity> comparator = new Comparator<Similarity>() {
			@Override
			public int compare(Similarity arg0, Similarity arg1) {
				if (arg1.getSimilarityScore() > arg0.getSimilarityScore()) {
					return 1;
				} else {
					return -1;
				}
			}

		};
		PriorityQueue<Similarity> priorityQueue;

		Similarity simTemp;
		for (int i = 0; i < disputed.length; i++) {
			sim = new ArrayList<Similarity>();
			for (int j = 0; j < vectors.length; j++) {
				if (!isThisInDisputed(j)) {
					simTemp = new Similarity();
					simTemp.setDocumentId1(disputed[i]);
					simTemp.setDocumentId2(j);
					for (int k = 0; k < vocabularySize; k++) {
						simTemp.setSimilarityScore(simTemp.getSimilarityScore()
								+ vectors[disputed[i]][k] * vectors[j][k]);
					}
					sim.add(simTemp);
				}
			}
			priorityQueue = new PriorityQueue<Similarity>(1, comparator);
			priorityQueue.addAll(sim);
			simMax.add(priorityQueue.poll());
		}
		System.out.println();
		System.out.println("Disputed\tSimilar to\t\tAuthor");
		System.out.println("-----------------------------------------------");
		for (int i = 0; i < simMax.size(); i++) {
			if (classes.get("HAMILTON").contains(
					diskIndex.getFileNames()
							.get(simMax.get(i).getDocumentId2()))) {
				System.out.println(diskIndex.getFileNames().get(
						simMax.get(i).getDocumentId1())
						+ "\t"
						+ diskIndex.getFileNames().get(
								simMax.get(i).getDocumentId2())
						+ "\t-->\tHAMILTON");
			} else if (classes.get("MADISON").contains(
					diskIndex.getFileNames()
							.get(simMax.get(i).getDocumentId2()))) {
				System.out.println(diskIndex.getFileNames().get(
						simMax.get(i).getDocumentId1())
						+ "\t"
						+ diskIndex.getFileNames().get(
								simMax.get(i).getDocumentId2())
						+ "\t-->\tMADISON");
			}
		}

	}

	private boolean isThisInDisputed(int documentId) {
		for (int i = 0; i < disputed.length; i++) {
			if (disputed[i] == documentId) {
				return true;
			}
		}
		return false;

	}
}
