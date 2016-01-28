package com.electronwill.toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Toml {
	
	private Toml() {}
	
	public static String writeToString(Map<String, Object> data) throws IOException {
		FastStringWriter writer = new FastStringWriter();
		write(data, writer);
		return writer.toString();
	}
	
	public static void write(Map<String, Object> data, Writer out) throws IOException {
	
	}
	
	/**
	 * WARNING: toml must contains ONLY \n, not "\r\n" nor \r.
	 */
	public static Map<String, Object> read(String toml) throws IOException {
		List<Integer> newlines = new ArrayList<>();
		for (int i = 0; i < toml.length(); i++) {
			if (toml.charAt(i) == '\n')
				newlines.add(i);
		}
		TomlReader tr = new TomlReader(toml, newlines);
		return tr.read();
	}
	
	public static Map<String, Object> read(InputStream in) throws IOException {
		return read(new InputStreamReader(in, StandardCharsets.UTF_8), in.available());
	}
	
	public static Map<String, Object> read(Reader reader, int stringBuilderSize) throws IOException {
		StringBuilder sb = new StringBuilder(stringBuilderSize);
		List<Integer> newlines = new ArrayList<>();
		
		char[] buf = new char[8192];
		int read;
		while ((read = reader.read(buf)) != -1) {
			boolean wasCR = false;
			for (char c : buf) {
				if (wasCR) {
					if (c != '\n')
						sb.append(c);
					wasCR = false;
				} else if (c == '\r') {
					wasCR = true;
					newlines.add(sb.length());
					sb.append('\n');
				} else {
					sb.append(c);
				}
			}
			sb.append(buf, 0, read);
		}
		
		TomlReader tr = new TomlReader(sb.toString(), newlines);
		return tr.read();
	}
	
}
