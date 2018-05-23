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
import java.util.ArrayList;

public class WatchfolderPolicy {
	
	public boolean directories = true;
	public boolean files = true;
	public boolean hidden = true;
	public boolean must_can_read = true;
	public boolean must_can_write = false;
	public boolean must_can_execute_file = true;
	
	public boolean symlink = true;
	public boolean transform_symlink_to_realfile = false;
	
	/**
	 * Windows status attr
	 */
	public boolean system = false; // https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html#dos
	
	public long min_size = 0;
	public long max_size = Long.MAX_VALUE;
	
	/**
	 * Delta with now
	 */
	public long min_age = Long.MIN_VALUE;
	
	/**
	 * Delta with now
	 */
	public long max_age = Long.MAX_VALUE;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> white_list_file_extentions;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> black_list_file_extentions;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> white_list_file_name_prefix;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> black_list_file_name_prefix;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> white_list_file_name_suffix;
	
	/**
	 * Empty/null = ignore
	 */
	public ArrayList<String> black_list_file_name_suffix;
	
	/**
	 * Free feel to bypass this.
	 * Called after all tests
	 */
	public boolean specificFilter(File f, WatchedDirectory detector, EventKind kind) {
		return true;
	}
	
	final boolean internalApplyFilter(File f, WatchedDirectory detector, EventKind kind) {
		// XXX apply all
		return specificFilter(f, detector, kind);
	}
	
}
