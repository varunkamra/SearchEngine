package disk_index;

import java.util.regex.Pattern;

public class PorterStemmer {

	// a single consonant
	private static final String c = "[^aeiou]";
	// a single vowel
	private static final String v = "[aeiouy]";

	// a sequence of consonants; the second/third/etc consonant cannot be 'y'
	private static final String C = c + "[^aeiouy]*";
	// a sequence of vowels; the second/third/etc cannot be 'y'
	private static final String V = v + "[aeiou]*";

	// this regex pattern tests if the token has measure > 0 [at least one VC].
	private static final Pattern mGr0 = Pattern
			.compile("^(" + C + ")?" + V + C);

	// add more Pattern variables for the following patterns:
	// m equals 1: token has measure == 1
	private static final Pattern meq1 = Pattern.compile("^(" + C + ")" + V + C
			+ "(" + V + ")?$");

	// m greater than 1: token has measure > 1
	private static final Pattern mGr1 = Pattern.compile(mGr0 + V + C);

	// vowel: token has a vowel after the first (optional) C
	private static final Pattern hasVowel = Pattern
			.compile("^(" + C + ")?" + V);

	// double consonant: token ends in two consonants that are the same,
	// unless they are L, S, or Z.
	private static final Pattern doubleCC = Pattern
			.compile("([^aeiouylsz])\\1$");

	private static final Pattern cvc = Pattern.compile(c + v + "[^aeiouywxy]"
			+ "$");

	public static String processToken(String token) {
		if (token.length() < 3) {
			return token; // token must be at least 3 chars
		}
		// step 1a
		// 1.
		if (token.endsWith("sses")) {
			token = token.substring(0, token.length() - 2);
		}
		// 2.
		else if (token.endsWith("ies")) {
			token = token.substring(0, token.length() - 2);
		}
		// 3.
		else if (!token.endsWith("ss") && token.endsWith("s")) {
			token = token.substring(0, token.length() - 1);
		}

		// step 1b
		boolean doStep1bb = false;
		String stem = null;

		if (token.endsWith("eed")) { // 1b.1
			stem = token.substring(0, token.length() - 3);
			if (mGr0.matcher(stem).find()) { // if the pattern find the
												// stem
				token = stem + "ee";
			}

		} else if (token.endsWith("ed")) { // 1b.2
			stem = token.substring(0, token.length() - 2);
			if (hasVowel.matcher(stem).find()) {
				token = stem;
				doStep1bb = true;
			}
		} else if (token.endsWith("ing")) { // 1b.3
			stem = token.substring(0, token.length() - 3);
			if (hasVowel.matcher(stem).find()) {
				token = stem;
				doStep1bb = true;
			}
		}

		// step 1b*, only if the 1b.2 or 1b.3 were performed.
		if (doStep1bb) {
			if (token.endsWith("at") || token.endsWith("bl")
					|| token.endsWith("iz")) { // 1b* 3,4 & 5

				token = token + "e";
			} else if (doubleCC.matcher(token).find()) { // 1b*.4
				token = token.substring(0, token.length() - 1);
			} else if (meq1.matcher(token).find() && cvc.matcher(token).find()) {// 1b*.5
				token = token + "e";
			}
		}

		// step 1c
		if (token.endsWith("y")) {
			if (hasVowel.matcher(token).find()) {
				token = token.substring(0, token.length() - 1);
				token = token + "i";
			}
		}

		// step 2
		String[][] step2pairs = new String[][] { { "ational", "ate" },
				{ "tional", "tion" }, { "enci", "ence" }, { "anci", "ance" },
				{ "izer", "ize" }, { "bli", "ble" }, { "alli", "al" },
				{ "entli", "ent" }, { "eli", "e" }, { "ousli", "ous" },
				{ "ization", "ize" }, { "ation", "ate" }, { "ator", "ate" },
				{ "alism", "al" }, { "iveness", "ive" }, { "fulness", "ful" },
				{ "ousness", "ous" }, { "aliti", "al" }, { "aviti", "ive" },
				{ "biliti", "ble" } };

		token = stemmer(token, step2pairs);

		// step 3
		String[][] step3pairs = new String[][] { { "icate", "ic" },
				{ "ative", "" }, { "alize", "al" }, { "iciti", "ic" },
				{ "ical", "ic" }, { "ful", "" }, { "ness", "" } };

		token = stemmer(token, step3pairs);

		// step 4
		String[] step4 = new String[] { "al", "ance", "ence", "er", "ic",
				"able", "ible", "ant", "ement", "ment", "ent", "ou", "ism",
				"ate", "iti", "ous", "ive", "ize" };
		boolean flag = false;

		if (token.endsWith("ion")) {
			stem = token.substring(0, token.length() - 3);
			if (mGr1.matcher(stem).find()
					&& (stem.endsWith("s") || stem.endsWith("t"))) {
				token = stem;
			}
		} else {
			for (int i = 0; i < step4.length; i++) {
				if (token.endsWith(step4[i])) {
					flag = true;
					stem = token.substring(0,
							token.length() - step4[i].length());
					if (mGr1.matcher(stem).find()) {
						token = stem;
					}
				}
				if (flag) {
					break;
				}
			}
		}

		// step 5
		if (token.endsWith("e")) {
			stem = token.substring(0, token.length() - 1);
			if (mGr1.matcher(stem).find()
					|| (meq1.matcher(stem).find() && !cvc.matcher(stem).find())) {
				token = stem;
			}
		} else if (token.endsWith("ll")) {
			stem = token.substring(0, token.length() - 2);
			if (mGr1.matcher(stem).find()) {
				token = stem + "l";
			}
		}
		return token;
	}

	private static String stemmer(String token, String[][] pairs) {
		boolean flag = false;
		for (int i = 0; i < pairs.length; i++) {
			if (token.endsWith(pairs[i][0])) {
				flag = true;
				String stem = token.substring(0,
						token.length() - pairs[i][0].length());
				if (mGr0.matcher(stem).find()) {
					token = stem + pairs[i][1];
				}
			}
			if (flag) {
				break;
			}
		}
		return token;
	}
}
