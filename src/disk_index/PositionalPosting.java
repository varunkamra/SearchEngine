package disk_index;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used for creating individual pairs of document ids and
 * positions.
 *
 */
public class PositionalPosting {

	private int docID;
	private List<Integer> positions;

	public PositionalPosting() {
		setDocID(0);
		setPositions(new ArrayList<Integer>());
	}

	public List<Integer> getPositions() {
		return positions;
	}

	public void setPositions(List<Integer> positions) {
		this.positions = positions;
	}

	public void addPositions(List<Integer> position) {
		positions.addAll(position);
	}

	public int getDocID() {
		return docID;
	}

	public void setDocID(int docID) {
		this.docID = docID;
	}
}
