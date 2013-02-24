package com.cbt.cliexecutor;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;

public interface ICliExecutor {
	
	/**
	 * 
	 * @param command
	 * @return - Result code
	 * @throws ExecuteException
	 * @throws IOException
	 */
	int execute(CommandLine command) throws ExecuteException, IOException;
	
	String getOutput();
	
	boolean isFailure(int exitValue);

}
