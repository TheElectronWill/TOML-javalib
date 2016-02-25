package com.electronwill.toml;

import java.io.IOException;
import java.io.Writer;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;

/**
 * Class for writing TOML v0.4.0.
 * 
 * @author TheElectronWill
 * 		
 */
public final class TomlWriter {
	
	private final Writer writer;
	private final int indentSize;
	private final char indentCharacter;
	private int indentationLevel = 0;
	
	/**
	 * Creates a new TomlWriter with the defaults parameters. This is exactly the same as
	 * {@code TomlWriter(writer, 1, false)}.
	 * 
	 * @param writer where to write the data
	 */
	public TomlWriter(Writer writer) {
		this(writer, 1, false);
	}
	
	/**
	 * Creates a new TomlWriter with the specified parameters.
	 * 
	 * @param writer where to write the data
	 * @param indentSize the size of each indent
	 * @param indentWithSpaces true to indent with spaces, false to indent with tabs
	 */
	public TomlWriter(Writer writer, int indentSize, boolean indentWithSpaces) {
		this.writer = writer;
		this.indentSize = indentSize;
		this.indentCharacter = indentWithSpaces ? ' ' : '\t';
	}
	
	/**
	 * Closes the underlying writer, flushing it first.
	 * 
	 * @throws IOException if an error occurs
	 */
	public void close() throws IOException {
		writer.close();
	}
	
	/**
	 * Flushes the underlying writer.
	 * 
	 * @throws IOException if an error occurs
	 */
	public void flush() throws IOException {
		writer.flush();
	}
	
	/**
	 * Writes the specified data in the TOML format.
	 * 
	 * @param data the data to write
	 * @throws IOException if an error occurs
	 */
	public void write(Map<String, Object> data) throws IOException {
		writeTableContent(data);
	}
	
	private void writeTableContent(Map<String, Object> table) throws IOException {
		indentationLevel++;
		for (Map.Entry<String, Object> entry : table.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Collection) {
				Collection c = (Collection) value;
				if (!c.isEmpty() && c.iterator().next() instanceof Map) {// array of tables
					indentationLevel++;
					for (Object element : c) {
						indent();
						write("[[");
						writeKey(name);
						write("]]\n");
						Map<String, Object> map = (Map) element;
						writeTableContent(map);
					}
					indentationLevel--;
				} else {// normal array
					indent();
					writeKey(name);
					write(" = ");
					writeArray(c);
				}
			} else if (value instanceof Object[]) {
				Object[] array = (Object[]) value;
				if (array.length > 0 && array[0] instanceof Map) {// array of tables
					indentationLevel++;
					for (Object element : array) {
						indent();
						write("[[");
						writeKey(name);
						write("]]\n");
						Map<String, Object> map = (Map) element;
						writeTableContent(map);
					}
					indentationLevel--;
				} else {// normal array
					indent();
					writeKey(name);
					write(" = ");
					writeArray(array);
				}
			} else if (value instanceof Map) {
				indentationLevel++;
				indent();
				write('[');
				writeKey(name);
				write(']');
				newLine();
				writeTableContent((Map) value);
				indentationLevel--;
			} else {
				indent();
				writeKey(name);
				write(" = ");
				writeValue(value);
			}
			newLine();
		}
		indentationLevel--;
		newLine();
	}
	
	private void writeKey(String key) throws IOException {
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_')) {
				writeString(key);
				return;
			}
		}
		write(key);
	}
	
	private void writeString(String str) throws IOException {
		if (str.indexOf('\'') == -1)
			write('\'' + str + '\'');
		else
			write(escape('\"', str, '\"'));
	}
	
	private void writeArray(Collection c) throws IOException {
		write('[');
		for (Object element : c) {
			writeValue(element);
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(Object[] array) throws IOException {
		write('[');
		for (Object element : array) {
			writeValue(element);
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(byte[] array) throws IOException {
		write('[');
		for (byte element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(short[] array) throws IOException {
		write('[');
		for (short element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(char[] array) throws IOException {
		write('[');
		for (char element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(int[] array) throws IOException {
		write('[');
		for (int element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(long[] array) throws IOException {
		write('[');
		for (long element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(float[] array) throws IOException {
		write('[');
		for (float element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeArray(double[] array) throws IOException {
		write('[');
		for (double element : array) {
			write(String.valueOf(element));
			write(", ");
		}
		write(']');
	}
	
	private void writeValue(Object value) throws IOException {
		if (value instanceof String) {
			writeString((String) value);
		} else if (value instanceof Number) {
			write(value.toString());
		} else if (value instanceof TemporalAccessor) {
			write(Toml.DATE_FORMATTER.format((TemporalAccessor) value));
		} else if (value instanceof int[]) {
			writeArray((int[]) value);
		} else if (value instanceof byte[]) {
			writeArray((byte[]) value);
		} else if (value instanceof short[]) {
			writeArray((short[]) value);
		} else if (value instanceof char[]) {
			writeArray((char[]) value);
		} else if (value instanceof long[]) {
			writeArray((long[]) value);
		} else if (value instanceof float[]) {
			writeArray((float[]) value);
		} else if (value instanceof double[]) {
			writeArray((double[]) value);
		} else if (value instanceof Map) {// should not happen because an array of tables is detected by
											// writeTableContent()
			throw new IOException("Unexpected value " + value);
		} else {
			// TODO Should we throw an Exception or write value.toString()??
			writeString(value.toString());
		}
	}
	
	private void newLine() throws IOException {
		writer.write('\n');
	}
	
	private void write(char c) throws IOException {
		writer.write(c);
	}
	
	private void write(String str) throws IOException {
		writer.write(str);
	}
	
	private void indent() throws IOException {
		for (int i = 0; i < indentationLevel; i++) {
			for (int j = 0; j < indentSize; j++) {
				writer.write(indentCharacter);
			}
		}
	}
	
	private String escape(char prefix, String str, char suffix) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\f':
					sb.append("\\f");
					break;
				default:
					sb.append(c);
					break;
			}
		}
		sb.append(suffix);
		return sb.toString();
	}
	
}
