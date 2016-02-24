package com.electronwill.toml;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TomlWriter {
	
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
	
	public void write(Map<String, Object> data) throws IOException {
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				writer.write('[');
				writer.write(name);
				writer.write(']');
				writer.write('\n');
				write((Map) value);
				writer.write('\n');
			} else if (value instanceof Collection) {
				Collection c = (Collection) value;
				if (c.isEmpty()) {
					writer.write(name);
					writer.write(" = []\n");
					continue;
				}
				Iterator it = c.iterator();
				Object first = it.next();
				if (first instanceof Map) {
					writer.write("[[");
					writer.write(name);
					writer.write("]]");
					write((Map) first);
					while (it.hasNext()) {
						Map nextMap = (Map) it.next();
						writer.write("[[");
						writer.write(name);
						writer.write("]]");
						write(nextMap);
					}
				} else {
					writeValue(first);
					writer.write(", ");
					while (it.hasNext()) {
						writeValue(it.next());
						writer.write(", ");
					}
				}
			}
			if (value instanceof TemporalAccessor) {
				writer.write(name);
				writer.write(" = ");
				writer.write(Toml.DATE_FORMATTER.format((TemporalAccessor) value));
			} else if (value instanceof String) {
				String str = (String) value;
				
				if (str.indexOf('\'') == -1)
					str = '\'' + str + '\'';
				else
					str = escape('\"', str, '\"');
					
				writer.write(" = ");
				writer.write(str);
			} else if (value instanceof List<?>) {
				writer.write(name);
			}
			if (value instanceof Number) {
				writer.write(name);
				writer.write(" ");
				writer.write(value.toString());
			}
		}
	}
	
	private void writeTableContent(Map<String, Object> table) throws IOException {
		for (Map.Entry<String, Object> entry : table.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Collection) {
				Collection c = (Collection) value;
				if (!c.isEmpty() && c.iterator().next() instanceof Map) {// array of tables
					for (Object element : c) {
						writer.write("[[");
						writer.write(name);
						writer.write("]]\n");
						Map<String, Object> map = (Map) element;
						writeTableContent(map);
						writer.write('\n');
					}
				} else {// normal array
					writer.write('[');
					for (Object element : c) {
						writeValue(element);
					}
					writer.write(']');
					writer.write('\n');
				}
			} else if (value instanceof Object[]) {
				Object[] array = (Object[]) value;
				if (array.length > 0 && array[0] instanceof Map) {// array of tables
					for (Object element : array) {
						writer.write("[[");
						writer.write(name);
						writer.write("]]\n");
						Map<String, Object> map = (Map) element;
						writeTableContent(map);
						writer.write('\n');
					}
				} else {// normal array
					writer.write('[');
					for (Object element : array) {
						writeValue(element);
					}
					writer.write(']');
					writer.write('\n');
				}
			} else if (value instanceof Map) {
				indentationLevel++;
				writer.write("\n[");
				writer.write(name);
				writer.write("]");
				writer.write('\n');
				writeTableContent((Map) value);
				writer.write('\n');
			} else {
				writer.write(name);
				writer.write(" = ");
				if (value instanceof String) {
					writeString((String) value);
				} else if (value instanceof Number) {
					writeString(value.toString());
				} else if (value instanceof TemporalAccessor) {
					writeString(Toml.DATE_FORMATTER.format((TemporalAccessor) value));
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
				} else {
					// TODO What should we do: throw an Exception or write value.toString() ?
					writeString(value.toString());
				}
				writer.write('\n');
			}
		}
	}
	
	private void writeIndented(String str) throws IOException {// TODO utiliser ça
		indent();
		writer.write(str);
	}
	
	private void indent() throws IOException {
		for (int i = 0; i < indentationLevel; i++) {
			for (int j = 0; j < indentSize; j++) {
				writer.write(indentCharacter);
			}
		}
	}
	
	private void writeString(String str) throws IOException {
		if (str.indexOf('\'') == -1)
			str = '\'' + str + '\'';
		else
			str = escape('\"', str, '\"');
			
		writer.write(str);
	}
	
	private void writeArray(Collection c) throws IOException {
		writer.write('[');
		for (Object element : c) {
			writeValue(element);
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(Object[] array) throws IOException {
		writer.write('[');
		for (Object element : array) {
			writeValue(element);
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(byte[] array) throws IOException {
		writer.write('[');
		for (byte element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(short[] array) throws IOException {
		writer.write('[');
		for (short element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(char[] array) throws IOException {
		writer.write('[');
		for (char element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(int[] array) throws IOException {
		writer.write('[');
		for (int element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(long[] array) throws IOException {
		writer.write('[');
		for (long element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(float[] array) throws IOException {
		writer.write('[');
		for (float element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeArray(double[] array) throws IOException {
		writer.write('[');
		for (double element : array) {
			writer.write(String.valueOf(element));
			writer.write(",");
		}
		writer.write(']');
	}
	
	private void writeValue(Object value) throws IOException {
		if (value instanceof TemporalAccessor) {
			writer.write(DateTimeFormatter.ISO_LOCAL_DATE.format((TemporalAccessor) value));
		} else if (value instanceof String) {
			String str = (String) value;
			
			if (str.indexOf('\'') == -1)
				str = '\'' + str + '\'';
			else
				str = escape('\"', str, '\"');
				
			writer.write(str);
		}
		// --- Collections and Arrays ---
		else if (value instanceof Collection) {
			writer.write('[');
			for (Object element : (Collection) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof Object[]) {
			writer.write('[');
			for (Object element : (Object[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof byte[]) {
			writer.write('[');
			for (byte element : (byte[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof short[]) {
			writer.write('[');
			for (short element : (short[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof char[]) {
			writer.write('[');
			for (char element : (char[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof int[]) {
			writer.write('[');
			for (int element : (int[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof long[]) {
			writer.write('[');
			for (long element : (long[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof float[]) {
			writer.write('[');
			for (float element : (float[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} else if (value instanceof double[]) {
			writer.write('[');
			for (double element : (double[]) value) {
				writeValue(element);
				writer.write(",");
			}
			writer.write(']');
		} // --- End of collections and arrays ---
		else if (value instanceof Map) {
			Map<String, Object> map = (Map) value;
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String entryName = entry.getKey();
				Object entryValue = entry.getValue();
				if (entryValue instanceof Map) {
				
				}
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
