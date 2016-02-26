package com.electronwill.toml;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

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
	private int lineRead = 1;// the last line we read without incrementing the position
	
	public TomlReader(String data) {
		this.data = data;
	}
	
	private String readLiteralString() throws IOException {
		int index = indexOf('\'');
		if (index == -1)
			throw new IOException("Invalid literal String at line " + line + ": it never ends");
		String str = data.substring(pos, index);
		pos = index + 1;
		line = lineRead;
		return str;
	}
	
	private String readBasicString() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		while (true) {
			if (pos >= data.length())
				throw new IOException("Invalid basic String at line " + line + ": it nerver ends");
			char ch = data.charAt(pos++);
			if (ch == '\n')
				throw new IOException("Invalid basic String at line " + line + ": newlines not allowed");
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
	
	private String until(boolean acceptComments, char... cs) throws IOException {
		int index = indexOf(cs);
		if (index == -1)
			throw new IOException("Invalid data at line " + line + ": expected one of the following characters: " + Arrays.toString(cs));
			
		String str = data.substring(pos, index);
		if (!acceptComments && str.contains("#"))
			throw new IOException("Invalid comment at line " + line);
			
		line = lineRead;
		pos = index + 1;// after the character we stopped at
		return str;
	}
	
	private char unescape(char c) throws IOException {
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
			default:
				throw new IOException("Invalid escape sequence: \\" + c);
		}
	}
	
}
