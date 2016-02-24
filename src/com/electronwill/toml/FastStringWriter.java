/*
 * Copyright (C) 2015 ElectronWill
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.electronwill.toml;

import java.io.Writer;

/**
 * A Writer which writes in a StringBuilder. This is NOT Thread safe.
 */
public class FastStringWriter extends Writer {
	
	/**
	 * The underlying StringBuilder. Everything is appended to it.
	 */
	private final StringBuilder sb;
	
	/**
	 * Creates a new FastStringWriter with a default StringBuilder
	 */
	public FastStringWriter() {
		sb = new StringBuilder();
	}
	
	/**
	 * Creates a new FastStringWriter with a given StringBuilder. It will append everything to this StringBuilder.
	 *
	 * @param sb the StringBuilder
	 */
	public FastStringWriter(final StringBuilder sb) {
		this.sb = sb;
	}
	
	/**
	 * Returns the underlying StringBuilder.
	 *
	 * @return the underlying StringBuilder
	 */
	public StringBuilder getBuilder() {
		return sb;
	}
	
	/**
	 * Returns the content of the underlying StringBuilder, as a String. Equivalent to {@link #getBuilder()#toString()}.
	 *
	 * @return the content of the underlying StringBuilder
	 */
	@Override
	public String toString() {
		return sb.toString();
	}
	
	@Override
	public FastStringWriter append(char c) {
		sb.append(c);
		return this;
	}
	
	@Override
	public FastStringWriter append(CharSequence csq, int start, int end) {
		sb.append(csq, start, end);
		return this;
	}
	
	@Override
	public FastStringWriter append(CharSequence csq) {
		sb.append(csq);
		return this;
	}
	
	@Override
	public void write(String str, int off, int len) {
		sb.append(str, off, off + len);
	}
	
	@Override
	public void write(String str) {
		sb.append(str);
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) {
		sb.append(cbuf, off, len);
	}
	
	@Override
	public void write(int c) {
		sb.append(c);
	}
	
	/**
	 * This method does nothing.
	 */
	@Override
	public void flush() {}
	
	/**
	 * This method does nothing.
	 */
	@Override
	public void close() {}
	
}
