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

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class TestWatchfolder extends TestCase {
	
	public void testService() throws Exception {
		WatchfolderService service = new WatchfolderService();
		
		File directory = Files.createTempDirectory("test-watchfolder").toFile().getCanonicalFile();
		
		WatchedDirectory w_d = service.watchDirectory(directory, new WatchfolderPolicy().setupRegularFiles(false), false, 0, TimeUnit.MILLISECONDS);
		
		assertEquals(directory, w_d.getObservedDirectory());
		assertEquals(0, w_d.getFileEventRetentionTime(TimeUnit.MILLISECONDS));
		assertFalse(w_d.isScanInHiddenDirs());
		assertTrue(w_d.isScanInSymboliclinkDirs());
		
		WatchfolderEvent event = (observed_directory, activity_on_file, kind, detected_by) -> {
			System.out.println(kind);// XXX
		};
		w_d.registerEventCallback(event);
		service.start();
		
		FileUtils.write(new File(directory.getPath() + File.separator + "testfile.txt"), "Just a test, you can delete it\r\n", StandardCharsets.UTF_8);
		
		service.stop();
		
		FileUtils.forceDelete(directory);
	}
	
	// XXX test callback_in_first_scan == true
	// XXX test file_event_retention_time > 0
}
