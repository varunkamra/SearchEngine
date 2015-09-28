package disk_index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * This class uses following formulas: 
 * wqt = max[0, ln((N - df)/df), 
 * wdt= (1 + ln(tf))/1 + ln(ave(tf)), 
 * Wd=sqrt(ByteSize(d))
 * 
 */
public class WackyRetrieval extends RankedRetrieval {

	@Override
	public float getWqt(String term, DiskPositionalIndex diskIndex) {
		float df = diskIndex.GetPostingsWithPositions(term).length;
		float val = (float) Math
				.log((float) (diskIndex.getFileNames().size() - df) / df);
		return Math.max(0, val);
	}

	@Override
	public float getWdt(String term, DiskPositionalIndex diskIndex,
			PositionalPostingFromFile positionalPosting) {
		PositionalPostingFromFile[] positionalPostingArray = diskIndex
				.GetPostingsWithPositions(term);
		float averageTermFrequency = 0;
		for (int i = 0; i < positionalPostingArray.length; i++) {
			averageTermFrequency = averageTermFrequency
					+ positionalPostingArray[i].getPositions().length;
		}
		averageTermFrequency = averageTermFrequency
				/ positionalPostingArray.length;
		return (float) ((1 + Math.log(positionalPosting.getPositions().length)) / (1 + Math
				.log(averageTermFrequency)));
	}

	@Override
	public Accumulator[] getDocumentWeights(DiskPositionalIndex diskIndex) {
		List<String> files = diskIndex.getFileNames();
		RandomAccessFile tempFile;
		Accumulator weights[] = new Accumulator[diskIndex.getFileNames().size()];
		int insertPosition = 0;
		Accumulator temp;
		int documentId = 0;
		for (String file : files) {
			try {
				temp = new Accumulator();
				tempFile = new RandomAccessFile(new File(
						DiskPositionalIndex.mPath, file), "r");
				try {
					temp.setDocumentId(documentId);
					temp.setDocumentScore((float) Math.sqrt(tempFile.length()));
					weights[insertPosition] = temp;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			insertPosition++;
			documentId++;
		}
		return weights;

	}

}
