package test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.electronwill.toml.Toml;

/**
 * Tests the TOML library.
 * 
 * @author TheElectronWill
 * 		
 */
public class Tester {
	
	public static void main(String[] args) throws IOException {
		URL urlExample = Tester.class.getResource("ressources/example.toml");
		URL urlHardExample = Tester.class.getResource("ressources/hard-example.toml");
		URL urlHardExampleUnicode = Tester.class.getResource("ressources/hard-example-unicode.toml");
		URL[] urls = { urlExample, urlHardExample, urlHardExampleUnicode };
		test(urls);
		Map<String, Object> map = new HashMap<>();
		map.put("yolo", "egriheoigh _uç(-");
		map.put("salut dd", 48468.256);
		map.put("test", new HashMap());
		map.put("testt", Arrays.asList(urls));
		map.put("yolo.a", new int[] { 12, 52, 8, 0, -5 });
		writeTest(map, "test01");
	}
	
	private static void writeTest(Map<String, Object> map, String name) throws IOException {
		System.out.println("Writing " + name);
		File file = new File("output- " + name + ".toml");
		Toml.write(map, file);
	}
	
	private static void test(URL[] urls) throws IOException {
		for (URL url : urls) {
			System.out.println("Reading " + url);
			Map<String, Object> read = Toml.read(url.openStream());
			printMap(read);
			System.out.println("=================================");
			writeTest(read, url.getFile());
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
			System.out.print(c);
			System.out.print(", ");
		}
		System.out.println("]");
	}
	
}
