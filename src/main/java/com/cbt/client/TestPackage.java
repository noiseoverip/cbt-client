package com.cbt.client;

public class TestPackage {

	private String mAppPackageName;
	private String mAppFileName;
	private String mTestFileName;

	/**
	 * Test class name to execute
	 */
	private String mTestClassName;

	public String getTestClassName() {
		return mTestClassName;
	}

	public void setTestClassName(String testClassName) {
		mTestClassName = testClassName;
	}

	public String getTestFileName() {
		return mTestFileName;
	}

	public void setTestFileName(String testFileName) {
		mTestFileName = testFileName;
	}

	public String getAppFileName() {
		return mAppFileName;
	}

	public String getAppPackageName() {
		return mAppPackageName;
	}

	public void setAppPackageName(String packageName) {
		this.mAppPackageName = packageName;
	}

	public void setAppFileName(String fileName) {
		mAppFileName = fileName;
	}

}
