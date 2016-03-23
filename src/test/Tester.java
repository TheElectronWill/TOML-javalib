package test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.electronwill.toml.Toml;
import com.electronwill.toml.TomlException;

/**
 * Tests the TOML library.
 * 
 * @author TheElectronWill
 * 		
 */
public class Tester {
	
	private static final File outputFolder = new File("./test-output");// where to write TOML data
	private static final File validFolder = new File("./test-valid");// contains valid TOML files
	private static final File invalidFolder = new File("./test-invalid");// contains invalid TOML files
	
	public static void main(String[] args) throws IOException, TomlException, URISyntaxException {
		System.out.println("---------------- Testing with valid files ----------------");
		for (File file : validFolder.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".toml"))
				readAndRewriteTest(file, false);
		}
		
		System.out.println("---------------- Testing with rewritten valid files ----------------");
		for (File file : outputFolder.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".toml"))
				readTest(file, false);
		}
		
		System.out.println("---------------- Testing with invalid files ----------------");
		List<String> noException = new LinkedList<>();
		for (File file : invalidFolder.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".toml")) {
				try {
					Map<String, Object> read = readTest(file, false);
					printMap(read);
					noException.add(file.getName());
					System.err.println("/!\\ No exception thrown when reading an invalid file!");
					System.err.println();
				} catch (Exception ex) {
					System.out.println("--> " + ex.toString());
					System.out.println();
				}
			}
		}
		for (String s : noException) {
			System.err.println("[!] No exception thrown when reading " + s);
		}
	}
	
	private static void readWrittenFiles() throws IOException {
		File validFolder = new File(".");
		for (File file : validFolder.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".toml"))
				readTest(file, false);
		}
	}
	
	private static void writeTest(File file, Map<String, Object> data) throws IOException {
		System.out.println("Writing \"" + file + "\"...");
		
		long t0 = System.nanoTime();
		Toml.write(data, file);
		double time = (System.nanoTime() - t0) / (1000_000.0);
		System.out.println("Written in " + time + " milliseconds");
	}
	
	private static Map<String, Object> readTest(File file, boolean print) throws IOException, TomlException {
		System.out.println("Reading \"" + file.getName() + "\"...");
		
		long t0 = System.nanoTime();
		Map<String, Object> read = Toml.read(file);
		double time = (System.nanoTime() - t0) / (1000_000.0);
		System.out.println("Read in " + time + " milliseconds");
		
		if (print) {
			System.out.println("====== Data output =======");
			printMap(read);
			System.out.println("====== End of data =======");
		}
		System.out.println();
		return read;
	}
	
	private static void readAndRewriteTest(File file, boolean print) throws IOException, TomlException {
		String[] pathParts = file.getPath().split("/");
		System.out.println("====== " + pathParts[pathParts.length - 2] + "/" + pathParts[pathParts.length - 1] + " ======");
		Map<String, Object> data = readTest(file, print);
		File out = new File(outputFolder, file.getName());
		writeTest(out, data);
		System.out.println();
	}
	
	private static void printMap(Map<String, Object> map) {
		System.out.println("{{");
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			System.out.print(name);
			System.out.print(" = ");
			if (value instanceof Map) {
				printMap((Map) value);
			} else if (value instanceof Collection) {
				printCollection((Collection) value);
			} else {
				System.out.println(value);
			}
		}
		System.out.println("}}");
	}
	
	private static void printCollection(Collection c) {
		System.out.print("[");
		for (Object o : c) {
			System.out.print(o);
			System.out.print(", ");
		}
		System.out.println("]");
	}
	
}
