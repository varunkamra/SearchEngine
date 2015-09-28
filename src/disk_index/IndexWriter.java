package disk_index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes an inverted indexing of a directory to disk.
 */
public class IndexWriter {
	private HashMap<Integer, Float> documentWDscores = new HashMap<Integer, Float>();;
	private String mFolderPath;
	KGramIndex kgram;

	/**
	 * Constructs an IndexWriter object which is prepared to index the given
	 * folder.
	 */
	public IndexWriter(String folderPath) {
		mFolderPath = folderPath;
	}

	/**
	 * Builds and writes an inverted index to disk. Creates three files:
	 * vocab.bin, containing the vocabulary of the corpus; postings.bin,
	 * containing the postings list of document IDs; vocabTable.bin, containing
	 * a table that maps vocab terms to postings locations
	 */
	public void buildIndex() {
		kgram = new KGramIndex(mFolderPath);
		buildIndexForDirectory(mFolderPath);

	}

	/**
	 * Builds the normal NaiveInvertedIndex for the folder.
	 */
	private void buildIndexForDirectory(String folder) {
		PositionalInvertedIndex index = new PositionalInvertedIndex();
		// Index the directory using a naive index
		indexFiles(folder, index, kgram);
		kgram.saveIndexToDisk(folder);

		// at this point, "index" contains the in-memory inverted index
		// now we save the index to disk, building three files: the postings
		// index,
		// the vocabulary list, and the vocabulary table.

		// the array of terms
		String[] dictionary = index.getDictionary();
		// an array of positions in the vocabulary file
		long[] vocabPositions = new long[dictionary.length];

		buildVocabFile(folder, dictionary, vocabPositions);
		buildPostingsFile(folder, index, dictionary, vocabPositions);
		buildDocWeightsFile(folder);
	}

	private void buildDocWeightsFile(String folder) {
		try {
			FileOutputStream docWeights = new FileOutputStream(new File(folder,
					"docWeights.bin"));
			byte[] scoreWD;
			float average = 0;
			byte[] documentID;
			for (Entry<Integer, Float> row : documentWDscores.entrySet()) {
				documentID = ByteBuffer.allocate(4).putInt(row.getKey())
						.array();
				scoreWD = ByteBuffer.allocate(4).putFloat(row.getValue())
						.array();
				average = average + row.getValue();
				if (row.getKey().equals(documentWDscores.size() - 1)) {
					average = average / documentWDscores.size();
				}
				try {
					docWeights.write(documentID, 0, documentID.length);
					docWeights.write(scoreWD, 0, scoreWD.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			documentID = ByteBuffer.allocate(4).putInt(-1).array();
			scoreWD = ByteBuffer.allocate(4).putFloat(average).array();
			try {
				docWeights.write(documentID, 0, documentID.length);
				docWeights.write(scoreWD, 0, scoreWD.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Builds the postings.bin file for the indexed directory, using the given
	 * NaiveInvertedIndex of that directory.
	 */
	private static void buildPostingsFile(String folder,
			PositionalInvertedIndex index, String[] dictionary,
			long[] vocabPositions) {
		FileOutputStream postingsFile = null;
		try {
			postingsFile = new FileOutputStream(
					new File(folder, "postings.bin"));

			// simultaneously build the vocabulary table on disk, mapping a term
			// index to a
			// file location in the postings file.
			FileOutputStream vocabTable = new FileOutputStream(new File(folder,
					"vocabTable.bin"));

			// the first thing we must write to the vocabTable file is the
			// number of vocab terms.
			byte[] tSize = ByteBuffer.allocate(4).putInt(dictionary.length)
					.array();
			vocabTable.write(tSize, 0, tSize.length);
			int vocabI = 0;
			for (String s : dictionary) {

				// for each String in dictionary, retrieve its postings.
				List<PositionalPosting> postings = index.getPostings(s);
				// write the vocab table entry for this term: the byte location
				// of the term in the vocab list file,
				// and the byte location of the postings for the term in the
				// postings file.
				byte[] vPositionBytes = ByteBuffer.allocate(8)
						.putLong(vocabPositions[vocabI]).array();
				vocabTable.write(vPositionBytes, 0, vPositionBytes.length);

				byte[] pPositionBytes = ByteBuffer.allocate(8)
						.putLong(postingsFile.getChannel().position()).array();
				vocabTable.write(pPositionBytes, 0, pPositionBytes.length);

				// write the postings file for this term. first, the document
				// frequency for the term, then
				// the document IDs, encoded as gaps.
				byte[] docFreqBytes = ByteBuffer.allocate(4)
						.putInt(postings.size()).array();
				postingsFile.write(docFreqBytes, 0, docFreqBytes.length);
				// encode(postingsFile, postings.size());
				int lastDocId = 0;
				for (PositionalPosting posting : postings) {
					int lastPos = 0;
					byte[] docIdBytes = ByteBuffer.allocate(4)
							.putInt(posting.getDocID() - lastDocId).array();
					// encode a gap, not a doc ID
					byte[] docPostingsFrequency = ByteBuffer
							.allocate(4)
							.putInt(posting.getPositions() == null ? 0
									: posting.getPositions().size()).array();
					postingsFile.write(docIdBytes, 0, docIdBytes.length);
					// encode(postingsFile, posting.getDocID() - lastDocId);
					postingsFile.write(docPostingsFrequency, 0,
							docPostingsFrequency.length);
					// encode(postingsFile, posting.getPositions() == null ? 0
					// : posting.getPositions().size());
					if (posting.getPositions() != null) {
						for (Integer position : posting.getPositions()) {
							byte[] termPosition = ByteBuffer.allocate(4)
									.putInt(position - lastPos).array();
							postingsFile.write(termPosition, 0,
									termPosition.length);
							// encode(postingsFile, position - lastPos);
							lastPos = position;
						}
					}

					lastDocId = posting.getDocID();
				}

				vocabI++;
			}
			vocabTable.close();
			postingsFile.close();
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		} finally {
			try {
				postingsFile.close();
			} catch (IOException ex) {
			}
		}
	}

	private static void buildVocabFile(String folder, String[] dictionary,
			long[] vocabPositions) {
		OutputStreamWriter vocabList = null;
		try {
			// first build the vocabulary list: a file of each vocab word
			// concatenated together.
			// also build an array associating each term with its byte location
			// in this file.
			int vocabI = 0;
			vocabList = new OutputStreamWriter(new FileOutputStream(new File(
					folder, "vocab.bin")), "ASCII");

			int vocabPos = 0;
			for (String vocabWord : dictionary) {
				// for each String in dictionary, save the byte position where
				// that term will start in the vocab file.
				vocabPositions[vocabI] = vocabPos;
				vocabList.write(vocabWord); // then write the String
				vocabI++;
				vocabPos += vocabWord.length();
			}
		} catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		} catch (UnsupportedEncodingException ex) {
			System.out.println(ex.toString());
		} catch (IOException ex) {
			System.out.println(ex.toString());
		} finally {
			try {
				vocabList.close();
			} catch (IOException ex) {
				System.out.println(ex.toString());
			}
		}
	}

	private void indexFiles(String folder, final PositionalInvertedIndex index,
			final KGramIndex kgram) {
		int documentID = 0;
		final Path currentWorkingPath = Paths.get(folder).toAbsolutePath();

		try {
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
								// we have found a .txt file; add its name to
								// the fileName list,
								// then index the file and increase the document
								// ID counter.
								// System.out.println("Indexing file " +
								// file.getFileName());

								indexFile(file.toFile(), index, kgram,
										mDocumentID);
								mDocumentID++;
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
		} catch (IOException ex) {
			Logger.getLogger(IndexWriter.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	private void indexFile(File fileName, PositionalInvertedIndex index,
			KGramIndex kgram, int documentID) {
		try {
			HashMap<String, List<Integer>> positions = getPositionsFromFile(
					fileName, documentID);
			SimpleTokenStream stream = new SimpleTokenStream(fileName);
			while (stream.hasNextToken()) {
				String term = stream.nextToken();
				if (term != null) {
					kgram.buildKGrams(term);
					String stemmed = PorterStemmer.processToken(term);

					if (stemmed != null && stemmed.length() > 0) {
						index.addTerm(stemmed, documentID, positions.get(term));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * This method notes the positions of all the tokens per document in a
	 * hashmap and returns it.
	 * 
	 * @param fileName
	 * @param documentID
	 * @return
	 * @throws FileNotFoundException
	 */
	private HashMap<String, List<Integer>> getPositionsFromFile(File fileName,
			int documentID) throws FileNotFoundException {
		HashMap<String, List<Integer>> positions = new HashMap<String, List<Integer>>();
		FileReader fileReader = new FileReader(fileName);
		int pos = 0;
		Scanner posReader = new Scanner(fileReader);
		String next = null;
		float tfd;
		float wdTotal = 0;
		while (posReader.hasNext()) {
			next = posReader.next().replaceAll("\\W", "").toLowerCase();
			if (positions.containsKey(next)) {
				if (!positions.get(next).contains(pos)) {
					positions.get(next).add(pos);
				}
			} else {
				List<Integer> position = new ArrayList<Integer>();
				position.add(pos);
				positions.put(next, position);
			}
			pos++;
		}
		for (Entry<String, List<Integer>> e : positions.entrySet()) {
			if (e.getValue() != null) {
				tfd = e.getValue().size();
				float wdt = (float) (1 + Math.log(tfd));
				wdTotal += (float) (Math.pow(wdt, 2));
			}
		}
		float finalWdScore = (float) Math.sqrt(wdTotal);
		documentWDscores.put(documentID, finalWdScore);
		return positions;

	}

	private static void encode(FileOutputStream postingsFile, int value)
			throws IOException {
		boolean flag = true;
		int singleByte;
		LinkedList<Integer> byteStream = new LinkedList<Integer>();
		if (value == 0) {
			singleByte = value & 0x80;
			postingsFile.write(singleByte);
		}
		int i = 0;
		while (value != 0) {
			if (i > 5) {
				break;
			} else {
				if (flag) {
					singleByte = value & 0x7F;
					singleByte = singleByte | 0x80;
					flag = false;
				} else {
					singleByte = value & 0x7F;
				}
				byteStream.add(singleByte);
				value = value >> 7;
				i++;
			}
		}
		for (int j = i - 1; j >= 0; j--) {
			postingsFile.write(byteStream.get(j).byteValue());
		}
	}

}
