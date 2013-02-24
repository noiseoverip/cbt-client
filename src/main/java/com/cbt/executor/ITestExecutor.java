package com.cbt.executor;

import java.io.IOException;

import org.apache.commons.exec.ExecuteException;

import com.cbt.model.TestPackage;

public interface ITestExecutor {
	void setTestPackage(TestPackage testPkg);
	void execute() throws ExecuteException, IOException, InterruptedException, Exception;	
}
