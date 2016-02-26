package com.electronwill.toml;

/**
 * Thrown when a problem occur during parsing or writing NBT data.
 *
 * @author TheElectronWill
 */
public class TOMLException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public TOMLException() {}
	
	public TOMLException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TOMLException(String message) {
		super(message);
	}
	
	public TOMLException(Throwable cause) {
		super(cause);
	}
	
}
