package disk_index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This class indexes each term received in a hashmap.
 *
 */
public class PositionalInvertedIndex {
	public HashMap<String, List<PositionalPosting>> mIndex;

	public PositionalInvertedIndex() {
		mIndex = new HashMap<String, List<PositionalPosting>>();
	}

	public void addTerm(String term, int documentID, List<Integer> positions) {
		boolean containsID = false;
		boolean containsTerm = false;
		containsTerm = mIndex.containsKey(term);
		if (containsTerm) {
			List<PositionalPosting> posting = mIndex.get(term);
			List<Integer> temp = new ArrayList<Integer>();
			for (PositionalPosting positional : posting) {
				if (positional.getDocID() == documentID) {
					containsID = true;
					List<Integer> docPosting = positional.getPositions();
					if (positions != null) {
						for (Integer position : positions) {
							if (!docPosting.contains(position)) {
								temp.add(position);
							}
						}
					}

					if (temp.size() > 0) {
						positional.addPositions(temp);
					}
				}
			}
			if (!containsID) {
				PositionalPosting tempPositionalPosting = new PositionalPosting();
				tempPositionalPosting.setDocID(documentID);
				tempPositionalPosting.setPositions(positions);

				List<PositionalPosting> newPositionalPostingList = new ArrayList<PositionalPosting>();
				newPositionalPostingList.add(tempPositionalPosting);
				mIndex.get(term).add(tempPositionalPosting);

			}

		}
		if (!containsTerm) {
			PositionalPosting tempPositionalPosting = new PositionalPosting();
			tempPositionalPosting.setDocID(documentID);
			tempPositionalPosting.setPositions(positions);

			List<PositionalPosting> newPositionalPostingList = new ArrayList<PositionalPosting>();
			newPositionalPostingList.add(tempPositionalPosting);
			mIndex.put(term, newPositionalPostingList);
		}
	}

	public List<PositionalPosting> getPostings(String term) {
		return mIndex.get(term);

	}

	public int getTermCount() {
		return mIndex.entrySet().size();
	}

	public String[] getDictionary() {
		String[] keys = new String[mIndex.entrySet().size()];
		int i = 0;
		for (String key : mIndex.keySet()) {
			keys[i] = key;
			i++;
		}
		Arrays.sort(keys);
		return keys;
	}

}
