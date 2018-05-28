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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import tv.hd3g.divergentframework.factory.Logtoolkit;

public class WatchfolderPolicy {
	
	private static Logger log = Logger.getLogger(WatchfolderPolicy.class);
	
	/**
	 * No directories, no symlinks, not empty, not desktop.ini .DS_Store and Thumbs.db.
	 */
	public final WatchfolderPolicy setupRegularFiles(boolean must_can_write) {
		directories = false;
		this.must_can_write = must_can_write;
		symlink = false;
		min_size = 1;
		
		/**
		 * Normally they can't pass here (hidden/system)
		 */
		black_list_file_name = new ArrayList<>(Arrays.asList("desktop.ini", ".DS_Store", "Thumbs.db"));
		
		return this;
	}
	
	public boolean directories = true;
	public boolean files = true;
	public boolean hidden = false;
	public boolean must_can_read = true;
	public boolean must_can_write = false;
	
	/**
	 * Ignored for directories
	 */
	public boolean must_can_execute_file = false;
	
	public boolean symlink = true;
	
	/**
	 * Windows status attr
	 */
	public boolean system = false;
	
	/**
	 * Excusive
	 * Ignored for directories
	 */
	public long min_size = 0;
	
	/**
	 * Inclusive
	 * Ignored for directories
	 */
	public long max_size = Long.MAX_VALUE;
	
	/**
	 * Delta with now
	 * Excusive
	 */
	public long min_age = Long.MIN_VALUE;
	
	/**
	 * Delta with now
	 * Inclusive
	 */
	public long max_age = Long.MAX_VALUE / 2;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> white_list_file_name;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> black_list_file_name;
	
	/**
	 * Empty/null = ignore
	 * WITHOUT the dot.
	 * Case insensitive.
	 */
	public ArrayList<String> white_list_file_extention;
	
	/**
	 * Empty/null = ignore
	 * WITHOUT the dot.
	 * Case insensitive.
	 */
	public ArrayList<String> black_list_file_extention;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> white_list_file_name_prefix;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> black_list_file_name_prefix;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> white_list_file_name_suffix;
	
	/**
	 * Empty/null = ignore
	 * Case insensitive.
	 */
	public ArrayList<String> black_list_file_name_suffix;
	
	/**
	 * Free feel to bypass this.
	 * Called after all tests
	 */
	public boolean specificFilter(File f, WatchFolder detector, EventKind kind) {
		return true;
	}
	
	private static final void l(String reason, File f, WatchFolder detector) {
		if (log.isTraceEnabled()) {
			log.trace("File not pass policy \"" + f.getPath().substring(detector.getObservedDirectory().getPath().length()) + "\" [" + reason + "] from " + detector);
		}
	}
	
	final boolean internalApplyFilter(File f, WatchFolder detector, EventKind kind) throws IOException {
		if (f.isDirectory() & directories == false) {
			l("Can't accept directories", f, detector);
			return false;
		} else if (f.isFile() & files == false) {
			l("Can't accept files", f, detector);
			return false;
		} else if ((f.isHidden() | f.getName().startsWith(".")) & hidden == false) {
			l("Can't accept hidden", f, detector);
			return false;
		} else if (f.canRead() == false & must_can_read) {
			l("Can't read", f, detector);
			return false;
		} else if (f.canWrite() == false & must_can_write) {
			l("Can't write", f, detector);
			return false;
		} else if (f.isDirectory() == false & f.canExecute() == false & must_can_execute_file) {
			l("Can't execute", f, detector);
			return false;
		} else if (f.isDirectory() == false & f.length() < min_size) {
			l("To small size, min is " + min_size, f, detector);
			return false;
		} else if (f.isDirectory() == false & f.length() >= max_size) {
			l("To big size, max is " + max_size, f, detector);
			return false;
		} else if (f.lastModified() < System.currentTimeMillis() - min_age) {
			l("To old, min is " + Logtoolkit.dateLog(System.currentTimeMillis() - min_age), f, detector);
			return false;
		} else if (f.lastModified() >= System.currentTimeMillis() + max_age) {
			l("To aged (in future), max is " + Logtoolkit.dateLog(System.currentTimeMillis() + max_age), f, detector);
			return false;
		}
		
		Path p = f.toPath();
		BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
		
		if (attrs.isSymbolicLink() && symlink == false) {
			l("No symbolic links", f, detector);
			return false;
		}
		
		if (system == false & SystemUtils.IS_OS_WINDOWS) {
			DosFileAttributes attr = Files.readAttributes(p, DosFileAttributes.class);
			if (attr.isSystem()) {
				l("No system attribute", f, detector);
				return false;
			}
		}
		
		if (checkCompliance(white_list_file_name, false, ext -> ext.equalsIgnoreCase(f.getName()))) {
			l("Can't pass white_list_file_name", f, detector);
			return false;
		} else if (checkCompliance(black_list_file_name, true, ext -> ext.equalsIgnoreCase(f.getName()))) {
			l("Can't pass black_list_file_name", f, detector);
			return false;
		} else if (checkCompliance(white_list_file_extention, false, ext -> ext.equalsIgnoreCase(FilenameUtils.getExtension(f.getName())))) {
			l("Can't pass white_list_file_extention", f, detector);
			return false;
		} else if (checkCompliance(black_list_file_extention, true, ext -> ext.equalsIgnoreCase(FilenameUtils.getExtension(f.getName())))) {
			l("Can't pass black_list_file_extention", f, detector);
			return false;
		} else if (checkCompliance(white_list_file_name_prefix, false, prefix -> f.getName().toLowerCase().startsWith(prefix.toLowerCase()))) {
			l("Can't pass white_list_file_name_prefix", f, detector);
			return false;
		} else if (checkCompliance(black_list_file_name_prefix, true, prefix -> f.getName().toLowerCase().startsWith(prefix.toLowerCase()))) {
			l("Can't pass black_list_file_name_prefix", f, detector);
			return false;
		} else if (checkCompliance(white_list_file_name_suffix, false, suffix -> FilenameUtils.getBaseName(f.getName()).toLowerCase().endsWith(suffix.toLowerCase()))) {
			l("Can't pass white_list_file_name_suffix", f, detector);
			return false;
		} else if (checkCompliance(black_list_file_name_suffix, true, suffix -> FilenameUtils.getBaseName(f.getName()).toLowerCase().endsWith(suffix.toLowerCase()))) {
			l("Can't pass black_list_file_name_suffix", f, detector);
			return false;
		}
		
		return specificFilter(f, detector, kind);
	}
	
	private static boolean checkCompliance(List<String> list, boolean black_list, Predicate<String> p) {
		if (list == null) {
			return false;
		} else if (list.isEmpty()) {
			return false;
		}
		
		if (black_list) {
			return list.stream().anyMatch(p);
		} else {
			return list.stream().noneMatch(p);
		}
	}
	
}
