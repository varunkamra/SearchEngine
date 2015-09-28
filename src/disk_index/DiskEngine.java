package disk_index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class DiskEngine {

	public static void main(String[] args) {
		final KGramIndex kgram;
		Scanner scan = new Scanner(System.in);
		System.out.println("Menu:");
		System.out.println("1) Build index");
		System.out.println("2) Boolean query retrieval mode");
		System.out.println("3) Ranked query retrieval mode");
		// For Cosine similarity give the name of the directory contaning all 85
		// text files.
		System.out.println("4) Cosine Similarity");
		// For bayesian classification give ALL as the name of the directory.
		System.out.println("5) Bayesian Classification");
		System.out.println("Choose a selection:");
		int menuChoice = scan.nextInt();
		scan.nextLine();

		switch (menuChoice) {
		case 1:
			System.out.println("Enter the name of a directory to index: ");
			String folder = scan.nextLine();

			IndexWriter writer = new IndexWriter(folder);
			writer.buildIndex();
			break;

		case 2:
			System.out.println("Enter the name of an index to read:");
			String indexName = scan.nextLine();

			DiskPositionalIndex index = new DiskPositionalIndex(indexName);
			QueryLanguage queryLanguage = new QueryLanguage();

			kgram = new KGramIndex(indexName);

			while (true) {
				System.out.println();
				System.out.println("Enter one or more search terms, separated "
						+ "by spaces:");
				String input = scan.nextLine();
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss.SSS");
				Date date = new Date();
				System.out.println(sdf.format(date));
				if (input.equals("EXIT")) {
					break;
				}

				PositionalPostingFromFile[] positionalPosting = queryLanguage
						.processQuery(input, index, kgram);

				date = new Date();
				System.out.println(sdf.format(date));
				if (positionalPosting == null) {
					System.out.println("Term not found");
				} else {
					int total = 0;
					for (int i = 0; i < positionalPosting.length; i++) {
						if (positionalPosting[i] != null) {
							total++;
						}
					}
					List<String> resultFileNames = new ArrayList<String>();
					System.out.print(total + " documents matched overall!!");
					for (PositionalPostingFromFile posting : positionalPosting) {
						if (posting != null) {
							System.out.print(index.getFileNames().get(
									posting.getDocID())
									+ " ");
							resultFileNames.add(index.getFileNames().get(
									posting.getDocID()));
						}
					}
					Path currentWorkingPath = Paths.get("").resolve(indexName)
							.toAbsolutePath();
					try {
						printFileContent(currentWorkingPath, resultFileNames);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

			break;
		case 3:
			System.out.println("Enter the name of an index to read:");
			String indexDirectory = scan.nextLine();

			DiskPositionalIndex diskIndex = new DiskPositionalIndex(
					indexDirectory);
			RankedRetrievalEngine rankedRetrieval = new RankedRetrievalEngine();
			Scanner retrievalMode = new Scanner(System.in);
			int choice = 0;
			while (!(choice > 4)) {
				System.out.println();
				System.out.println("Ranked retrieval menu:");
				System.out.println("1) Default Retrieval");
				System.out.println("2) Traditional Retrieval");
				System.out.println("3) Okapi Retrieval");
				System.out.println("4) Wacky Retrieval");
				System.out.println("Choose a selection:");
				choice = retrievalMode.nextInt();
				switch (choice) {
				case 1:
					rankedRetrieval.setRetrievalMode(new DefaultRetrieval());
					break;
				case 2:
					rankedRetrieval
							.setRetrievalMode(new TraditionalRetrieval());
					break;
				case 3:
					rankedRetrieval.setRetrievalMode(new OkapiRetrieval());
					break;
				case 4:
					rankedRetrieval.setRetrievalMode(new WackyRetrieval());
					break;
				}
				if (!(choice > 4)) {
					System.out
							.println("Enter one or more search terms, separated "
									+ "by spaces:");
					String input = scan.nextLine();

					if (input.equals("EXIT")) {
						break;
					}
					rankedRetrieval.printResult(input, diskIndex);
				}

			}
			break;
		case 4:
			// This case requires for all the directories with names HAMILTON,
			// MADISON be in the current working directory.
			System.out.println("Enter the name of an index to read:");
			String homeDirectory = scan.nextLine();

			DiskPositionalIndex disk_index = new DiskPositionalIndex(
					homeDirectory);
			Path currentPath = Paths.get("").resolve("HAMILTON");
			HashMap<String, List<String>> classes = new HashMap<String, List<String>>();
			File directory = currentPath.toFile();
			List<String> fileNames = new ArrayList<String>();
			for (File file : directory.listFiles()) {
				fileNames.add(file.getName());
			}
			classes.put("HAMILTON", fileNames);

			currentPath = Paths.get("").resolve("MADISON");
			directory = currentPath.toFile();
			List<String> fileNames1 = new ArrayList<String>();
			for (File file : directory.listFiles()) {
				fileNames1.add(file.getName());
			}

			classes.put("MADISON", fileNames1);
			Cosine cosine = new Cosine(disk_index);
			cosine.buildVectors();
			cosine.findAuthors(classes);
			break;
		case 5:
			// This case requires for all the directories with names HAMILTON,
			// MADISON, JAY and HAMILON OR MADISON be in the current working
			// directory.
			System.out.println("Enter the name of an index to read:");
			String directory1 = scan.nextLine();

			DiskPositionalIndex diskPositionalindex = new DiskPositionalIndex(
					directory1);
			Path path = Paths.get("").resolve("HAMILTON");
			HashMap<String, List<String>> classMap = new HashMap<String, List<String>>();
			File directory2 = path.toFile();
			List<String> fileName = new ArrayList<String>();
			for (File file : directory2.listFiles()) {
				fileName.add(file.getName());
			}
			classMap.put("HAMILTON", fileName);

			currentPath = Paths.get("").resolve("MADISON");
			directory = currentPath.toFile();
			List<String> fileNames2 = new ArrayList<String>();
			for (File file : directory.listFiles()) {
				fileNames2.add(file.getName());
			}
			classMap.put("MADISON", fileNames2);

			currentPath = Paths.get("").resolve("JAY");
			directory = currentPath.toFile();
			List<String> fileNames3 = new ArrayList<String>();
			for (File file : directory.listFiles()) {
				fileNames3.add(file.getName());
			}
			classMap.put("JAY", fileNames3);

			BayesianClassifier bayesian = new BayesianClassifier(
					diskPositionalindex, classMap);
			bayesian.findDiscriminatingTerms();
			bayesian.laplaceSmoothing();
			bayesian.findClass();

		}
	}

	private static void printFileContent(Path currentWorkingPath,
			List<String> resultFileNames) throws IOException,
			FileNotFoundException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		System.out.println();
		System.out
				.println("Do you want to see the content of a result file (Y/N): ");
		String answer = bufferedReader.readLine();
		if (answer.equalsIgnoreCase("y")) {
			System.out.println("Enter the name of the file to view: ");
			String file = bufferedReader.readLine();
			if (resultFileNames.contains(file + ".txt")) {
				File folder = currentWorkingPath.toFile();
				File[] listOfFiles = folder.listFiles();

				for (int i = 0; i < listOfFiles.length; i++) {
					File anotherfile = listOfFiles[i];
					if (anotherfile.isFile()
							&& anotherfile.getName().equals(file + ".txt")) {
						BufferedReader br = new BufferedReader(new FileReader(
								anotherfile));
						String line = null;
						while ((line = br.readLine()) != null) {
							System.out.println(line);
						}
					}
				}
			} else {
				System.out
						.println("The file name entered is not contained in the result set.");
			}
		}
	}
}
