package org.apache.karaf.main.lock;

public class NoLock implements Lock {

	@Override
	public boolean lock() throws Exception {
		return true;
	}

	@Override
	public void release() throws Exception {
	}

	@Override
	public boolean isAlive() throws Exception {
		return true;
	}

}
