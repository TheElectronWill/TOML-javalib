package test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
	
	public static void main(String[] args) throws IOException, TomlException {
		URL urlExample = Tester.class.getResource("ressources/example.toml");
		URL urlHardExample = Tester.class.getResource("ressources/hard-example.toml");
		URL urlHardExampleUnicode = Tester.class.getResource("ressources/hard-example-unicode.toml");
		URL[] urls = { urlExample, urlHardExample, urlHardExampleUnicode };
		test(urls);
	}
	
	private static void writeTest(Map<String, Object> map, String name) throws IOException {
		if (!name.endsWith(".toml")) {
			name += ".toml";
		}
		File file = new File("Writing " + name);
		Toml.write(map, file);
	}
	
	private static void test(URL[] urls) throws IOException, TomlException {
		for (URL url : urls) {
			System.out.println("Reading " + url);
			Map<String, Object> read = Toml.read(url.openStream());
			printMap(read);
			System.out.println("=================================");
			String[] urlParts = url.getPath().split("/");
			writeTest(read, urlParts[urlParts.length - 1]);
		}
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
