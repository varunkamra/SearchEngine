package disk_index;

/**
 * This class uses following formulas: 
 * idf = N/df, 
 * wqt=idf, 
 * wdt=tf * idf, 
 * Wd= docWeights(d).
 * 
 */
public class TraditionalRetrieval extends RankedRetrieval {

	public float getWqt(String term, DiskPositionalIndex diskIndex) {
		return (float) Math.log(((float) diskIndex.getFileNames().size())
				/ diskIndex.GetPostingsWithPositions(term).length);
	}

	public float getWdt(String term, DiskPositionalIndex diskIndex,
			PositionalPostingFromFile positionlPosting) {
		return getWqt(term, diskIndex)
				* (float) positionlPosting.getPositions().length;
	}

	@Override
	public Accumulator[] getDocumentWeights(DiskPositionalIndex diskIndex) {
		return diskIndex.getDocumentWeights();

	}

}
