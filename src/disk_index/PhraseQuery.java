package disk_index;

/**
 * This class processes normal phrase queries as well as phrase queries with
 * near operator.
 * 
 */
public class PhraseQuery {
	private int insertPosition = 0;

	public PositionalPostingFromFile[] processPhraseQuery(String phrase,
			DiskPositionalIndex index) {
		String[] phraseWords = phrase.split(" ");
		String tempString = null;
		PositionalPostingFromFile[][] positionalPostings = new PositionalPostingFromFile[phrase
				.toLowerCase().contains("near/") ? phraseWords.length - 1
				: phraseWords.length][];
		int near = 1;
		boolean containsNEAR = false;
		String term;
		for (int i = 0; i < phraseWords.length; i++) {
			term = phraseWords[i].toLowerCase();
			if (term.contains("near/")) {
				near = Character
						.getNumericValue(term.charAt(term.length() - 1));
				containsNEAR = true;
			} else {
				tempString = PorterStemmer.processToken(term);
				if (index.GetPostingsWithPositions(tempString) != null
						&& !containsNEAR) {
					positionalPostings[i] = index
							.GetPostingsWithPositions(tempString);
				} else if (index.GetPostingsWithPositions(tempString) != null
						&& containsNEAR) {
					positionalPostings[i - 1] = index
							.GetPostingsWithPositions(tempString);
				} else {
					System.out
							.println("Some words inside the phrase query are not present in the corpus.");
					break;
				}
			}
		}

		PositionalPostingFromFile[] list1;
		PositionalPostingFromFile[] list2;
		boolean flag = false;
		PositionalPostingFromFile[] result;

		result = new PositionalPostingFromFile[index.getFileNames().size()];
		for (int i = 0; i < positionalPostings.length - 1; i++) {
			if (i == 0) {
				list1 = positionalPostings[i];
			} else {
				list1 = result;
			}

			// result.clear();
			insertPosition = 0;

			result = QueryLanguage.initializeArrayForAND(positionalPostings);
			list2 = positionalPostings[i + 1];
			int j = 0;
			int k = 0;
			if (list1 != null && list2 != null) {
				while (j < list1.length && k < list2.length) {
					if (list1[j] != null && list2[k] != null) {

						if (list1[j].getDocID() == list2[k].getDocID()) {
							flag = false;
							for (Integer occurrence : list1[j].getPositions()) {
								for (Integer occurrence2 : list2[k]
										.getPositions()) {
									if (occurrence2 == occurrence + near) {
										result[insertPosition] = list2[k];
										insertPosition++;
										flag = true;
										break;
									}
								}
								if (flag) {
									break;
								}

							}
							j++;
							k++;
						} else if (list1[j].getDocID() < list2[k].getDocID()) {
							j++;
						} else {
							k++;
						}
					} else {
						break;
					}

				}
			}

		}
		if (positionalPostings.length == 1) {
			return positionalPostings[0];
		}
		return result;
	}
}
