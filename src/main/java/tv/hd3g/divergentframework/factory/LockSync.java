/*
 * This file is part of Divergent Framework Taskjob.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.divergentframework.factory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

public class LockSync {
	private static Logger log = Logger.getLogger(LockSync.class);
	
	private final ReentrantLock lock;
	private final Condition lock_condition;
	private boolean in_write_operation;
	
	public LockSync() {
		lock = new ReentrantLock();
		lock_condition = lock.newCondition();
	}
	
	public void syncRead(Runnable onLockDown) {
		syncRead(null, n -> {
			onLockDown.run();
			return null;
		});
	}
	
	public <T> T syncRead(Supplier<T> onLockDown) {
		Function<Void, T> supp = v -> {
			return onLockDown.get();
		};
		return syncRead(null, supp);
	}
	
	public <T> void syncRead(T value, Consumer<T> onLockDown) {
		Function<T, Void> cons = v -> {
			onLockDown.accept(v);
			return null;
		};
		syncRead(value, cons);
	}
	
	public void syncWrite(Runnable onLockDown) {
		syncWrite(null, n -> {
			onLockDown.run();
			return null;
		});
	}
	
	public <T> T syncWrite(Supplier<T> onLockDown) {
		Function<Void, T> supp = v -> {
			return onLockDown.get();
		};
		return syncWrite(null, supp);
	}
	
	public <T> void syncWrite(T value, Consumer<T> onLockDown) {
		Function<T, Void> cons = v -> {
			onLockDown.accept(v);
			return null;
		};
		syncWrite(value, cons);
	}
	
	public <T, R> R syncRead(T value, Function<T, R> onLockDown) {
		if (log.isTraceEnabled()) {
			log.trace("Want read");
		}
		lock.lock();
		
		try {
			while (in_write_operation) {
				lock_condition.await();
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Get read");
			}
			return onLockDown.apply(value);
		} catch (InterruptedException ie) {
			throw new RuntimeException("Cancel read", ie);
		} finally {
			lock.unlock();
		}
	}
	
	public <T, R> R syncWrite(T value, Function<T, R> onLockDown) {
		if (log.isTraceEnabled()) {
			log.trace("Want write");
		}
		lock.lock();
		
		try {
			while (in_write_operation) {
				lock_condition.await();
			}
			in_write_operation = true;
			
			if (log.isTraceEnabled()) {
				log.trace("Get write");
			}
			return onLockDown.apply(value);
		} catch (InterruptedException ie) {
			throw new RuntimeException("Cancel write", ie);
		} finally {
			in_write_operation = false;
			try {
				lock_condition.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}
	
	/*public void switchInWrite() {
	}
	
	public void releaseWrite() {
	}*/
}
