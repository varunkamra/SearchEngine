package disk_index;

/**
 * This class wraps a document id with a document score. It is used for the
 * purpose of sorting the ranked retrieval results.
 * 
 **/

public class Accumulator {
	private int documentId;
	private float documentScore;

	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	public float getDocumentScore() {
		return documentScore;
	}

	public void setDocumentScore(float documentScore) {
		this.documentScore = documentScore;
	}

}
