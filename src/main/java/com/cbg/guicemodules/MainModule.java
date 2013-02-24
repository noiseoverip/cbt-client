package com.cbg.guicemodules;

import com.cbt.client.CbtClient;
import com.cbt.cliexecutor.CliExecutor;
import com.cbt.cliexecutor.ICliExecutor;
import com.cbt.executor.ITestExecutor;
import com.cbt.executor.TestExecutor;
import com.cbt.installer.ApplicationInstaller;
import com.cbt.installer.IApplicationInstaller;
import com.google.inject.AbstractModule;

public class MainModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(CbtClient.class);
		bind(IApplicationInstaller.class).to(ApplicationInstaller.class);
		bind(ICliExecutor.class).to(CliExecutor.class);
		bind(ITestExecutor.class).to(TestExecutor.class);
	}
	
}
