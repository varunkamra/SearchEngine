package disk_index;

/**
 * This class uses following formulas: 
 * wqt= ln(1+N/df), 
 * wdt=1 + ln(tf),
 * Wd=docWeights(d).
 * 
 */

public class DefaultRetrieval extends RankedRetrieval {

	@Override
	public float getWqt(String term, DiskPositionalIndex diskIndex) {
		return (float) Math.log1p(((float) diskIndex.getFileNames().size())
				/ diskIndex.GetPostingsWithPositions(term).length);
	}

	@Override
	public float getWdt(String input, DiskPositionalIndex diskIndex,
			PositionalPostingFromFile positionalPosting) {
		return (float) (1 + Math.log(positionalPosting.getPositions().length));

	}

	@Override
	public Accumulator[] getDocumentWeights(DiskPositionalIndex diskIndex) {
		return diskIndex.getDocumentWeights();

	}
}
