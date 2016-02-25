package com.electronwill.toml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A SimpleReader reads characters by using a Reader object. It transforms every <tt>\r\n</tt> and <tt>\r</tt> (alone)
 * into <tt>\n</tt>. The {@link #lookAhead()} allows for getting the next character without incrementing the read
 * position.
 * 
 * @author TheElectronWill
 * 		
 */
public final class SimpleReader {
	
	private final Reader reader;
	private final Deque<Integer> nexts = new ArrayDeque<>();
	
	public SimpleReader(Reader reader) {
		this.reader = new BufferedReader(reader);// buffer for better performance
	}
	
	public SimpleReader(BufferedReader reader) {
		this.reader = reader;
	}
	
	/**
	 * Gets the next character without incrementing the position.
	 * 
	 * @return The next character, as an integer in the range 0 to 65535 (0x00-0xffff), or -1 if the end of the stream
	 *         has been reached
	 */
	public int lookAhead() throws IOException {
		Integer keeped = nexts.pollFirst();
		if (keeped == null) {
			keeped = reader.read();
			nexts.addLast(keeped);
		}
		return keeped;
	}
	
	public int lookAhead(char[] dest) throws IOException {
		int i = 0;
		for (; i < dest.length; i++) {
			int read = read();
			nexts.addLast(read);
			if (read == -1)
				break;
			dest[i] = (char) read;
		}
		return i;
	}
	
	/**
	 * Increments the position to the last character read with {@link #lookAhead()} or {@link #lookAhead(char[])}.
	 */
	public void jumpAhead() throws IOException {
		nexts.clear();
	}
	
	/**
	 * Reads the next character.
	 * 
	 * @return The next character, as an integer in the range 0 to 65535 (0x00-0xffff), or -1 if the end of the stream
	 *         has been reached
	 */
	public int read() throws IOException {
		int read;
		Integer keeped = nexts.pollFirst();
		if (keeped == null)
			read = reader.read();
		else
			read = keeped;
		if (read == '\r') {// maybe \r\n
			int read2 = reader.read();
			if (read2 != '\n')// oops, it's not \r\n
				nexts.addLast(read2);// keep it for later
			return '\n';
		}
		return read;
	}
	
	public int read(char[] dest) throws IOException {
		int i = 0;
		for (; i < dest.length; i++) {
			int read = read();
			if (read == -1)
				break;
			dest[i] = (char) read;
		}
		return i;
	}
	
	public void close() throws IOException {
		reader.close();
	}
	
	public void skip(int n) throws IOException {
		while (!nexts.isEmpty()) {
			nexts.pollFirst();
			n--;
		}
		reader.skip(n);
	}
	
}
