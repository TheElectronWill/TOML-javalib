package com.electronwill.toml;

import java.io.EOFException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for reading TOML v0.4.0.
 * 
 * @author TheElectronWill
 * 		
 */
public final class TomlReader {
	
	private final String data;
	private final List<Integer> newlines;
	private int pos = 0;
	private Map<String, Object> map;
	
	/**
	 * Creates a new TomlReader with the specified data to read.
	 * 
	 * @param data the data to read
	 * @param newlines a list containing the position of every newlines (\n characters).
	 */
	public TomlReader(String data, List<Integer> newlines) {
		this.data = data;
		this.newlines = newlines;
	}
	
	/**
	 * Reads the TOML data as a {@code Map<String, Object>}.
	 * <h1>Data types</h1>
	 * <ul>
	 * <li>A boolean is parsed as a <code>boolean</code></li>
	 * <li>An integer number is parsed as an <code>int</code> or a <code>long</code> (int if smaller than 10^10, long
	 * otherwise).</li>
	 * <li>A decimal number, or a number written with the exponential notation, is parsed as a <code>double</code>.</li>
	 * <li>A date is parsed as a {@link ZonedDateTime} or a {@link LocalDateTime} or a {@link LocalDate} (depending on
	 * the informations provided by the data)</li>
	 * <li>An array (even if it's an array of numbers) is parsed as a {@link List}</li>
	 * <li>A table is parsed as a {@code Map<String, Object>}</li>
	 * </ul>
	 * 
	 */
	public Map<String, Object> read() throws IOException {
		map = readTableContent();// reads everything until the first table (remember that non-inline tables are declared
									// at the end of the file, because a table ends with another table, or with the end
									// of the file)
		while (pos < data.length()) {
			int firstChar = data.charAt(pos++);// The [ character has already been read by #readTableContent()
			
			// -- Reads the key --
			if (firstChar == '[') {// there are two [
				pos++;
			}
			List<String> keyParts = new ArrayList<>(4);
			StringBuilder keyBuilder = new StringBuilder();
			while (true) {
				if (pos >= data.length())
					throw new IOException("Invalid end of file: missing end of key declaration");
				char next = data.charAt(pos);
				if (next == '\"') {
					keyParts.add(keyBuilder.toString().trim());
					keyBuilder = new StringBuilder();
					keyParts.add(readBasicString());
				} else if (next == ']') {
					keyParts.add(keyBuilder.toString().trim());
					break;
				} else if (next == '.') {
					keyParts.add(keyBuilder.toString().trim());
					keyBuilder = new StringBuilder();
				} else {
					keyBuilder.append(next);
				}
			}
			if (firstChar == '[') {// there were two [
				if (data.charAt(pos++) != ']')// there are only one ] that ends the declaration -> error
					throw new IOException("Missing character ] at " + getCurrentPosition());
			}
			
			// -- Reads the value --
			Map<String, Object> value = readTableContent();
			
			// -- Saves the value --
			Map<String, Object> valueMap = map;// the map that contains the value
			for (int i = 0; i < keyParts.size() - 1; i++) {
				String part = keyParts.get(i);
				valueMap = (Map) map.get(part);
			}
			if (firstChar == '[') {// element of a table array
				Collection<Map> tableArray = (Collection) valueMap.get(keyParts.get(keyParts.size() - 1));
				tableArray.add(value);
			} else {// just a table
				valueMap.put(keyParts.get(keyParts.size() - 1), value);
			}
		}
		return map;
	}
	
	// === Methods for reading data structures ===
	
	/**
	 * Reads the content of a table. Stops at the next table (reads the [ character), or at the end of the file.
	 */
	private Map<String, Object> readTableContent() throws IOException {
		HashMap<String, Object> table = new HashMap<>();
		while (true) {
			final int ch = nextChar();
			String key = null;
			Object value = null;
			
			if (ch == '[' || ch == -1) {
				return table;
			} else if (ch == '\"') {
				key = readBasicString();
				goAfter('=', false);
			} else if (ch == '\'') {
				key = readLiteralString();
				goAfter('=', false);
			} else if (ch == '\n' || ch == '\t' || ch == ' ') {
				continue;// ignores
			} else if (ch == '#') {
				goAfterOrAtEnd('\n');
				continue;
			} else {
				pos--;// unreads ch
				// Reads the name of the key:
				StringBuilder keyBuilder = new StringBuilder();
				while (true) {
					if (pos >= data.length())
						throw new IOException("Invalid end of file: missing = character at " + getCurrentPosition());
					char next = data.charAt(pos++);
					if (next == '#')
						throw new IOException("Invalid comment: missing = character at " + getCurrentPosition());
					if (next == '\t' || next == ' ')
						continue;
					if (next == '=')
						break;
					else
						keyBuilder.append(next);
				}
				key = keyBuilder.toString();
				
				// Skips whitespaces after the = character:
				while (true) {
					if (pos >= data.length())
						throw new IOException("Invalid end of file: missing value after the = character at " + getCurrentPosition());
					char next = data.charAt(pos);
					if (next != ' ' && next != '\t')
						break;
					pos++;// increments the position only if the character was a space or a tab, for the readValue()
							// method not to miss the first character of the value
				}
				
			}
			
			value = readValue(true, true, '[');
			table.put(key, value);
		}
	}
	
	/**
	 * Reads an inline table.
	 */
	private Map<String, Object> readInlineTable() throws IOException {
		Map<String, Object> table = new HashMap<>();
		while (true) {
			final int ch = nextChar();
			final String key;
			if (ch == '}') {// end of table
				return table;
			} else if (ch == '\"') {
				key = readBasicString();
				pos = data.indexOf('=', pos) + 1;// goes after the '=' character
			} else if (ch == '\'') {
				key = readLiteralString();
				pos = data.indexOf('=', pos) + 1;// goes after the '=' character
			} else if (ch == '\t' || ch == ' ') {
				continue;// ignores
			} else if (ch == '\n') {
				throw new IOException("Invalid line break in an inline table, at the end of line " + (getPreviousLine()));
			} else if (ch == '#') {
				throw new IOException("Invalid comment in an inline table at " + getCurrentPosition());
			} else {
				pos--;// unreads ch
				StringBuilder sb = new StringBuilder();
				boolean needToFindEqual = true;
				for (; pos < data.length(); pos++) {
					char next = data.charAt(pos);
					if (next == ' ' || next == '\t')
						break;
					if (next == '=') {
						needToFindEqual = false;
						break;
					}
					if (next == '#' || next == '\n')
						throw new IOException("Invalid character in an inline table at " + getCurrentPosition());
						
				}
				if (needToFindEqual) {
					for (; pos < data.length(); pos++) {
						char next = data.charAt(pos++);
						if (next == '=')
							break;
						if (next == '#' || next == '\n')
							throw new IOException("Invalid character in an inline table at " + getCurrentPosition());
					}
				}
				key = sb.toString();
			}
			
			final Object value = readValue(false, false, ',', '}');
			table.put(key, value);
			
			if (data.charAt(pos) == '}')// end of table
				return table;
		}
	}
	
	/**
	 * Reads an array of values.
	 */
	private List<Object> readArray() throws IOException {
		List<Object> list = new ArrayList();
		while (true) {
			int ch = nextChar();
			if (ch == ']')// end of array
				return list;
			else if (ch == '\n' || ch == '\t' || ch == ' ')
				continue;// ignores
			else if (ch == '#') {// comment
				goAfterOrAtEnd('\n');
				continue;
			} else
				pos--;// unreads ch
				
			Object v = readValue(true, false, ',', ']');
			list.add(v);
			
			if (data.charAt(pos) == ']')// end of array
				return list;
		}
	}
	
	// === Methods for reading some values ===
	
	/**
	 * Reads the next value.
	 * 
	 * @param acceptEOF true if EOF is normal, false if it should throw an exception
	 * @param end the characters that marks the end of the value
	 */
	private Object readValue(boolean acceptComment, boolean acceptEOF, char... end) throws IOException {
		int ch = nextChar();
		switch (ch) {
			case '[':
				return readArray();
			case '{':
				return readInlineTable();
			case '\"': {
				if (seekNext(1) == '\"' && seekNext(2) == '\"') {
					return readBasicMultiString();
				}
				return readBasicString();
			}
			case '\'': {
				if (seekNext(1) == '\'' && seekNext(2) == '\'') {
					return readLiteralMultiString();
				}
				return readLiteralString();
			}
			case 'f':
				return readFalseBoolean();
			case 't':
				return readTrueBoolean();
			default:
				pos--;
				return readDateOrNumber(acceptComment, acceptEOF, end);
			case -1:
				return null;
			case '#':
				if (!acceptComment)
					throw new IOException("Invalid comment at " + getCurrentPosition());
				goAfterOrAtEnd('\n');
				return readValue(acceptEOF, acceptComment, end);
		}
	}
	
	/**
	 * Reads a date or a number.
	 * 
	 * @param firstCharWasRead true if the first char of the value has been read
	 * @param acceptEOF true if EOF is normal, false if it should throw an exception
	 * @param end the characters that marks the end of the value
	 */
	private Object readDateOrNumber(boolean acceptComment, boolean acceptEOF, char... end) throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean maybeLong = true, maybeDouble = true, maybeDate = true;
		for (; pos < data.length(); pos++) {
			char next = data.charAt(pos++);
			if (next == '#') {
				goAfterOrAtEnd('\n');
				break;
			} else if (next == '.' || next == 'e') {
				maybeLong = false;
			} else if (next == ':' || next == 'T' || next == 'Z') {
				maybeLong = maybeDouble = false;
			}
			if (next == '_') {
				maybeDate = false;
			} else {// don't add the _ to the StringBuilder because it would cause a NumberFormatException
				sb.append(next);
			}
		}
		String str = sb.toString();
		if (maybeLong) {
			return str.length() < 10 ? Integer.parseInt(str) : Long.parseLong(str);
		} else if (maybeDouble) {
			return Double.parseDouble(str);
		} else if (maybeDate) {
			return Toml.DATE_FORMATTER.parseBest(str, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
		} else {
			throw new IOException("Invalid value at " + getCurrentPosition() + ": " + str);
		}
	}
	
	// === Methods for reading booleans ===
	
	private boolean readFalseBoolean() throws IOException {
		String boolEnd;
		try {
			boolEnd = data.substring(pos, pos + 4);
		} catch (IndexOutOfBoundsException ex) {
			throw new EOFException("Invalid end of data");
		}
		
		if (!boolEnd.equals("alse"))
			throw new IOException("Invalid boolean value at " + getCurrentPosition());
			
		pos += 4;
		return false;
	}
	
	private boolean readTrueBoolean() throws IOException {
		String boolEnd;
		try {
			boolEnd = data.substring(pos, pos + 3);
		} catch (IndexOutOfBoundsException ex) {
			throw new EOFException("Invalid end of data");
		}
		
		if (!boolEnd.equals("rue"))
			throw new IOException("Invalid boolean value at " + getCurrentPosition());
			
		pos += 3;
		return true;
	}
	
	// === Methods for reading Strings ===
	
	/**
	 * Reads a literal String which doesn't support escaping.
	 * 
	 * @throws EOFException
	 */
	private String readLiteralString() throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int ch = nextChar();
			if (ch == -1)
				throw new IOException("Invalid end of literal string at " + getCurrentPosition());
			if (ch == '\'')
				break;
			sb.append(ch);
		}
		return sb.toString();
	}
	
	/**
	 * Reads a literal multi-line String, which doesn't support escaping.
	 */
	private String readLiteralMultiString() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (true) {
			int ch = nextChar();
			if (first) {
				first = false;
				if (ch == '\n') {// skips the first newline
					continue;
				}
			}
			if (ch == -1)
				throw new EOFException("Invalid end of data");
			if (ch == '\'') {
				if (seekNext(1) != '\'' && seekNext(2) != '\'')
					throw new IOException("Invalid end of multi-line string at " + getCurrentPosition());
				return sb.toString();
			} else {
				sb.append(ch);
			}
		}
	}
	
	/**
	 * Reads a basic String which supports escaping.
	 */
	private String readBasicString() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		while (true) {
			int ch = nextChar();
			if (ch == -1)
				throw new EOFException("Invalid end of data");
			if (ch == '\n')
				throw new IOException("Invalid line break after line " + getPreviousLine());
			if (escape) {
				sb.append(unescape(ch));
				escape = false;
			} else if (ch == '\\') {
				escape = true;
			} else if (ch == '\"') {
				return sb.toString();
			} else {
				sb.append(ch);
			}
		}
	}
	
	/**
	 * Reads a literal multi-line String, which supports escaping.
	 */
	private String readBasicMultiString() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean escape = false, first = true;
		while (true) {
			int ch = nextChar();
			if (first) {
				first = false;
				if (ch == '\n') {// skips the first newline
					continue;
				}
			}
			if (ch == -1)
				throw new EOFException("Invalid end of data");
			if (escape) {
				sb.append(unescape(ch));
				escape = false;
			} else if (ch == '\\') {
				escape = true;
			} else if (ch == '\"') {
				if (seekNext(1) != '\"' && seekNext(2) != '\"')
					throw new IOException("Invalid end of multi-line string at " + getCurrentPosition());
				return sb.toString();
			} else {
				sb.append(ch);
			}
		}
	}
	
	// === Utilities for manipulating characters and Strings ===
	
	private char unescape(int c) throws IOException {
		if (c == -1)
			throw new EOFException("Invalid end of data at " + getCurrentPosition());
		switch (c) {
			case 'b':
				return '\b';
			case 't':
				return '\t';
			case 'n':
				return '\n';
			case 'f':
				return '\f';
			case 'r':
				return '\r';
			case '\"':
				return '\"';
			case '\\':
				return '\\';
			case 'u': {// unicode U+XXXX
				if (data.length() - pos < 4)
					throw new EOFException("Invalid end of data");
				String unicode = data.substring(pos, pos + 4);
				int hexVal = Integer.parseInt(unicode, 16);
				return (char) hexVal;
			}
			case 'U': {// unicode U+XXXXXXXX
				if (data.length() - pos < 8)
					throw new EOFException("Invalid end of data");
				String unicode = data.substring(pos, pos + 8);
				int hexVal = Integer.parseInt(unicode, 16);
				return (char) hexVal;
			}
			case -1:
				throw new EOFException("Invalid end of data");
			default:
				throw new IOException("Invalid escape sequence at " + getCurrentPosition() + ": \\" + c);
		}
	}
	
	/**
	 * Gets the next char, or -1 if EOF is reached, and increments the position.
	 */
	private int nextChar() {
		if (pos >= data.length())
			return -1;
		return data.charAt(pos++);
	}
	
	/**
	 * Gets the next char, or -1 if EOF is reached, but does not increment the position.
	 */
	private int seekNext(int n) {
		if (pos >= data.length())
			return -1;
		return data.charAt(pos + (n - 1));
	}
	
	/**
	 * Goes at the position of the next character that equals to c.
	 */
	private void goAt(char c, boolean acceptNewlines) throws IOException {
		for (; pos < data.length(); pos++) {
			char ch = data.charAt(pos);
			if (ch == c)
				return;
			if (ch == '\n' && !acceptNewlines) {
				throw new IOException("Invalid line break after line " + getPreviousLine());
			}
		}
	}
	
	/**
	 * Goes after the position of the next character that equals to c.
	 */
	private void goAfter(char c, boolean acceptNewlines) throws IOException {
		goAt(c, acceptNewlines);
		pos++;
	}
	
	/**
	 * Goes after the position of the next character that equals to c, or at the end of the file.
	 */
	private void goAfterOrAtEnd(char c) throws IOException {
		int index = data.indexOf(c, pos);
		pos = (index == -1) ? data.length() : index;
	}
	
	// === Methods for getting the current position in text ===
	
	/**
	 * Constructs a string indicating the current position in the data. The format is:
	 * {@code line <line number> position <position on line>}. For example "line 10 position 25".
	 */
	private String getCurrentPosition() {
		int previousLineStart = 0;
		for (int i = 0; i < newlines.size(); i++) {
			int newLinePosition = newlines.get(i);
			if (pos < newLinePosition) {
				return "line " + (i + 1) + " position " + (pos - previousLineStart);
			}
			previousLineStart = newLinePosition;
		}
		return "line 1 position " + pos;
	}
	
	/**
	 * Gets the number of the previous line.
	 */
	private int getPreviousLine() {
		return getCurrentLine() - 1;
	}
	
	/**
	 * Gets the number of the current line.
	 */
	private int getCurrentLine() {
		for (int i = 0; i < newlines.size(); i++) {
			int newLinePosition = newlines.get(i);
			if (pos < newLinePosition)
				return i;
		}
		return newlines.size();
	}
	
}
