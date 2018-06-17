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

import junit.framework.TestCase;

/**
 * Lazy unit test
 */
public class TestWatchfolderPolicy extends TestCase {
	
	public void testDefaultPolicy() {
		WatchfolderPolicy p = new WatchfolderPolicy();
		p.setupRegularFiles(false);
		
		assertFalse(p.directories);
		assertFalse(p.must_can_write);
		assertFalse(p.symlink);
		assertEquals(1, p.min_size);
		assertNotNull(p.black_list_file_name);
		assertTrue(p.black_list_file_name.contains("desktop.ini"));
		assertTrue(p.black_list_file_name.contains(".DS_Store"));
		assertTrue(p.black_list_file_name.contains("Thumbs.db"));
		
		p.setupRegularFiles(true);
		assertTrue(p.must_can_write);
	}
	
	/*
	public boolean directories = true;
	public boolean files = true;
	public boolean hidden = false;
	public boolean must_can_read = true;
	public boolean must_can_write = false;
	
	 * If true, delete events will be ignored.
	public boolean must_exists = false;
	
	 * Ignored for directories
	public boolean must_can_execute_file = false;
	
	public boolean symlink = true;
	
	 * Windows status attr
	public boolean system = false;
	
	 * Excusive
	 * Ignored for directories
	public long min_size = 0;
	
	 * Inclusive
	 * Ignored for directories
	public long max_size = Long.MAX_VALUE;
	
	 * Delta with now
	 * Excusive
	public long min_age = Long.MIN_VALUE;
	
	 * Delta with now
	 * Inclusive
	public long max_age = Long.MAX_VALUE / 2;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> white_list_file_name;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> black_list_file_name;
	
	 * Empty/null = ignore
	 * WITHOUT the dot.
	 * Case insensitive.
	public ArrayList<String> white_list_file_extention;
	
	 * Empty/null = ignore
	 * WITHOUT the dot.
	 * Case insensitive.
	public ArrayList<String> black_list_file_extention;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> white_list_file_name_prefix;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> black_list_file_name_prefix;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> white_list_file_name_suffix;
	
	 * Empty/null = ignore
	 * Case insensitive.
	public ArrayList<String> black_list_file_name_suffix;
	
	 * Free feel to bypass this.
	 * Called after all tests
	public boolean applySpecificFilter(File f, WatchFolder detector, EventKind kind) {
		return true;
	}
	*/
}
