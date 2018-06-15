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
package tv.hd3g.divergentframework.factory.watchfolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;

public class TestWatchfolder extends TestCase {
	
	public void testSimple() throws Exception {
		File observed_directory = Files.createTempDirectory("test-watchfolder").toFile().getCanonicalFile();
		
		WatchFolder wf = new WatchFolder().setObservedDirectory(observed_directory).setFileDetectionTime(10, TimeUnit.MILLISECONDS).setScanPeriod(100, TimeUnit.MILLISECONDS);
		
		assertEquals(observed_directory, wf.getObservedDirectory());
		assertEquals(100, wf.getScanPeriod(TimeUnit.MILLISECONDS));
		assertEquals(10, wf.getFileDetectionTime(TimeUnit.MILLISECONDS));
		assertFalse(wf.isScanInHiddenDirs());
		assertTrue(wf.isScanInSymboliclinkDirs());
		assertTrue(wf.isCallbackInFirstScan());
		assertFalse(wf.isClosed());
		
		File temp_file = new File(observed_directory.getPath() + File.separator + "testfile.txt");
		
		AtomicBoolean detect_ok = new AtomicBoolean(false);
		AtomicReference<Exception> last_error = new AtomicReference<Exception>(null);
		
		wf.registerCallback((root, activity_on_file, kind, detected_by) -> {
			if (EventKind.CREATE.equals(kind) == false) {
				last_error.set(new RuntimeException("Invalid event: " + kind + " != " + EventKind.CREATE + " > " + activity_on_file.getPath()));
			} else if (wf.equals(detected_by) == false) {
				last_error.set(new RuntimeException("Invalid event: bad wf"));
			} else if (activity_on_file.equals(temp_file) == false) {
				last_error.set(new RuntimeException("Invalid event: bad activity_on_file " + activity_on_file));
			} else if (observed_directory.equals(root) == false) {
				last_error.set(new RuntimeException("Invalid event: bad root " + root));
			}
			detect_ok.set(true);
		});
		
		FileUtils.write(temp_file, "Just a test, you can delete it\r\n", StandardCharsets.UTF_8);
		
		while (detect_ok.get() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		wf.close();
		
		assertTrue(wf.isClosed());
		
		Thread.sleep(150);
		
		FileUtils.forceDelete(observed_directory);
	}
	
	static class Evt {
		final File f;
		final EventKind kind;
		
		Evt(File f, EventKind kind) {
			this.f = f;
			this.kind = kind;
		}
		
		public boolean equals(File f, EventKind kind) {
			return this.f.equals(f) && this.kind.equals(kind);
		}
		
		public String toString() {
			return kind + " > " + f.getPath();
		}
	}
	
	static File createUpdateFile(File root, String path) throws IOException {
		File temp_file = new File(root.getPath() + File.separator + path);
		FileUtils.forceMkdirParent(temp_file);
		FileUtils.write(temp_file, "Just a test, you can delete it\r\n" + StringUtils.repeat("+", ThreadLocalRandom.current().nextInt(1, 100)) + "\r\n", StandardCharsets.UTF_8);
		return temp_file;
	}
	
	public void testFileTreeAndChange() throws Exception {
		File observed_directory = Files.createTempDirectory("test-watchfolder").toFile().getCanonicalFile();
		
		WatchFolder wf = new WatchFolder().setObservedDirectory(observed_directory).setFileDetectionTime(10, TimeUnit.MILLISECONDS).setScanPeriod(10, TimeUnit.MILLISECONDS);
		
		final LinkedBlockingQueue<Evt> expected_events = new LinkedBlockingQueue<>();
		AtomicReference<Exception> last_error = new AtomicReference<Exception>(null);
		
		wf.registerCallback((root, activity_on_file, kind, detected_by) -> {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
			}
			Evt next_evt = expected_events.poll();
			assertNotNull(next_evt);
			if (next_evt.equals(activity_on_file, kind) == false) {
				last_error.set(new RuntimeException("Invalid event: " + next_evt.toString() + ", instead of " + kind + " > " + activity_on_file.getPath()));
			}
			System.out.println(activity_on_file.getPath() + "\t" + kind);
		});
		
		/**
		 * Test create
		 */
		File f = createUpdateFile(observed_directory, "test1.txt");
		expected_events.add(new Evt(f, EventKind.CREATE));
		
		while (expected_events.isEmpty() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		/**
		 * Test update
		 */
		f = createUpdateFile(observed_directory, "test1.txt");
		expected_events.add(new Evt(f, EventKind.MODIFY));
		
		while (expected_events.isEmpty() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		/**
		 * Test delete
		 */
		FileUtils.forceDelete(f);
		expected_events.add(new Evt(f, EventKind.DELETE));
		
		while (expected_events.isEmpty() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		/**
		 * Test subdir
		 */
		f = createUpdateFile(observed_directory, "folder1" + File.separator + "simple.txt");
		expected_events.add(new Evt(f, EventKind.CREATE));
		
		while (expected_events.isEmpty() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		/**
		 * Test files on a subdir
		 */
		for (int pos = 0; pos < 10; pos++) {
			f = createUpdateFile(observed_directory, "folder1" + File.separator + "file" + (pos % 4) + ".txt");
			if (pos < 4) {
				expected_events.add(new Evt(f, EventKind.CREATE));
			} else {
				expected_events.add(new Evt(f, EventKind.MODIFY));
			}
			
			while (expected_events.isEmpty() == false) {
				Thread.onSpinWait();
			}
			if (last_error.get() != null) {
				last_error.get().printStackTrace();
				fail("Error with last event");
			}
		}
		
		/**
		 * Delete subdir
		 */
		wf.getPolicy().directories = true;
		
		File folder_1 = new File(observed_directory + File.separator + "folder1");
		expected_events.add(new Evt(folder_1, EventKind.DELETE));
		FileUtils.forceDelete(folder_1);
		
		while (expected_events.isEmpty() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		wf.close();
		FileUtils.forceDelete(observed_directory);
	}
	
	public void testgrowingFile() throws Exception {
		File observed_directory = Files.createTempDirectory("test-watchfolder").toFile().getCanonicalFile();
		
		WatchFolder wf = new WatchFolder().setObservedDirectory(observed_directory).setFileDetectionTime(20, TimeUnit.MILLISECONDS).setScanPeriod(50, TimeUnit.MILLISECONDS);
		
		File temp_file = new File(observed_directory.getPath() + File.separator + "growing_file.zero");
		
		AtomicBoolean detect_ok = new AtomicBoolean(false);
		AtomicReference<Exception> last_error = new AtomicReference<Exception>(null);
		
		long total_size = 100;
		
		wf.registerCallback((root, activity_on_file, kind, detected_by) -> {
			if (EventKind.CREATE.equals(kind) == false) {
				last_error.set(new RuntimeException("Invalid event: " + kind + " != " + EventKind.CREATE + " > " + activity_on_file.getPath()));
			} else if (total_size != activity_on_file.length()) {
				last_error.set(new RuntimeException("Invalid event total_size: " + activity_on_file.length() + " != " + total_size + " > " + activity_on_file.getPath()));
			}
			detect_ok.set(true);
		});
		
		Thread t = new Thread(() -> {
			FileOutputStream fos = null;
			try {
				fos = FileUtils.openOutputStream(temp_file);
				for (int pos = 0; pos < total_size; pos++) {
					Thread.sleep(2);
					fos.write(0);
					fos.flush();
				}
			} catch (Exception e) {
				last_error.set(new RuntimeException("Invalid writer thread", e));
			} finally {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(false);
		t.setPriority(Thread.MAX_PRIORITY);
		t.setName("Writer");
		t.start();
		
		while (detect_ok.get() == false) {
			Thread.onSpinWait();
		}
		if (last_error.get() != null) {
			last_error.get().printStackTrace();
			fail("Error with last event");
		}
		
		wf.close();
		FileUtils.forceDelete(observed_directory);
	}
	
	// TODO test callback_in_first_scan == true && test first detection (file present before start WF)
	// TODO test push conf twice
	// TODO test progressive growing file
	// TODO check if policy is correctly applied
	
	// TODO check all policies (in other test)
	
}
