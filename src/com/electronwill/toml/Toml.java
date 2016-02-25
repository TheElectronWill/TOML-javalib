package com.electronwill.toml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading and writing TOML v0.4.0.
 * 
 * @author TheElectronWill
 * 		
 */
public final class Toml {
	
	/**
	 * A DateTimeFormatter that uses the TOML format.
	 */
	public static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
			.append(DateTimeFormatter.ISO_LOCAL_DATE)
			.optionalStart()
			.appendLiteral('T')
			.append(DateTimeFormatter.ISO_LOCAL_TIME)
			.optionalStart()
			.appendOffsetId()
			.optionalEnd()
			.optionalEnd()
			.toFormatter();
			
	private Toml() {}
	
	/**
	 * Writes the specified data to a String, in the TOML format.
	 * 
	 * @param data the data to write
	 * @return a String that contains the data in the TOML format.
	 * @throws IOException if an error occurs
	 */
	public static String writeToString(Map<String, Object> data) throws IOException {
		FastStringWriter writer = new FastStringWriter();
		write(data, writer);
		return writer.toString();
	}
	
	/**
	 * Writes data to a File, in the TOML format and with the UTF-8 encoding. The default indentation parameters are
	 * used, ie each indent is one tab character.
	 * 
	 * @param data the data to write
	 * @param file where to write the data
	 * @throws IOException if an error occurs
	 */
	public static void write(Map<String, Object> data, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		write(data, out);
	}
	
	/**
	 * Writes data to an OutputStream, in the TOML format and with the UTF-8 encoding. The default indentation
	 * parameters are used, ie each indent is one tab character.
	 * 
	 * @param data the data to write
	 * @param file where to write the data
	 * @throws IOException if an error occurs
	 */
	public static void write(Map<String, Object> data, OutputStream out) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		write(data, writer);
	}
	
	/**
	 * Writes data to a Writer, in the TOML format and with the default parameters, ie each indent is 1 tab character.
	 * This is the same as {@code write(data, writer, 1, false)}.
	 * 
	 * @param data the data to write
	 * @param writer where to write the data
	 * @throws IOException if an error occurs
	 */
	public static void write(Map<String, Object> data, Writer writer) throws IOException {
		TomlWriter tw = new TomlWriter(writer);
		tw.write(data);
	}
	
	/**
	 * Writes the specified data to a Writer, in the TOML format and with the specified parameters.
	 * 
	 * @param data the data to write
	 * @param writer where to write the data
	 * @param indentSize the indentation size, ie the number of times the indentation character is repeated in one
	 *        indent.
	 * @param indentWithSpaces true to indent with spaces, false to indent with tabs
	 * @throws IOException if an error occurs
	 */
	public static void write(Map<String, Object> data, Writer writer, int indentSize, boolean indentWithSpaces) throws IOException {
		TomlWriter tw = new TomlWriter(writer, indentSize, indentWithSpaces);
		tw.write(data);
	}
	
	/**
	 * Reads a String that contains TOML data.
	 * <p>
	 * <b>WARNING: the <code>String <i>toml</i></code> must indicate newlines ONLY with the '\n' (aka LF) character. It
	 * must not contains the '\r' character nor the "\r\n" sequence.</b> This method does not check if
	 * <code>String <i>toml</i></code> contains the forbidden '\r' character. It's up to the caller to do it.
	 * </p>
	 * 
	 * @return a {@code Map<String, Object>} containing the parsed data
	 * @throws IOException if an error occurs
	 */
	public static Map<String, Object> readLineFeedOnlyString(String toml) throws IOException {
		List<Integer> newlines = new ArrayList<>();
		for (int i = 0; i < toml.length(); i++) {
			if (toml.charAt(i) == '\n')
				newlines.add(i);
		}
		TomlReader tr = new TomlReader(toml, newlines);
		return tr.read();
	}
	
	/**
	 * Reads TOML data from an InputStream. This method may contains "\r\n" sequence, because any "\r\n" sequence found
	 * in the data read by the Reader is replaced by a single '\n' character.
	 * 
	 * @param in the InputStream to read data from
	 * @return a {@code Map<String, Object>} containing the parsed data
	 * @throws IOException if an error occurs
	 */
	public static Map<String, Object> read(InputStream in) throws IOException {
		return read(new InputStreamReader(in, StandardCharsets.UTF_8), in.available());
	}
	
	/**
	 * Reads TOML data from a Reader, with a specific <code>stringBuilderSize</code>. This method may contains "\r\n"
	 * sequence, because any "\r\n" sequence found in the data read by the Reader is replaced by a single '\n'
	 * character.
	 * 
	 * @param in the InputStream to read data from
	 * @return a {@code Map<String, Object>} containing the parsed data
	 * @throws IOException if an error occurs
	 */
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
