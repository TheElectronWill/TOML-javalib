package com.electronwill.toml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A CharacterIterator iterates through all the characters of some source of characters. It also transforms every
 * <tt>\r\n</tt> and <tt>\r</tt> (alone) into <tt>\n</tt>.
 * 
 * @author ElectronWill
 * 		
 */
public final class CharacterIterator {
	
	private final Reader reader;
	private int next = -2;// -2: no value, -1: EOF, >=0: a character
	
	public CharacterIterator(Reader reader) {
		this.reader = new BufferedReader(reader);// buffer for better performance
	}
	
	public CharacterIterator(BufferedReader reader) {
		this.reader = reader;
	}
	
	/**
	 * Gets the next character.
	 * 
	 * @return The next character, as an integer in the range 0 to 65535 (0x00-0xffff), or -1 if the end of the stream
	 *         has been reached
	 * @throws IOException
	 */
	public int next() throws IOException {
		int read;
		if (next != -2) {// give the keeped character, if any
			read = next;
			next = -2;
		} else {
			read = reader.read();
		}
		if (read == '\r') {// maybe \r\n
			int read2 = reader.read();
			if (read2 != '\n')// oops, it's not \r\n
				next = read2;// keep it for later
			return '\n';
		}
		return read;
	}
	
	public int next(char[] dest) throws IOException {
		int i = 0;
		for (; i < dest.length; i++) {
			int next = next();
			if (next == -1)
				break;
			dest[i] = (char) next;
		}
		return i;
	}
	
	public void close() throws IOException {
		reader.close();
	}
	
	public void skip(long n) throws IOException {
		reader.skip(n);
	}
	
}
