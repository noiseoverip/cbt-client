package com.cbt.installer;

import java.io.IOException;

import com.cbt.model.TestPackage;

public interface IApplicationInstaller {
	
	/**
	 * Install target application
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	void installApp() throws Exception;
	
	void installTest() throws Exception;

	void setTestPackage(TestPackage testPkg);

}
