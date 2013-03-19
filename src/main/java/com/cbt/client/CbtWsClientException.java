package com.cbt.client;

/**
 * Base exception class for Cbt client exceptions
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class CbtWsClientException extends Exception {
	
	/**
	 * Auto-generated value
	 */
	private static final long serialVersionUID = 2889478159029254411L;

	public CbtWsClientException(String message, Throwable t) {
		super(message, t);
	}
	
	public CbtWsClientException(String message) {
		super(message);
	}
}
