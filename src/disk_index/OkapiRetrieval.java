package disk_index;

/**
 * This class uses following formulas: 
 * wqt=ln((N - df + 0.5)/(df + 0.5)), 
 * Kd=1.2 * (0.25 + (0.75 * docWeights(d)/docWeights(A))), 
 * wdt= (2.2 * tf)(Kd + tf),
 * Wd=1
 *
 */
public class OkapiRetrieval extends RankedRetrieval {

	@Override
	public float getWqt(String term, DiskPositionalIndex diskIndex) {
		float df = diskIndex.GetPostingsWithPositions(term).length;
		return (float) Math
				.log((((float) diskIndex.getFileNames().size()) - df + 0.5)
						/ (df + 0.5));

	}

	@Override
	public float getWdt(String term, DiskPositionalIndex diskIndex,
			PositionalPostingFromFile positionalPosting) {
		Accumulator[] docWeights = diskIndex.getDocumentWeights();
		float docWeightD = docWeights[isDocumentPresent(docWeights,
				positionalPosting.getDocID())].getDocumentScore();
		float docWeightA = docWeights[docWeights.length - 1].getDocumentScore();
		float kd = (float) (1.2 * (0.25 + (0.75 * (docWeightD / docWeightA))));
		float termFrequency = positionalPosting.getPositions().length;
		float wdt = (float) ((2.2 * termFrequency) / (kd + termFrequency));
		return wdt;
	}

	@Override
	public Accumulator[] getDocumentWeights(DiskPositionalIndex diskIndex) {
		return null;
	}

}
