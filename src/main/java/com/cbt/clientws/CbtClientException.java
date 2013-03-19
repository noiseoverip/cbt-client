package com.cbt.clientws;

/**
 * Base exception class for Cbt client exceptions
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class CbtClientException extends Exception {
	
	/**
	 * Auto-generated value
	 */
	private static final long serialVersionUID = 2889478159029254411L;

	public CbtClientException(String message, Throwable t) {
		super(message, t);
	}
	
	public CbtClientException(String message) {
		super(message);
	}
}
