package com.electronwill.toml;

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
 * <h1>About newlines</h1>
 * <p>
 * This reader only supports '\n' and "\r\n" as newlines. Any '\r' not followed by '\n' causes some issues. This should
 * not be a problem, because the modern operating systems use '\n' (Linux, OSX) or "\r\n" (Windows).
 * </p>
 * 
 * @author TheElectronWill
 * 		
 */
public final class TomlReader {
	
	private final String data;
	private int pos = 0;// current position
	private int line = 1;// current line
	
	public TomlReader(String data) {
		this.data = data;
	}
	
	private boolean hasNext() {
		return pos < data.length();
	}
	
	private char next() {
		return data.charAt(pos++);
	}
	
	private char nextUseful(boolean skipComments) {
		char c = ' ';
		while (hasNext() && (c == ' ' || c == '\t' || c == '\r' || c == '\n' || (c == '#' && skipComments))) {
			c = next();
			if (c == '#') {
				int nextLinebreak = data.indexOf('\n', pos);
				if (nextLinebreak == -1) {
					pos = data.length();
				} else {
					pos = nextLinebreak + 1;
					line++;
				}
			} else if (c == '\n') {
				line++;
			}
		}
		return c;
	}
	
	private char nextUsefulOrLinebreak() {
		char c = ' ';
		while (c == ' ' || c == '\t' || c == '\r')
			c = next();
		if (c == '\n')
			line++;
		return c;
	}
	
	private Object nextValue(char firstChar) {
		switch (firstChar) {
			case '+':
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return nextNumberOrDate(firstChar);
			case '"':
				if (pos + 1 < data.length()) {
					char c2 = data.charAt(pos);
					char c3 = data.charAt(pos + 1);
					if (c2 == '"' && c3 == '"') {
						pos += 2;
						return nextBasicMultilineString();
					}
				}
				return nextBasicString();
			case '\'':
				if (pos + 1 < data.length()) {
					char c2 = data.charAt(pos);
					char c3 = data.charAt(pos + 1);
					if (c2 == '\'' && c3 == '\'') {
						pos += 2;
						return nextLiteralMultilineString();
					}
				}
				return nextLiteralString();
			case '[':
				return nextArray();
			case '{':
				return nextInlineTable();
			case 't':// Must be "true"
				if (pos + 3 >= data.length() || next() != 'r' || next() != 'u' || next() != 'e') {
					throw new TOMLException("Invalid value at line " + line);
				}
				return true;
			case 'f':// Must be "false"
				if (pos + 4 >= data.length() || next() != 'a' || next() != 'l' || next() != 's' || next() != 'e') {
					throw new TOMLException("Invalid value at line " + line);
				}
				return false;
			default:
				throw new TOMLException("Invalid character at line " + line);
		}
	}
	
	public Map<String, Object> read() {
		Map<String, Object> map = nextTableContent();
		
		while (hasNext()) {
			char c = nextUseful(true);
			boolean twoBrackets = (c == '[');
			pos--;
			
			// --- Reads the key --
			List<String> keyParts = new ArrayList<>(4);
			StringBuilder keyBuilder = new StringBuilder();
			whileLoop: while (true) {
				if (!hasNext())
					throw new TOMLException("Invalid table declaration at line " + line + ": not enough data");
				char next = nextUsefulOrLinebreak();
				switch (next) {
					case '"': {
						String current = keyBuilder.toString().trim();
						if (current.length() > 0) {
							keyParts.add(current);
							keyBuilder.setLength(0);
						}
						keyParts.add(nextBasicString());
						break;
					}
					case '\'': {
						String current = keyBuilder.toString().trim();
						if (current.length() > 0) {
							keyParts.add(current);
							keyBuilder.setLength(0);
						}
						keyParts.add(nextLiteralString());
						break;
					}
					case ']': {
						String current = keyBuilder.toString().trim();
						if (!current.isEmpty())
							keyParts.add(current);
						break whileLoop;
					}
					case '.': {
						String current = keyBuilder.toString().trim();
						if (current.length() == 0) {
							throw new TOMLException("Invalid table name at line " + line + ": empty value are not allowed here");
						}
						keyParts.add(current);
						keyBuilder.setLength(0);
						break;
					}
					case '#':
						throw new TOMLException("Invalid table name at line " + line + ": comments are not allowed here");
					case '\n':
					case '\r':
						throw new TOMLException("Invalid table name at line " + line + ": line breaks are not allowed here");
					default:
						keyBuilder.append(next);
						break;
				}
			}
			
			// -- Check --
			if (twoBrackets) {
				if (next() != ']')// there are only one ] that ends the declaration -> error
					throw new TOMLException("Missing character ] at line " + line);
			}
			
			// -- Reads the value (table content) --
			Map<String, Object> value = nextTableContent();
			
			// -- Saves the value --
			Map<String, Object> valueMap = map;// the map that contains the value
			for (int i = 0; i < keyParts.size() - 1; i++) {
				String part = keyParts.get(i);
				Map<String, Object> childMap = (Map) valueMap.get(part);
				if (childMap == null) {
					childMap = new HashMap<>(4);
					valueMap.put(part, childMap);
				}
				valueMap = childMap;
			}
			if (twoBrackets) {// element of a table array
				String name = keyParts.get(keyParts.size() - 1);
				Collection<Map> tableArray = (Collection) valueMap.get(name);
				if (tableArray == null) {
					tableArray = new ArrayList<>(2);
					valueMap.put(name, tableArray);
				}
				tableArray.add(value);
			} else {// just a table
				valueMap.put(keyParts.get(keyParts.size() - 1), value);
			}
			
		}
		return map;
	}
	
	private List nextArray() {
		ArrayList<Object> list = new ArrayList<>();
		while (true) {
			char c = nextUseful(true);
			if (c == ']') {
				pos++;
				break;
			}
			Object value = nextValue(c);
			if (!list.isEmpty() && !(list.get(0).getClass().isAssignableFrom(value.getClass())))
				throw new TOMLException("Invalid array at line " + line + ": all the values must have the same type");
			list.add(value);
			
			System.out.println("current: " + data.charAt(pos) + ", in: " + data.substring(pos - 10, pos + 10));
			char afterEntry = nextUseful(true);
			System.out.println("afterEntry: " + afterEntry);
			if (afterEntry == ']') {
				pos++;
				break;
			}
			if (afterEntry != ',') {
				throw new TOMLException("Invalid array at line " + line + ": expected a comma after each value");
			}
		}
		pos--;
		list.trimToSize();
		return list;
	}
	
	private Map<String, Object> nextInlineTable() {
		Map<String, Object> map = new HashMap<>();
		while (true) {
			char nameFirstChar = nextUsefulOrLinebreak();
			String name = null;
			switch (nameFirstChar) {
				case '}':
					return map;
				case '"': {
					if (pos + 1 < data.length()) {
						char c2 = data.charAt(pos);
						char c3 = data.charAt(pos + 1);
						if (c2 == '"' && c3 == '"') {
							pos += 2;
							name = nextBasicMultilineString();
						}
					}
					if (name == null)
						name = nextBasicString();
					break;
				}
				case '\'': {
					if (pos + 1 < data.length()) {
						char c2 = data.charAt(pos);
						char c3 = data.charAt(pos + 1);
						if (c2 == '\'' && c3 == '\'') {
							pos += 2;
							name = nextLiteralMultilineString();
						}
					}
					if (name == null)
						name = nextLiteralString();
					break;
				}
				default:
					name = nextBareKey();
					if (data.charAt(pos - 1) == '=')
						pos--;
					break;
			}
			
			char separator = nextUsefulOrLinebreak();
			if (separator != '=') {
				throw new TOMLException("Invalid data at line " + line);
			}
			
			char valueFirstChar = nextUsefulOrLinebreak();
			Object value = nextValue(valueFirstChar);
			map.put(name, value);
			
			char after = nextUsefulOrLinebreak();
			if (after == '}' || !hasNext()) {
				return map;
			} else if (after != ',') {
				throw new TOMLException("Invalid inline table at line " + line + ": missing comma");
			}
		}
	}
	
	private Map<String, Object> nextTableContent() {
		Map<String, Object> map = new HashMap<>();
		while (true) {
			char nameFirstChar = nextUseful(true);
			if (!hasNext() || nameFirstChar == '[') {
				return map;
			}
			String name = null;
			switch (nameFirstChar) {
				case '"': {
					if (pos + 1 < data.length()) {
						char c2 = data.charAt(pos);
						char c3 = data.charAt(pos + 1);
						if (c2 == '"' && c3 == '"') {
							pos += 2;
							name = nextBasicMultilineString();
						}
					}
					if (name == null) {
						name = nextBasicString();
					}
					break;
				}
				case '\'': {
					if (pos + 1 < data.length()) {
						char c2 = data.charAt(pos);
						char c3 = data.charAt(pos + 1);
						if (c2 == '\'' && c3 == '\'') {
							pos += 2;
							name = nextLiteralMultilineString();
						}
					}
					if (name == null) {
						name = nextLiteralString();
					}
					break;
				}
				default:
					pos--;
					name = nextBareKey();
					if (pos > 1 && data.charAt(pos - 1) == '=')
						pos--;
					break;
			}
			char separator = nextUseful(true);
			if (separator != '=') {
				throw new TOMLException("Invalid data at line " + line);
			}
			
			char valueFirstChar = nextUseful(false);
			Object value = nextValue(valueFirstChar);
			map.put(name, value);
		}
	}
	
	private Object nextNumberOrDate(char first) {
		boolean maybeDouble = true, maybeInteger = true, maybeDate = true;
		StringBuilder sb = new StringBuilder();
		sb.append(first);
		char c;
		whileLoop: while (hasNext()) {
			c = next();
			switch (c) {
				case ':':
				case 'T':
				case 'Z':
					maybeInteger = maybeDouble = false;
					break;
				case 'e':
				case 'E':
					maybeInteger = maybeDate = false;
					break;
				case '.':
					maybeInteger = false;
					break;
				case '-':
					if (c == '-' && pos != 0 && data.charAt(pos - 1) != 'e' && data.charAt(pos - 1) != 'E')
						maybeInteger = maybeDouble = false;
					break;
				case ',':
				case ' ':
				case '\t':
				case '\n':
				case '\r':
				case ']':
				case '}':
					pos--;
					break whileLoop;
			}
			if (c == '_')
				maybeDate = false;
			else
				sb.append(c);
		}
		String valueStr = sb.toString();
		if (maybeInteger)
			return (valueStr.length() < 10) ? Integer.parseInt(valueStr) : Long.parseLong(valueStr);
			
		if (maybeDouble)
			return Double.parseDouble(valueStr);
			
		if (maybeDate)
			return Toml.DATE_FORMATTER.parseBest(valueStr, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
			
		throw new TOMLException("Invalid value: " + valueStr + " at line " + line);
	}
	
	private String nextBareKey() {
		String keyName;
		for (int i = pos; i < data.length(); i++) {
			char c = data.charAt(i);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-')) {
				keyName = data.substring(pos, i);
				pos = i;
				return keyName;
			}
		}
		throw new TOMLException(
				"Invalid key/value pair at line " + line + " end of data reached before the value attached to the key was found");
	}
	
	private String nextLiteralString() {
		int index = data.indexOf('\'', pos);
		if (index == -1)
			throw new TOMLException("Invalid literal String at line " + line + ": it never ends");
			
		String str = data.substring(pos, index);
		if (str.indexOf('\n') != -1)
			throw new TOMLException("Invalid literal String at line " + line + ": newlines are not allowed here");
			
		pos = index + 1;
		return str;
	}
	
	private String nextLiteralMultilineString() {
		int index = data.indexOf("'''", pos);
		if (index == -1)
			throw new TOMLException("Invalid multiline literal String at line " + line + ": it never ends");
		String str;
		if (data.charAt(pos) == '\r' && data.charAt(pos + 1) == '\n') {// "\r\n" at the beginning of the string
			str = data.substring(pos + 2, index);
			line++;
		} else if (data.charAt(pos) == '\n') {// '\n' at the beginning of the string
			str = data.substring(pos + 1, index);
			line++;
		} else {
			str = data.substring(pos, index);
		}
		for (int i = 0; i < str.length(); i++) {// count lines
			char c = str.charAt(i);
			if (c == '\n')
				line++;
		}
		pos = index + 3;// goes after the 3 quotes
		return str;
	}
	
	private String nextBasicString() {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		while (hasNext()) {
			char c = next();
			if (c == '\n' || c == '\r')
				throw new TOMLException("Invalid basic String at line " + line + ": newlines not allowed");
			if (escape) {
				sb.append(unescape(c));
				escape = false;
			} else if (c == '\\') {
				escape = true;
			} else if (c == '"') {
				return sb.toString();
			} else {
				sb.append(c);
			}
		}
		throw new TOMLException("Invalid basic String at line " + line + ": it nerver ends");
	}
	
	private String nextBasicMultilineString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true, escape = false;
		while (hasNext()) {
			char c = next();
			if (first && (c == '\r' || c == '\n')) {
				if (c == '\r' && hasNext() && data.charAt(pos) == '\n')// "\r\n"
					pos++;// so that it is NOT read by the next call to next()
				else
					line++;
				first = false;
				continue;
			}
			if (escape) {
				if (c == '\r' || c == '\n') {
					if (c == '\r' && hasNext() && data.charAt(pos) == '\n')// "\r\n"
						pos++;
					else
						line++;
					nextUseful(false);
					pos--;// so that it is read by the next call to next()
				} else {
					sb.append(unescape(c));
				}
				escape = false;
			} else if (c == '\\') {
				escape = true;
			} else if (c == '"') {
				if (pos + 1 >= data.length())
					break;
				if (data.charAt(pos) == '"' && data.charAt(pos + 1) == '"') {
					pos += 2;
					return sb.toString();
				}
			} else if (c == '\n') {
				line++;
				sb.append(c);
			} else {
				sb.append(c);
			}
		}
		throw new TOMLException("Invalid multiline basic String at line " + line + ": it never ends");
	}
	
	private char unescape(char c) {
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
			case '"':
				return '"';
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
