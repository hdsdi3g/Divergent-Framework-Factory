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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class TestWatchfolder extends TestCase {
	
	public void testService() throws Exception {
		
		File observed_directory = Files.createTempDirectory("test-watchfolder").toFile().getCanonicalFile();
		
		WatchFolder wf = new WatchFolder().setObservedDirectory(observed_directory).setFileDetectionTime(10, TimeUnit.MILLISECONDS).setScanPeriod(1, TimeUnit.SECONDS);
		
		assertEquals(observed_directory, wf.getObservedDirectory());
		assertEquals(1, wf.getScanPeriod(TimeUnit.SECONDS));
		assertEquals(10, wf.getFileDetectionTime(TimeUnit.MILLISECONDS));
		assertFalse(wf.isScanInHiddenDirs());
		assertTrue(wf.isScanInSymboliclinkDirs());
		assertTrue(wf.isCallbackInFirstScan());
		assertFalse(wf.isClosed());
		
		AtomicBoolean detect_ok = new AtomicBoolean(false);
		wf.registerCallback((root, activity_on_file, kind, detected_by) -> {
			System.out.println(activity_on_file.getPath() + "\t" + kind);
			detect_ok.set(true);
		});
		
		FileUtils.write(new File(observed_directory.getPath() + File.separator + "testfile.txt"), "Just a test, you can delete it\r\n", StandardCharsets.UTF_8);
		
		while (detect_ok.get() == false) {
			Thread.onSpinWait();
		}
		
		// FIXME no FIRST_DETECTION ! (double Update directory pass)
		
		FileUtils.forceDelete(observed_directory);
	}
	
	// TODO test callback_in_first_scan == true
	
	// TODO test stop
	// TODO test push conf twice
	
}
