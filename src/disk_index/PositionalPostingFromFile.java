package disk_index;

/**
 * This class binds a documentID with its corresponding positions as an array of
 * integers.
 * 
 */

public class PositionalPostingFromFile {
	private int docID;
	private int[] positions;

	public PositionalPostingFromFile() {
		setDocID(0);
	}

	public int[] getPositions() {
		return positions;
	}

	public void addPositions(int[] positions) {
		this.positions = positions;
	}

	public int getDocID() {
		return docID;
	}

	public void setDocID(int docID) {
		this.docID = docID;
	}
}
