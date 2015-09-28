package disk_index;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is instantiated for each subquery generated as a result of
 * splitting the query over + sign of OR.
 * 
 */
public class SubQuery {
	private List<String> queryLiterals;
	private List<String> phraseLiterals;
	private List<String> negativeLiterals;
	private PositionalPostingFromFile[] resultPositionalPostings;
	private List<String> negativePhrases;
	private List<String> wildcards;

	public SubQuery() {
		queryLiterals = new ArrayList<String>();
		phraseLiterals = new ArrayList<String>();
		negativeLiterals = new ArrayList<String>();
		negativePhrases = new ArrayList<String>();
		wildcards = new ArrayList<String>();
	}

	public List<String> getQueryLiterals() {
		return queryLiterals;
	}

	public List<String> getPhraseLiterals() {
		return phraseLiterals;
	}

	public List<String> getNegativeLiterals() {
		return negativeLiterals;
	}

	public PositionalPostingFromFile[] getResultPositionalPostings() {
		return resultPositionalPostings;
	}

	public List<String> getNegativePhrases() {
		return negativePhrases;
	}

	public List<String> getWildcards() {
		return wildcards;
	}

	public void addWildcard(String wildcards) {
		this.wildcards.add(wildcards);
	}

	public void addWildcard(List<String> wildcards) {
		this.wildcards.addAll(wildcards);
	}

	public void setResultPositionalPostings(
			PositionalPostingFromFile[] resultPositionalPostings) {
		this.resultPositionalPostings = resultPositionalPostings;
	}
}
