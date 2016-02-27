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
 * 
 * @author TheElectronWill
 * 		
 */
public final class TomlReader {
	
	private final String data;
	private int pos = 0;// current position
	
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
				pos = (nextLinebreak == -1) ? data.length() : nextLinebreak + 1;
			}
		}
		return c;
	}
	
	private char nextUsefulOrLinebreak() {
		char c = ' ';
		while (c == ' ' || c == '\t')
			c = next();
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
					if (c2 == '"' && c3 == '"') {
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
					throw new TOMLException("Invalid value at pos " + pos);
				}
				return true;
			case 'f':// Must be "false"
				if (pos + 4 >= data.length() || next() != 'a' || next() != 'l' || next() != 's' || next() != 'e') {
					throw new TOMLException("Invalid value at pos " + pos);
				}
				return false;
			default:
				throw new TOMLException("Invalid character at pos " + pos);
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
					throw new TOMLException("Invalid table declaration at pos " + pos + ": not enough data");
				char next = nextUsefulOrLinebreak();
				switch (next) {
					case '"': {
						String current = keyBuilder.toString().trim();
						if (current.length() > 0) {
							keyParts.add(current);
							keyBuilder.setLength(0);
						}
						keyParts.add(nextBasicString());
						pos--;
						break;
					}
					case '\'': {
						String current = keyBuilder.toString().trim();
						if (current.length() > 0) {
							keyParts.add(current);
							keyBuilder.setLength(0);
						}
						keyParts.add(nextLiteralString());
						pos--;
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
							throw new TOMLException("Invalid table name at pos " + pos + ": empty value are not allowed here");
						}
						keyParts.add(current);
						keyBuilder.setLength(0);
						break;
					}
					case '#':
						throw new TOMLException("Invalid table name at pos " + pos + ": comments are not allowed here");
					case '\n':
					case '\r':
						throw new TOMLException("Invalid table name at pos " + pos + ": line breaks are not allowed here");
					default:
						keyBuilder.append(next);
						break;
				}
			}
			
			// -- Check --
			if (twoBrackets) {
				if (next() != ']')// there are only one ] that ends the declaration -> error
					throw new TOMLException("Missing character ] at pos " + pos);
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
				throw new TOMLException("Invalid array at pos " + pos + ": all the values must have the same type");
			list.add(value);
			
			pos--;
			char afterEntry = nextUseful(true);
			if (afterEntry == ']') {
				pos++;
				break;
			}
			if (afterEntry != ',') {
				throw new TOMLException("Invalid array at pos " + pos + ": expected a comma after each value");
			}
		}
		list.trimToSize();
		return list;
	}
	
	private Map<String, Object> nextInlineTable() {
		Map<String, Object> map = new HashMap<>();
		while (true) {
			char nameFirstChar = nextUsefulOrLinebreak();
			String name;
			switch (nameFirstChar) {
				case '}':
					return map;
				case '"': {
					char c2 = next(), c3 = next();
					name = (c2 == '"' && c3 == '"') ? nextBasicMultilineString() : nextBasicString();
					break;
				}
				case '\'': {
					char c2 = next(), c3 = next();
					name = (c2 == '\'' && c3 == '\'') ? nextLiteralMultilineString() : nextLiteralString();
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
				throw new TOMLException("Invalid key at pos " + pos);
			}
			
			char valueFirstChar = nextUsefulOrLinebreak();
			Object value = nextValue(valueFirstChar);
			map.put(name, value);
			
			char after = nextUsefulOrLinebreak();
			if (after == '}' || !hasNext()) {
				return map;
			} else if (after != ',') {
				throw new TOMLException("Invalid inline table at pos " + pos + ": missing comma");
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
						if (c2 == '"' && c3 == '"') {
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
					if (data.charAt(pos - 1) == '=')
						pos--;
					break;
			}
			char separator = nextUseful(true);
			if (separator != '=') {
				throw new TOMLException("Invalid key at pos " + pos);
			}
			
			char valueFirstChar = nextUseful(true);
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
			
		throw new TOMLException("Invalid value: " + valueStr + " at pos " + pos);
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
				"Invalid key/value pair at pos " + pos + " end of data reached before the value attached to the key was found");
	}
	
	private String nextLiteralString() {
		int index = data.indexOf('\'');
		if (index == -1)
			throw new TOMLException("Invalid literal String at pos " + pos + ": it never ends");
		String str = data.substring(pos, index);
		pos = index + 1;
		return str;
	}
	
	private String nextLiteralMultilineString() {
		int index = data.indexOf("'''");
		if (index == -1)
			throw new TOMLException("Invalid multiline literal String at pos " + pos + ": it never ends");
		String str;
		if (data.charAt(pos) == '\r' && data.charAt(pos) == '\n') {
			str = data.substring(pos + 2, index);
		} else if (data.charAt(pos) == '\n' || data.charAt(pos) == '\r') {
			str = data.substring(pos + 1, index);
		} else {
			str = data.substring(pos, index);
		}
		pos = index + 1;
		return str;
	}
	
	private String nextBasicString() {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		while (hasNext()) {
			char c = next();
			if (c == '\n' || c == '\r')
				throw new TOMLException("Invalid basic String at pos " + pos + ": newlines not allowed");
			if (escape) {
				sb.append(unescape(c));
				escape = false;
			} else if (c == '\\') {
				escape = true;
			} else if (c == '"') {
				pos++;
				return sb.toString();
			} else {
				sb.append(c);
			}
		}
		throw new TOMLException("Invalid basic String at pos " + pos + ": it nerver ends");
	}
	
	private String nextBasicMultilineString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		boolean escape = false;
		while (hasNext()) {
			char c = next();
			if (first && (c == '\r' || c == '\n')) {
				first = false;
				continue;
			}
			if (escape) {
				if (c == '\r' || c == '\n') {
					if (c == '\r' && hasNext() && data.charAt(pos) == '\n')
						pos++;
					nextUseful(false);
					pos--;// so that it is read by the next call to next()
				} else {
					sb.append(unescape(c));
				}
				escape = false;
			} else if (c == '\\') {
				escape = true;
				continue;
			} else if (c == '"') {
				if (pos + 1 >= data.length())
					break;
				if (data.charAt(pos) == '"' && data.charAt(pos + 1) == '"') {
					pos += 2;
					return sb.toString();
				}
				continue;
			}
			sb.append(c);
			
		}
		throw new TOMLException("Invalid multiline basic String at pos " + pos + ": it never ends");
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
