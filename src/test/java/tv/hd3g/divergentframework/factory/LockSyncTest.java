/*
 * This file is part of Divergent Framework Factory.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import junit.framework.TestCase;

public class LockSyncTest extends TestCase {
	
	public void test1() throws InterruptedException {
		LockSync ls = new LockSync();
		
		assertTrue(ls.syncRead(() -> {
			return true;
		}));
		
		assertTrue(ls.syncWrite(() -> {
			return true;
		}));
	}
	
	public void test2() {
		LockSync ls = new LockSync();
		
		AtomicBoolean in_write = new AtomicBoolean(false);
		
		IntStream.range(0, 1_000_000).parallel().forEach(i -> {
			if (i % 1000 == 0) {
				ls.syncWrite(() -> {
					// System.out.println("W+");
					in_write.set(true);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
					}
					in_write.set(false);
					// System.out.println("W-");
				});
			} else {
				ls.syncRead(() -> {
					// System.out.println("R+");
					if (in_write.get()) {
						throw new RuntimeException("Invalid state");
					}
					// System.out.println("R-");
				});
			}
		});
		
	}
	
}
