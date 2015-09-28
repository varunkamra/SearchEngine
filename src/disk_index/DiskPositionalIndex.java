package disk_index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiskPositionalIndex {

	public static String mPath;
	private RandomAccessFile mVocabList;
	private RandomAccessFile mPostings;
	private static RandomAccessFile mDocWeights;
	private long[] mVocabTable;
	private List<String> mFileNames;

	public DiskPositionalIndex(String path) {
		try {
			mPath = path;
			mVocabList = new RandomAccessFile(new File(path, "vocab.bin"), "r");
			mPostings = new RandomAccessFile(new File(path, "postings.bin"),
					"r");
			mVocabTable = readVocabTable(path);
			mFileNames = readFileNames(path);
		} catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		}
	}

	public Accumulator[] getDocumentWeights() {
		return readDocumentWeights();
	}

	private static Accumulator[] readDocumentWeights() {
		try {
			mDocWeights = new RandomAccessFile(
					new File(mPath, "docWeights.bin"), "r");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] buffer = new byte[4];
		Accumulator[] docWeights = null;
		try {
			float weight;
			int documentId;
			Accumulator temp;
			int size = (int) mDocWeights.length() / 8;
			docWeights = new Accumulator[size];
			for (int i = 0; i < size; i++) {
				temp = new Accumulator();
				mDocWeights.read(buffer, 0, buffer.length);
				temp.setDocumentId(ByteBuffer.wrap(buffer).getInt());
				mDocWeights.read(buffer, 0, buffer.length);
				temp.setDocumentScore(ByteBuffer.wrap(buffer).getFloat());
				docWeights[i] = temp;

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return docWeights;
	}

	private static int[] readPostingsFromFile(RandomAccessFile postings,
			long postingsPosition) {
		try {
			// seek to the position in the file where the postings start.
			postings.seek(postingsPosition);

			// read the 4 bytes for the document frequency
			byte[] buffer = new byte[4];
			postings.read(buffer, 0, buffer.length);

			// use ByteBuffer to convert the 4 bytes into an int.
			int documentFrequency = ByteBuffer.wrap(buffer).getInt();

			// initialize the array that will hold the postings.
			int[] docIds = new int[documentFrequency];

			// read 4 bytes at a time from the file, until you have read as many
			// postings as the document frequency promised.
			//
			// after each read, convert the bytes to an int posting. this value
			// is the GAP since the last posting. decode the document ID from
			// the gap and put it in the array.
			//
			// repeat until all postings are read.
			int gap;
			int bytesToSkip = 0;
			for (int i = 0; i < documentFrequency; i++) {
				postings.read(buffer, 0, buffer.length);
				if (i == 0) {
					docIds[i] = ByteBuffer.wrap(buffer).getInt();
				} else {
					gap = ByteBuffer.wrap(buffer).getInt();
					docIds[i] = docIds[i - 1] + gap;
				}
				postings.read(buffer, 0, buffer.length);
				bytesToSkip = 4 * ByteBuffer.wrap(buffer).getInt();
				postings.skipBytes(bytesToSkip);

			}

			return docIds;
		} catch (IOException ex) {
			System.out.println(ex.toString());
		}
		return null;
	}

	private static PositionalPostingFromFile[] readPostingsWithPositionsFromFile(
			RandomAccessFile postings, long postingsPosition) {
		try {
			// seek to the position in the file where the postings start.
			postings.seek(postingsPosition);

			// read the 4 bytes for the document frequency
			byte[] buffer = new byte[4];
			postings.read(buffer, 0, buffer.length);

			// use ByteBuffer to convert the 4 bytes into an int.
			int documentFrequency = ByteBuffer.wrap(buffer).getInt();
			// decode(postings);

			PositionalPostingFromFile[] postingsFromDisk = new PositionalPostingFromFile[documentFrequency];
			// read 4 bytes at a time from the file, until you have read as many
			// postings as the document frequency promised.
			//
			// after each read, convert the bytes to an int posting. this value
			// is the GAP since the last posting. decode the document ID from
			// the gap and put it in the array.
			//
			// repeat until all postings are read.
			int docId;
			int gap;
			int gapPos;
			PositionalPostingFromFile posting;
			for (int i = 0; i < documentFrequency; i++) {
				posting = new PositionalPostingFromFile();
				postings.read(buffer, 0, buffer.length);
				if (i == 0) {
					docId = ByteBuffer.wrap(buffer).getInt();
					// decode(postings);
					posting.setDocID(docId);
				} else {
					gap = ByteBuffer.wrap(buffer).getInt();
					// decode(postings);
					docId = postingsFromDisk[i - 1].getDocID() + gap;
					posting.setDocID(docId);
				}
				postings.read(buffer, 0, buffer.length);
				int termFrequency = ByteBuffer.wrap(buffer).getInt();
				// decode(postings);
				int[] positions = new int[termFrequency];
				int position;
				for (int j = 0; j < termFrequency; j++) {
					postings.read(buffer, 0, buffer.length);

					if (j == 0) {
						position = ByteBuffer.wrap(buffer).getInt();
						positions[j] = position;
					} else {
						gapPos = ByteBuffer.wrap(buffer).getInt();
						position = positions[j - 1] + gapPos;
						positions[j] = position;
					}
				}
				posting.addPositions(positions);
				postingsFromDisk[i] = posting;

			}

			return postingsFromDisk;
		} catch (IOException ex) {
			System.out.println(ex.toString());
		}
		return null;
	}

	public int[] GetPostings(String term) {
		long postingsPosition = BinarySearchVocabulary(term);
		if (postingsPosition >= 0) {
			return readPostingsFromFile(mPostings, postingsPosition);
		}
		return null;
	}

	public PositionalPostingFromFile[] GetPostingsWithPositions(String term) {
		long postingsPosition = BinarySearchVocabulary(term);
		if (postingsPosition >= 0) {
			return readPostingsWithPositionsFromFile(mPostings,
					postingsPosition);
		}
		return null;
	}

	private long BinarySearchVocabulary(String term) {
		// do a binary search over the vocabulary, using the vocabTable and the
		// file vocabList.
		int i = 0, j = mVocabTable.length / 2 - 1;
		while (i <= j) {
			try {
				int m = (i + j) / 2;
				long vListPosition = mVocabTable[m * 2];
				int termLength = (int) (mVocabTable[(m + 1) * 2] - vListPosition);
				mVocabList.seek(vListPosition);

				byte[] buffer = new byte[termLength];
				mVocabList.read(buffer, 0, termLength);
				String fileTerm = new String(buffer, "ASCII");

				int compareValue = term.compareTo(fileTerm);
				if (compareValue == 0) {
					// found it!
					return mVocabTable[m * 2 + 1];
				} else if (compareValue < 0) {
					j = m - 1;
				} else {
					i = m + 1;
				}
			} catch (IOException ex) {
				System.out.println(ex.toString());
			}
		}
		return -1;
	}

	public HashMap<String, Integer> getDictionary() {
		HashMap<String, Integer> dictionaryWithPositions = new HashMap<String, Integer>();
		String dictionary[] = new String[mVocabTable.length / 2 - 1];
		int termLength;
		for (int i = 0; i < mVocabTable.length; i++) {
			if ((i + 1) * 2 < mVocabTable.length && i * 2 < mVocabTable.length) {
				try {
					termLength = (int) (mVocabTable[(i + 1) * 2] - mVocabTable[i * 2]);
					mVocabList.seek(mVocabTable[i * 2]);

					byte[] buffer = new byte[termLength];
					mVocabList.read(buffer, 0, termLength);
					String fileTerm = new String(buffer, "ASCII");
					dictionaryWithPositions.put(fileTerm, i);
					dictionary[i] = fileTerm;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return dictionaryWithPositions;
	}

	private static List<String> readFileNames(String indexName) {
		try {
			final List<String> names = new ArrayList<String>();
			final Path currentWorkingPath = Paths.get(indexName)
					.toAbsolutePath();

			Files.walkFileTree(currentWorkingPath,
					new SimpleFileVisitor<Path>() {
						int mDocumentID = 0;

						public FileVisitResult preVisitDirectory(Path dir,
								BasicFileAttributes attrs) {
							// make sure we only process the current working
							// directory
							if (currentWorkingPath.equals(dir)) {
								return FileVisitResult.CONTINUE;
							}
							return FileVisitResult.SKIP_SUBTREE;
						}

						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs) {
							// only process .txt files
							if (file.toString().endsWith(".txt")) {
								names.add(file.toFile().getName());
							}
							return FileVisitResult.CONTINUE;
						}

						// don't throw exceptions if files are locked/other
						// errors occur
						public FileVisitResult visitFileFailed(Path file,
								IOException e) {

							return FileVisitResult.CONTINUE;
						}

					});
			return names;
		} catch (IOException ex) {
			System.out.println(ex.toString());
		}
		return null;
	}

	private static long[] readVocabTable(String indexName) {
		try {
			long[] vocabTable;

			RandomAccessFile tableFile = new RandomAccessFile(new File(
					indexName, "vocabTable.bin"), "r");

			byte[] byteBuffer = new byte[4];
			tableFile.read(byteBuffer, 0, byteBuffer.length);

			int tableIndex = 0;
			vocabTable = new long[ByteBuffer.wrap(byteBuffer).getInt() * 2];
			byteBuffer = new byte[8];

			while (tableFile.read(byteBuffer, 0, byteBuffer.length) > 0) { // while
																			// we
																			// keep
																			// reading
																			// 4
																			// bytes
				vocabTable[tableIndex] = ByteBuffer.wrap(byteBuffer).getLong();
				tableIndex++;
			}
			tableFile.close();
			return vocabTable;
		} catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		} catch (IOException ex) {
			System.out.println(ex.toString());
		}
		return null;
	}

	private static int decode(RandomAccessFile postingsFile) throws IOException {
		byte[] buffer = new byte[1];
		postingsFile.read(buffer, 0, buffer.length);

		int value = ByteBuffer.wrap(buffer).get() & 0xff;
		int numberRead = 0;
		while (value < 128) {
			numberRead += value;
			postingsFile.read(buffer, 0, buffer.length);
			value = ByteBuffer.wrap(buffer).get() & 0xff;
		}
		numberRead += value - 128;
		return numberRead;
	}

	public List<String> getFileNames() {
		return mFileNames;
	}

	public int getTermCount() {
		return mVocabTable.length / 2;
	}
}
