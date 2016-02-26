package com.electronwill.toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
	private int pos = 0;// current position
	private int line = 1;// current line
	
	private char stoppedAt;// the char we stopped at in the last indexOf(char[]) method
	private int lineRead = 0;// the number of lines read without incrementing the position
	
	public TomlReader(String data) {
		this.data = data;
	}
	
	public Map<String, Object> read() throws TOMLException {
		Map<String, Object> map = readTableContent();
		pos++;
		while (pos < data.length()) {
			boolean twoBrackets = (data.charAt(pos) == '[');
			
			// --- Reads the key --
			List<String> keyParts = new ArrayList<>(4);
			StringBuilder keyBuilder = new StringBuilder();
			while (true) {
				if (pos >= data.length())
					throw new TOMLException("Invalid table or table array element declaratio at line " + line + ": not enough data");
				char next = data.charAt(pos++);
				if (next == '\"') {
					keyParts.add(keyBuilder.toString().trim());
					keyBuilder.setLength(0);
					keyParts.add(readBasicString());
				} else if (next == ']') {
					keyParts.add(keyBuilder.toString().trim());
					break;
				} else if (next == '.') {
					keyParts.add(keyBuilder.toString().trim());
					keyBuilder.setLength(0);
				} else if (next == '#') {
					throw new TOMLException("Invalid comment at line " + line);
				} else {
					keyBuilder.append(next);
				}
			}
			
			// -- Check --
			if (twoBrackets) {
				if (data.charAt(pos++) != ']')// there are only one ] that ends the declaration -> error
					throw new TOMLException("Missing character ] at line " + line);
			}
			
			// -- Reads the value (table content) --
			Map<String, Object> value = readTableContent();
			
			// -- Saves the value --
			Map<String, Object> valueMap = map;// the map that contains the value
			for (int i = 0; i < keyParts.size() - 1; i++) {
				String part = keyParts.get(i);
				valueMap = (Map) map.get(part);
			}
			if (twoBrackets) {// element of a table array
				Collection<Map> tableArray = (Collection) valueMap.get(keyParts.get(keyParts.size() - 1));
				tableArray.add(value);
			} else {// just a table
				valueMap.put(keyParts.get(keyParts.size() - 1), value);
			}
			
		}
		return map;
	}
	
	private Collection readArray() throws TOMLException {
		Collection coll = new LinkedList();
		while (true) {
			boolean end = skipSpacesAndLines();
			if (end)
				throw new TOMLException("Invalid end of data: each array must be closed");
			if (data.charAt(pos) == '#') {
				goToNextLine();
				continue;
			}
			Object value = readValue(true, ']', ',', '#');
			if (value == null) {// empty value
				if (stoppedAt == ']')
					return coll;
				throw new TOMLException("Invalid empty value in the array at line " + line);
			}
			coll.add(value);
			if (stoppedAt == ']')
				return coll;
			if (stoppedAt == '#')
				goToNextLine();
		}
	}
	
	private Map<String, Object> readTableContent() throws TOMLException {
		Map<String, Object> map = new HashMap<>();
		while (true) {
			boolean end = skipSpacesAndLines();
			if (end || data.charAt(pos) == '[')
				return map;
			if (data.charAt(pos) == '#') {
				goToNextLine();
				continue;
			}
			String key = readKey();
			Object value = readValue(true, '\n', '#');
			if (stoppedAt == '#')
				goToNextLine();
			map.put(key, value);
		}
		
	}
	
	private Map<String, Object> readInlineTable() throws TOMLException {
		Map<String, Object> map = new HashMap<>();
		while (true) {
			String key = readKey();
			String valueStr = until(false, '}', ',', '#', '\n', '\r');
			// TODO support multiline strings
			if (stoppedAt == '\n' || stoppedAt == '\r')
				throw new TOMLException("Invalid table array at line " + line + ": newlines not allowed");
			Object value = parseValue(valueStr.trim());
			map.put(key, value);
			if (stoppedAt == '}')
				return map;
			if (stoppedAt == '#')
				goToNextLine();
		}
	}
	
	// goes after the = character
	private String readKey() throws TOMLException {
		final String key;
		final char c = data.charAt(pos);
		if (c == '"' || c == '\'') {
			pos++;
			key = (c == '"') ? readBasicString() : readLiteralString();
			String space = until(false, '=');// goes after the =
			for (int i = 0; i < space.length(); i++) {// checks if there is an invalid character between the key and '='
				char shouldBeSpace = space.charAt(i);
				if (shouldBeSpace != ' ' && shouldBeSpace != '\t')
					throw new TOMLException("Invalid key at line " + line);
			}
		} else {
			key = until(false, '=').trim();
			if (key.indexOf(' ') != -1)
				throw new TOMLException("Invalid bare key at line " + line + " spaces/tabs not allowed");
		}
		return key;
	}
	
	// goes after one of the characters of the "ends" array
	private Object readValue(boolean acceptComments, char... ends) throws TOMLException {
		skipSpacesAndLines();
		final String valueStr;
		final char c = data.charAt(pos);
		if (c == '[')
			return readArray();
		if (c == '{')
			return readInlineTable();
		if (c == '"' || c == '\'') {
			// TODO support multiline strings
			pos++;
			valueStr = (c == '"') ? readBasicString() : readLiteralString();
			String space = until(acceptComments, ends);// goes after the end
			for (int i = 0; i < space.length(); i++) {// checks if there is an invalid character after the value
				char shouldBeSpace = space.charAt(i);
				if (shouldBeSpace != ' ' && shouldBeSpace != '\t')
					throw new TOMLException("Invalid value at line " + line);
			}
			return valueStr;
		} else {
			valueStr = until(acceptComments, ends).trim();
			if (valueStr.isEmpty())
				return null;
			if (valueStr.indexOf(' ') != -1)
				throw new TOMLException("Invalid value at line " + line + " spaces/tabs not allowed");
			return parseValue(valueStr);
		}
	}
	
	/**
	 * Parses a boolean, an integer, a decimal number or a date.
	 */
	private Object parseValue(String valueStr) throws TOMLException {
		if (valueStr.equals("true"))
			return true;
		if (valueStr.equals("false"))
			return false;
		boolean maybeInteger = true, maybeDouble = true, maybeDate = true;
		for (int i = 0; i < valueStr.length(); i++) {
			char c = valueStr.charAt(i);
			if (c == 'Z' || c == 'T' || c == ':')
				maybeInteger = maybeDouble = false;
			else if (c == 'e')
				maybeInteger = maybeDate = false;
			else if (c == '.')
				maybeInteger = false;
			else if (c == '_')
				maybeDate = false;
			else if (c == '-' && i != 0 && valueStr.charAt(i - 1) != 'e')
				maybeInteger = maybeDouble = false;
		}
		if (maybeInteger || maybeDouble) {
			valueStr = valueStr.replace("_", "");
			if (maybeInteger) {
				if (valueStr.length() < 10)
					return Integer.parseInt(valueStr);
				return Long.parseLong(valueStr);
			} else {
				return Double.parseDouble(valueStr);
			}
		} else if (maybeDate) {
			return Toml.DATE_FORMATTER.parseBest(valueStr, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
		} else {
			throw new TOMLException("Invalid value: " + valueStr + " at line " + line);
		}
	}
	
	private String readLiteralString() throws TOMLException {
		int index = indexOf('\'');
		if (index == -1)
			throw new TOMLException("Invalid literal String at line " + line + ": it never ends");
		String str = data.substring(pos, index);
		pos = index + 1;
		line += lineRead;
		lineRead = 0;
		return str;
	}
	
	private String readBasicString() throws TOMLException {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		while (true) {
			if (pos >= data.length())
				throw new TOMLException("Invalid basic String at line " + line + ": it nerver ends");
			char ch = data.charAt(pos++);
			if (ch == '\n')
				throw new TOMLException("Invalid basic String at line " + line + ": newlines not allowed");
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
	
	private int indexOf(char c) {
		for (int i = pos; i < data.length(); i++) {
			char ch = data.charAt(i);
			if (ch == '\n')
				lineRead++;
			if (ch == c)
				return i;
		}
		return -1;
	}
	
	private int indexOf(char... cs) {
		for (int i = pos; i < data.length(); i++) {
			char c = data.charAt(i);
			if (c == '\n')
				lineRead++;
			for (char ch : cs) {
				if (ch == c) {
					stoppedAt = c;
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * @return true if it reached the end of the data.
	 */
	private boolean skipSpacesAndLines() {
		for (; pos < data.length(); pos++) {
			char c = data.charAt(pos);
			if (c == '\n')
				line++;
			if (c != ' ' && c != '\t' && c != '\n')
				return false;
		}
		return true;
	}
	
	private void goToNextLine() {
		pos = data.indexOf('\n', pos) + 1;
		line++;
	}
	
	private String until(boolean acceptComments, char... cs) throws TOMLException {
		int index = indexOf(cs);
		if (index == -1)
			throw new TOMLException("Invalid data at line " + line + ": expected one of the following characters: " + Arrays.toString(cs));
			
		String str = data.substring(pos, index);
		if (!acceptComments && str.contains("#"))
			throw new TOMLException("Invalid comment at line " + line);
			
		line += lineRead;
		lineRead = 0;
		pos = index + 1;// after the character we stopped at
		return str;
	}
	
	private char unescape(char c) throws TOMLException {
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
					throw new TOMLException("Invalid unicode point: not enough data");
				String unicode = data.substring(pos, pos + 4);
				int hexVal = Integer.parseInt(unicode, 16);
				return (char) hexVal;
			}
			case 'U': {// unicode U+XXXXXXXX
				if (data.length() - pos < 8)
					throw new TOMLException("Invalid end of data");
				String unicode = data.substring(pos, pos + 8);
				int hexVal = Integer.parseInt(unicode, 16);
				return (char) hexVal;
			}
			default:
				throw new TOMLException("Invalid escape sequence: \\" + c);
		}
	}
	
}
