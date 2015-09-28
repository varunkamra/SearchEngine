package disk_index;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class accepts a query from a user in which the operators for NOT - and
 * for OR + are used by putting a space before and after the operator. It parses
 * the query and performs the given operations.
 *
 */
public class QueryLanguage {
	int insertPositionForOR = 0;
	int insertPositionForAND = 0;

	public PositionalPostingFromFile[] processQuery(String phrase,
			DiskPositionalIndex index, KGramIndex kgram) {

		boolean correctSyntax = querySyntaxChecker(phrase);
		if (!correctSyntax) {
			return null;
		}
		List<SubQuery> literals = new ArrayList<SubQuery>();

		queryParser(phrase, literals);

		PhraseQuery phraseQuery = new PhraseQuery();
		PositionalPostingFromFile[][] resultList = null;
		PositionalPostingFromFile[] resultPostingsForPhrase;
		PositionalPostingFromFile[] resultPostings = null;
		PositionalPostingFromFile[] resultOfQueryLiteralsAND = null;

		// This loop will perform all the operation of AND over the result of
		// phrase queries and the result of normal query literals and store
		// there result in the same Literal object.
		List<List<String>> words = new ArrayList<List<String>>();
		PositionalPostingFromFile[][] wildcardsPostings;
		PositionalPostingFromFile[][] intermediateWildcardsResult = null;
		PositionalPostingFromFile[] wildcardsResult = null;
		for (SubQuery subQuery : literals) {
			if (subQuery.getWildcards() != null
					&& !subQuery.getWildcards().isEmpty()) {
				words.clear();
				for (String wildcard : subQuery.getWildcards()) {
					words.add(kgram.getWords(wildcard));
				}
				intermediateWildcardsResult = new PositionalPostingFromFile[words
						.size()][];
				// wildcardsPostings = new
				// PositionalPostingFromDisk[words.size()][];
				String word;
				List<String> termsPerWildcard;
				for (int i = 0; i < words.size(); i++) {
					termsPerWildcard = new ArrayList<String>();
					for (int k = 0; k < words.get(i).size(); k++) {
						word = PorterStemmer.processToken(words.get(i).get(k));
						termsPerWildcard.add(word);
					}
					if (getPositionalPostingsList(termsPerWildcard, index) != null
							&& getPositionalPostingsList(termsPerWildcard,
									index).length != 0) {
						wildcardsPostings = getPositionalPostingsList(
								termsPerWildcard, index);
						intermediateWildcardsResult[i] = operationOR(wildcardsPostings);
					}
				}
				wildcardsResult = performAND(intermediateWildcardsResult);

			}
			PositionalPostingFromFile[][] positionalPostings = new PositionalPostingFromFile[subQuery
					.getQueryLiterals() == null ? 0 : subQuery
					.getQueryLiterals().size()][];
			if (getPositionalPostingsList(subQuery.getQueryLiterals(), index) != null
					&& !subQuery.getQueryLiterals().isEmpty()) {
				positionalPostings = getPositionalPostingsList(
						subQuery.getQueryLiterals(), index);
			}
			// perform AND operation on query literals
			resultOfQueryLiteralsAND = performAND(positionalPostings);

			// process phrase queries one by one and add the result in a
			// resultList.
			int positivePhraseListSize = subQuery.getPhraseLiterals() != null ? subQuery
					.getPhraseLiterals().size() : 0;

			int negativePhraseListSize = subQuery.getNegativePhrases() != null ? subQuery
					.getNegativePhrases().size() : 0;

			resultList = new PositionalPostingFromFile[positivePhraseListSize < negativePhraseListSize ? negativePhraseListSize
					: positivePhraseListSize][];
			for (int i = 0; i < subQuery.getPhraseLiterals().size(); i++) {
				resultPostingsForPhrase = phraseQuery.processPhraseQuery(
						subQuery.getPhraseLiterals().get(i), index);
				if (resultPostingsForPhrase != null) {
					resultList[i] = resultPostingsForPhrase;
				}
			}
			// if more than one phrase queries are there then perform AND
			// operation on their result positings list.
			if (resultList.length > 1
					&& subQuery.getPhraseLiterals().size() > 1) {
				resultPostings = performAND(resultList);
			} else if (resultList.length == 1) {
				resultPostings = resultList[0];
			} else {

			}
			// perform AND operation on the result of AND operations of phrase
			// queries and normal literals
			int insertPos = 0;
			int arraySize1 = resultList != null ? resultList.length : 0;
			int arraySize2 = wildcardsResult != null ? wildcardsResult.length
					: 0;

			int arraySize3 = resultOfQueryLiteralsAND != null ? resultOfQueryLiteralsAND.length
					: 0;
			int size = arraySize1 > 0 ? (arraySize2 > 0 ? (arraySize3 > 0 ? 3
					: 2) : (arraySize3 > 0 ? 2 : 1))
					: (arraySize2 > 0 ? (arraySize3 > 0 ? 2 : 1)
							: (arraySize3 > 0 ? 1 : 0));
			positionalPostings = new PositionalPostingFromFile[size][];

			if (resultList != null && resultList.length > 0) {
				positionalPostings[insertPos] = resultPostings;
				insertPos++;
			}
			if (resultOfQueryLiteralsAND != null
					&& resultOfQueryLiteralsAND.length > 0) {
				positionalPostings[insertPos] = resultOfQueryLiteralsAND;
				insertPos++;
			}
			if (wildcardsResult != null && wildcardsResult.length > 0) {
				positionalPostings[insertPos] = wildcardsResult;
				insertPos++;
			}

			PositionalPostingFromFile[] resultOfLiteralAndPhrase;
			if (positionalPostings.length > 1
					&& (resultOfQueryLiteralsAND != null
							&& resultOfQueryLiteralsAND.length == 0 ? subQuery
							.getQueryLiterals().isEmpty() : true)
					&& (resultPostings != null && resultPostings.length == 0 ? subQuery
							.getPhraseLiterals().isEmpty() : true)
					&& (wildcardsResult != null && wildcardsResult.length == 0 ? (subQuery
							.getWildcards() == null ? true : subQuery
							.getWildcards().isEmpty()) : true)) {
				resultOfLiteralAndPhrase = performAND(positionalPostings);
				subQuery.setResultPositionalPostings(resultOfLiteralAndPhrase);
			} else if (positionalPostings.length == 1
					&& (resultOfQueryLiteralsAND != null
							&& resultOfQueryLiteralsAND.length == 0 ? subQuery
							.getQueryLiterals().isEmpty() : true)
					&& (resultPostings != null && resultPostings.length == 0 ? subQuery
							.getPhraseLiterals().isEmpty() : true)
					&& (wildcardsResult != null && wildcardsResult.length == 0 ? (subQuery
							.getWildcards() == null ? true : subQuery
							.getWildcards().isEmpty()) : true)) {
				subQuery.setResultPositionalPostings(positionalPostings[0]);
			}
			// checks for the presence of only negative literals and negative
			// phrases
			if (subQuery.getQueryLiterals().isEmpty()
					&& subQuery.getPhraseLiterals().isEmpty()
					&& (subQuery.getWildcards() == null || subQuery
							.getWildcards().isEmpty())
					&& (!subQuery.getNegativeLiterals().isEmpty() || !subQuery
							.getNegativePhrases().isEmpty())) {
				System.out.println("Cannot process only negative literal(s)!");
				return null;
			}

		}

		performNOTOnEachNegativeLiteralInSubQuery(index, literals, phraseQuery,
				resultList, resultPostings, resultOfQueryLiteralsAND);

		// Performs OR operation
		PositionalPostingFromFile[][] operationORparameters = new PositionalPostingFromFile[literals == null ? 0
				: literals.size()][];
		for (int i = 0; i < literals.size(); i++) {
			operationORparameters[i] = literals.get(i)
					.getResultPositionalPostings();

		}
		PositionalPostingFromFile[] finalResult;
		finalResult = operationOR(operationORparameters);
		return finalResult;
	}

	private void queryParser(String phrase, List<SubQuery> literals) {
		String[] strings = phrase.split(" \\+ ");
		int beginIndex;
		int endIndex;
		String tempString = null;

		int j;
		for (int i = 0; i < strings.length; i++) {
			j = 0;
			beginIndex = 0;
			endIndex = 0;
			SubQuery tempLiteral = new SubQuery();
			while (j < strings[i].length()) {

				// checks for a phrase query and adds it in respective list.
				if (strings[i].charAt(j) == '\"'
						&& (j >= 2 ? strings[i].charAt(j - 2) != '-' : true)) {
					endIndex = strings[i].indexOf('\"', j + 1);
					tempLiteral.getPhraseLiterals().add(
							strings[i].substring(j + 1, endIndex));
					j = endIndex + 2;
					beginIndex = j;
				}

				// checks for presence of normal query literals and adds it
				// in its own list.
				else if (strings[i].charAt(j) == ' ') {
					tempString = strings[i].substring(beginIndex, j)
							.toLowerCase();
					if (tempString.contains("*")) {
						tempLiteral.addWildcard(tempString);
						beginIndex = j + 1;
						j = beginIndex;
						continue;
					}
					tempString = PorterStemmer.processToken(tempString)
							.toLowerCase();

					tempLiteral.getQueryLiterals().add(tempString);
					beginIndex = j + 1;
					j = beginIndex;
				}

				// checks for negative literals and adds them to their list.
				else if (strings[i].charAt(j) == '-'
						&& (j <= (strings[i].length() - 3) ? strings[i]
								.charAt(j + 2) != '\"' : true)) {
					endIndex = strings[i].indexOf(' ', j + 2) == -1 ? strings[i]
							.length() : strings[i].indexOf(' ', j + 2);
					tempString = strings[i].substring((j + 2), endIndex)
							.toLowerCase();
					tempString = PorterStemmer.processToken(tempString)
							.toLowerCase();
					tempLiteral.getNegativeLiterals().add(tempString);
					j = endIndex + 1;
					beginIndex = j;
				}

				// checks for the presence of negative phrase queries and
				// adds them in its own list.
				else if (strings[i].charAt(j) == '-'
						&& (j <= (strings[i].length() - 3) ? strings[i]
								.charAt(j + 2) == '\"' : false)) {
					endIndex = strings[i].indexOf('\"', j + 3);
					tempString = strings[i].substring(j + 3, endIndex);
					tempLiteral.getNegativePhrases().add(tempString);
					j = endIndex + 2;
					beginIndex = j;
				}
				// this else part adds the last normal literal inside its list.
				else if ((j + 1) == strings[i].length()) {
					tempString = strings[i].substring(beginIndex, j + 1);
					if (tempString.contains("*")) {
						tempLiteral.addWildcard(tempString);
						j = j + 1;
						continue;
					}
					tempString = PorterStemmer.processToken(tempString)
							.toLowerCase();
					tempLiteral.getQueryLiterals().add(tempString);
					j = j + 1;
				} else {
					j = j + 1;
				}

			}
			if (tempLiteral != null) {
				literals.add(tempLiteral);
			}
		}
	}

	/**
	 * Loops over every Literal object and performs NOT operation on each not
	 * query literal in the sub-query.
	 * 
	 * @param index
	 * @param literals
	 * @param phraseQuery
	 * @param resultList
	 * @param resultPostings
	 * @param resultOfQueryLiteralsAND
	 */

	private void performNOTOnEachNegativeLiteralInSubQuery(
			DiskPositionalIndex index, List<SubQuery> literals,
			PhraseQuery phraseQuery, PositionalPostingFromFile[][] resultList,
			PositionalPostingFromFile[] resultPostings,
			PositionalPostingFromFile[] resultOfQueryLiteralsAND) {
		PositionalPostingFromFile[] resultPostingsForPhrase;
		// This loop will check if there are negative literals present in the
		// subquery and if it also contains some positive literals in it,
		// if it does, NOT operation is performed here and the final result of
		// the subquery is recorded in the Literal object

		for (SubQuery subQuery : literals) {
			if (subQuery.getResultPositionalPostings() != null
					&& subQuery.getResultPositionalPostings().length != 0
					&& (subQuery.getNegativeLiterals() != null
							&& !subQuery.getNegativeLiterals().isEmpty() || !subQuery
							.getNegativePhrases().isEmpty())) {
				PositionalPostingFromFile[][] positionalPostings = new PositionalPostingFromFile[subQuery
						.getNegativeLiterals() == null ? 0 : subQuery
						.getNegativeLiterals().size()][];
				if (getPositionalPostingsList(subQuery.getNegativeLiterals(),
						index) != null
						&& !subQuery.getNegativeLiterals().isEmpty()) {
					positionalPostings = getPositionalPostingsList(
							subQuery.getNegativeLiterals(), index);
				}
				resultOfQueryLiteralsAND = performAND(positionalPostings);

				// process phrase queries one by one and add the result in a
				// resultList.
				for (int i = 0; i < subQuery.getNegativePhrases().size(); i++) {
					resultPostingsForPhrase = phraseQuery.processPhraseQuery(
							subQuery.getNegativePhrases().get(i), index);
					if (resultPostingsForPhrase != null) {
						resultList[i] = resultPostingsForPhrase;
					}
				}
				if (resultList.length > 1) {
					resultPostings = performAND(resultList);
				} else if (resultList.length == 1) {
					resultPostings = resultList[0];
				}
				int insertPos = 0;
				if (positionalPostings.length == 0
						&& (resultList != null && resultList.length > 0)) {
					int arraySize1 = resultList != null ? resultList.length : 0;
					positionalPostings = new PositionalPostingFromFile[arraySize1][];

				}
				if (resultList.length > 0) {
					positionalPostings[insertPos] = resultPostings;
					insertPos++;
				}
				if (resultOfQueryLiteralsAND.length > 0) {
					positionalPostings[insertPos] = resultOfQueryLiteralsAND;
					insertPos++;
				}
				PositionalPostingFromFile[] resultOfLiteralAndPhrase = null;

				// checks the size of positionlPostings list and if the result
				// of AND operation over query literals and Phrase literals
				// returned a list of size 0.
				if (positionalPostings.length > 1
						&& (resultOfQueryLiteralsAND != null
								&& resultOfQueryLiteralsAND.length == 0 ? subQuery
								.getNegativeLiterals().isEmpty() : true)
						&& (resultPostings != null
								&& resultPostings.length == 0 ? subQuery
								.getNegativePhrases().isEmpty() : true)) {
					resultOfLiteralAndPhrase = performAND(positionalPostings);
				}

				// checks the size of positionlPostings list and if the result
				// of AND operation over query literals and Phrase literals
				// returned a list of size 0.
				else if (positionalPostings.length == 1
						&& (resultOfQueryLiteralsAND != null
								&& resultOfQueryLiteralsAND.length == 0 ? subQuery
								.getNegativeLiterals().isEmpty() : true)
						&& (resultPostings != null
								&& resultPostings.length == 0 ? subQuery
								.getNegativePhrases().isEmpty() : true)) {
					resultOfLiteralAndPhrase = positionalPostings[0];
				}
				resultPostings = operationNot(
						subQuery.getResultPositionalPostings(),
						resultOfLiteralAndPhrase);
				subQuery.setResultPositionalPostings(resultPostings);

			}
		}
	}

	/**
	 * This is one of the additional feature for our search engine, it checks
	 * the syntax of the query.
	 * 
	 * @param phrase
	 * @return
	 */
	private boolean querySyntaxChecker(String phrase) {
		int quotes = 0;
		int bracket = 0;
		int i = 0;
		boolean flag = true;
		flag = validate(phrase, '+');
		if (!flag) {
			return flag;
		}
		flag = validate(phrase, '-');
		if (!flag) {
			return flag;
		}
		flag = validate(phrase, '\"');
		if (!flag) {
			return flag;
		}

		while (i < phrase.length()) {
			if (phrase.charAt(i) == '\"') {
				quotes++;
			}
			if (phrase.charAt(i) == '(') {
				bracket++;
			}
			if (phrase.charAt(i) == ')') {
				bracket--;
			}
			if (bracket == -1) {
				System.out.println("This is an invalid query!");
				return false;
			}
			i++;
		}
		if (phrase.startsWith("+") || phrase.endsWith("+")
				|| phrase.endsWith("-")) {
			System.out.println("This is not a valid query!");
			return false;
		}
		if ((quotes % 2) != 0 || bracket != 0) {
			System.out.println("This is not a valid query!");
			return false;
		}
		return true;

	}

	/**
	 * This method is used for the purpose of query syntax checking.
	 * 
	 * @param phrase
	 * @param character
	 * @return
	 */
	private boolean validate(String phrase, Character character) {
		String subqueryInQuotes = "";
		Pattern p = Pattern.compile("\\w");
		int j = phrase.indexOf(character);
		if (j == -1) {
			return true;
		}
		while (j < phrase.length() - 1 && phrase.charAt(j + 1) != character) {
			subqueryInQuotes = subqueryInQuotes
					+ Character.toString(phrase.charAt(j + 1));
			j++;
		}
		if (subqueryInQuotes.isEmpty() || !p.matcher(subqueryInQuotes).find()) {
			System.out.println("This is not a valid query!");
			return false;
		}
		return true;
	}

	/**
	 * This method accepts two parameters: (1) Result of AND operation over
	 * phrase queries and normal query literals. (2) List of Positional postings
	 * for Negative literals which includes negative phrase queries as well.
	 * Then performs the NOT operation over these lists.
	 * 
	 * @param resultPostingsOfLiterals
	 * @param postingsOfAndOfNotLiterals
	 * @return
	 */
	private PositionalPostingFromFile[] operationNot(
			PositionalPostingFromFile[] resultPostingsOfLiterals,
			PositionalPostingFromFile[] postingsOfAndOfNotLiterals) {
		PositionalPostingFromFile[] result = new PositionalPostingFromFile[resultPostingsOfLiterals != null ? resultPostingsOfLiterals.length
				: 0];
		boolean flag = false;
		int insertPos = 0;

		if ((resultPostingsOfLiterals != null && resultPostingsOfLiterals.length != 0)
				&& (postingsOfAndOfNotLiterals != null)) {
			for (int i = 0; i < resultPostingsOfLiterals.length; i++) {
				flag = false;
				for (int j = 0; j < postingsOfAndOfNotLiterals.length; j++) {
					if (resultPostingsOfLiterals[i] != null
							&& postingsOfAndOfNotLiterals[j] != null
							&& resultPostingsOfLiterals[i].getDocID() == postingsOfAndOfNotLiterals[j]
									.getDocID()) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					result[insertPos] = resultPostingsOfLiterals[i];
					insertPos++;
				}

			}
		}
		return result;

	}

	/**
	 * This method performs the AND operation by taking two lists at a time. It
	 * proceeds by storing the intermediate result in list1 and then AND-ing it
	 * with list2.
	 * 
	 * @param positionalPostings
	 * @return
	 */
	private PositionalPostingFromFile[] performAND(
			PositionalPostingFromFile[][] positionalPostings) {
		insertPositionForAND = 0;
		PositionalPostingFromFile[] list1;
		PositionalPostingFromFile[] list2;
		PositionalPostingFromFile[] result;
		int j = 0;
		int k = 0;
		result = initializeArrayForAND(positionalPostings);
		if (positionalPostings != null) {
			for (int i = 0; i < positionalPostings.length; i++) {
				if (i == 0) {
					list1 = positionalPostings[i];
				} else {
					list1 = result;
				}
				if (i < positionalPostings.length - 1) {
					// result.clear();
					insertPositionForAND = 0;
					result = initializeArrayForAND(positionalPostings);
					list2 = positionalPostings[i + 1];
					j = 0;
					k = 0;
					if (list1 == null || list2 == null) {
						return null;
					} else {
						while (j < list1.length && k < list2.length) {
							if (list2[k] != null
									&& list1[j] != null
									&& list1[j].getDocID() == list2[k]
											.getDocID()) {
								result[insertPositionForAND] = list2[k];
								insertPositionForAND++;
								j++;
								k++;
							} else if (list2[k] != null
									&& list1[j] != null
									&& list1[j].getDocID() < list2[k]
											.getDocID()) {
								j++;
							} else if (list2[k] != null
									&& list1[j] != null
									&& list1[j].getDocID() > list2[k]
											.getDocID()) {
								k++;
							} else {
								break;
							}
						}
					}

				}
				if (positionalPostings.length == 1) {
					return positionalPostings[0];
				}
			}
		}
		return result;
	}

	public static PositionalPostingFromFile[] initializeArrayForAND(
			PositionalPostingFromFile[][] positionalPostings) {
		int length = 0;
		if (positionalPostings != null && positionalPostings.length != 0) {
			for (int i = 0; i < positionalPostings.length; i++) {
				if (positionalPostings[i] != null) {
					if (positionalPostings[i].length > length) {
						length = positionalPostings[i].length;
					}
				}
			}
		}
		PositionalPostingFromFile[] result = new PositionalPostingFromFile[length];
		return result;
	}

	/**
	 * This method takes a list of positional positing list as parameter and
	 * performs an OR operation on them taking two positional postings list at a
	 * time, and returns the result.
	 * 
	 * @param positionalPostings
	 * @return
	 */
	private PositionalPostingFromFile[] operationOR(
			PositionalPostingFromFile[][] positionalPostings) {
		insertPositionForOR = 0;
		PositionalPostingFromFile[] list1;
		PositionalPostingFromFile[] list2;
		PositionalPostingFromFile[] result = null;
		result = initializeResultArray(positionalPostings);
		int j = 0;
		int k = 0;
		for (int i = 0; i < positionalPostings.length - 1; i++) {
			list1 = positionalPostings[i];
			list2 = positionalPostings[i + 1];
			j = 0;
			k = 0;
			while (j < list1.length || k < list2.length) {
				if (j < list1.length && k < list2.length) {
					if (list1[j] != null && list2[k] != null
							&& list1[j].getDocID() == list2[k].getDocID()) {
						result = addToResult(result, list1[j]);
						j++;
						k++;
					} else if (list1[j] != null && list2[k] != null
							&& list1[j].getDocID() < list2[k].getDocID()) {
						result = addToResult(result, list1[j]);
						j++;
					} else if (list1[j] != null && list2[k] != null
							&& list1[j].getDocID() > list2[k].getDocID()) {
						result = addToResult(result, list2[k]);
						k++;
					} else {
						break;
					}
				} else if (j < list1.length) {
					result = addToResult(result, list1[j]);
					j++;
				} else if (k < list2.length) {
					result = addToResult(result, list2[k]);
					k++;
				}
			}

		}
		if (positionalPostings.length == 1) {
			return positionalPostings[0];
		}

		return result;
	}

	private PositionalPostingFromFile[] initializeResultArray(
			PositionalPostingFromFile[][] positionalPostings) {
		int length = 0;
		for (int i = 0; i < positionalPostings.length; i++) {
			if (positionalPostings[i] != null) {
				length += positionalPostings[i].length;
			}
		}
		PositionalPostingFromFile[] result = new PositionalPostingFromFile[length];
		return result;

	}

	/**
	 * Adds the PositionalPosting object in the result list for OR operation.
	 * 
	 * @param result
	 * @param doc
	 * @return
	 */
	private PositionalPostingFromFile[] addToResult(
			PositionalPostingFromFile[] result, PositionalPostingFromFile doc) {
		PositionalPostingFromFile postingObject;
		boolean flag = false;
		for (int i = 0; i < result.length; i++) {
			if (result[i] != null && doc != null
					&& (result[i].getDocID() == doc.getDocID())) {
				flag = true;
			}
		}
		if (!flag) {
			postingObject = doc;
			result[insertPositionForOR] = postingObject;
			insertPositionForOR++;
		}
		return result;
	}

	/**
	 * This method takes a list of terms and positional index as parameter and
	 * returns their positional postings as a list of positional positings
	 * lists.
	 * 
	 * @param words
	 * @param index
	 * @return
	 */
	private PositionalPostingFromFile[][] getPositionalPostingsList(
			List<String> words, DiskPositionalIndex index) {

		PositionalPostingFromFile[][] positionalPostings = new PositionalPostingFromFile[words
				.size()][];
		for (int i = 0; i < words.size(); i++) {
			if (index.GetPostingsWithPositions(words.get(i)) != null) {
				positionalPostings[i] = index.GetPostingsWithPositions(words
						.get(i));
			} else {
				return null;
			}
		}
		return positionalPostings;
	}
}
