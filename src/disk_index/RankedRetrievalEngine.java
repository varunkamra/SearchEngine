package disk_index;

import java.util.List;

/**
 * This class implements the strategy pattern, by setting the instance of class
 * recieved as parameter. It also prints the top 10 results returned by calling
 * the retrieve method of RankedRetrieval class.
 * 
 */

public class RankedRetrievalEngine {

	private RankedRetrieval rankedRetrieval;

	public void setRetrievalMode(RankedRetrieval retrievalMode) {
		this.rankedRetrieval = retrievalMode;
	}

	public void printResult(String input, DiskPositionalIndex diskIndex) {
		Accumulator[] result = rankedRetrieval.retrieve(input, diskIndex);
		System.out.println("Top 10 retrieved documents: ");
		List<String> fileNames = diskIndex.getFileNames();
		for (int i = 0; i < 10; i++) {
			if (result[i] != null) {
				System.out.println(fileNames.get(result[i].getDocumentId())
						+ "\t" + result[i].getDocumentScore());
			}
		}
	}
}
