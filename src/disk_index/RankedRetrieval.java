package disk_index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

/**
 * An abstract class which is extended by 4 other classes to achieve ranked
 * retrieval using different mechanisms. The retrieve method returns top 10
 * results to the RankedRetrievalEngine class.
 * 
 */

public abstract class RankedRetrieval {
	public abstract float getWqt(String term, DiskPositionalIndex diskIndex);

	public abstract float getWdt(String term, DiskPositionalIndex diskIndex,
			PositionalPostingFromFile positionalPosting);

	public abstract Accumulator[] getDocumentWeights(
			DiskPositionalIndex diskIndex);

	public Accumulator[] retrieve(String input, DiskPositionalIndex diskIndex) {
		Accumulator[] docWeights;
		HashMap<Integer, Float> accumulator = new HashMap<Integer, Float>();
		String[] queryTerms = input.split(" ");

		float wqt, wdt;
		String term;
		float incrementedValue;
		PositionalPostingFromFile[] positionalPosting;
		for (int i = 0; i < queryTerms.length; i++) {
			term = PorterStemmer.processToken(queryTerms[i].toLowerCase());

			positionalPosting = diskIndex.GetPostingsWithPositions(term);
			wqt = getWqt(term, diskIndex);
			System.out.println("wqt:\t" + wqt);
			for (int j = 0; j < positionalPosting.length; j++) {
				incrementedValue = 0;
				if (positionalPosting[j] != null) {
					wdt = getWdt(term, diskIndex, positionalPosting[j]);
					if (accumulator
							.containsKey(positionalPosting[j].getDocID())) {
						incrementedValue = accumulator.get(positionalPosting[j]
								.getDocID());
					}
					incrementedValue += (wqt * wdt);
					accumulator.put(positionalPosting[j].getDocID(),
							incrementedValue);
				}

			}
		}
		docWeights = getDocumentWeights(diskIndex);
		float normalizedvalue;
		if (docWeights != null) {
			for (Entry<Integer, Float> entry : accumulator.entrySet()) {
				normalizedvalue = entry.getValue()
						/ docWeights[isDocumentPresent(docWeights,
								entry.getKey())].getDocumentScore();
				accumulator.put(entry.getKey(), normalizedvalue);
			}
		}
		List<Accumulator> accumulatorList = new ArrayList<Accumulator>();
		Accumulator temp;
		for (Entry<Integer, Float> entry : accumulator.entrySet()) {
			temp = new Accumulator();
			temp.setDocumentId(entry.getKey());
			temp.setDocumentScore(entry.getValue());
			accumulatorList.add(temp);
		}
		Comparator<Accumulator> comparator = new Comparator<Accumulator>() {

			@Override
			public int compare(Accumulator arg0, Accumulator arg1) {
				if (arg1.getDocumentScore() > arg0.getDocumentScore()) {
					return 1;
				} else {
					return -1;
				}
			}

		};

		PriorityQueue<Accumulator> priorityQueue = new PriorityQueue<Accumulator>(
				1, comparator);

		priorityQueue.addAll(accumulatorList);
		int insertPositon = 0;
		Accumulator[] accumulatorResult = new Accumulator[10];
		for (int i = 0; i < priorityQueue.size(); i++) {
			temp = priorityQueue.poll();
			if (insertPositon < 10) {
				accumulatorResult[insertPositon] = temp;
				insertPositon++;
			}
		}
		return accumulatorResult;

	}

	public static int isDocumentPresent(Accumulator[] accumulator, int docId) {
		for (int i = 0; i < accumulator.length; i++) {
			if (accumulator[i] != null
					&& accumulator[i].getDocumentId() == docId) {
				return i;
			}
		}

		return -1;
	}
}
