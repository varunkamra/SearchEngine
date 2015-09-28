package disk_index;

public class Similarity {
	private int documentId1;
	private int documentId2;
	private float SimilarityScore;

	public int getDocumentId1() {
		return documentId1;
	}

	public void setDocumentId1(int documentId1) {
		this.documentId1 = documentId1;
	}

	public int getDocumentId2() {
		return documentId2;
	}

	public void setDocumentId2(int documentId2) {
		this.documentId2 = documentId2;
	}

	public float getSimilarityScore() {
		return SimilarityScore;
	}

	public void setSimilarityScore(float similarityScore) {
		SimilarityScore = similarityScore;
	}
}
