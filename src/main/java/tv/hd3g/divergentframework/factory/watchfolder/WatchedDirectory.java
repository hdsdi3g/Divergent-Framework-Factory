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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import tv.hd3g.divergentframework.factory.Logtoolkit;

public class WatchedDirectory {
	private static Logger log = Logger.getLogger(WatchedDirectory.class);
	
	private final ArrayList<WatchfolderEvent> callbacks;
	private final WatchfolderService service;
	private final boolean callback_in_first_scan;
	private final File observed_directory;
	private final WatchfolderPolicy policy;
	
	private long file_event_retention_time;
	private volatile boolean scan_in_hidden_dirs;
	private volatile boolean scan_in_symboliclink_dirs;
	
	WatchedDirectory(File observed_directory, WatchfolderPolicy policy, boolean callback_in_first_scan, long file_event_retention_time, WatchfolderService service) throws IOException {
		if (observed_directory == null) {
			throw new NullPointerException("\"observed_directory\" can't to be null");
		} else if (observed_directory.exists() == false) {
			throw new FileNotFoundException(observed_directory + " can't to be found");
		} else if (observed_directory.isDirectory() == false) {
			throw new FileNotFoundException(observed_directory + " is not a directory");
		} else if (observed_directory.canRead() == false) {
			throw new IOException(observed_directory + " can't to be read");
		}
		this.observed_directory = observed_directory.getCanonicalFile();
		
		this.policy = policy;
		this.service = service;
		if (service == null) {
			throw new NullPointerException("\"service\" can't to be null");
		}
		
		callbacks = new ArrayList<>();
		this.callback_in_first_scan = callback_in_first_scan;
		this.file_event_retention_time = file_event_retention_time;
		scan_in_hidden_dirs = false;
		scan_in_symboliclink_dirs = true;
	}
	
	public String toString() {
		return "Watch \"" + observed_directory + "\"";
	}
	
	public synchronized WatchedDirectory setFileEventRetentionTime(long retention_time, TimeUnit unit) {
		this.file_event_retention_time = unit.toMillis(retention_time);
		return this;
	}
	
	public synchronized long getFileEventRetentionTime(TimeUnit unit) {
		return unit.convert(file_event_retention_time, TimeUnit.MILLISECONDS);
	}
	
	public boolean isScanInHiddenDirs() {
		return scan_in_hidden_dirs;
	}
	
	public WatchedDirectory setScanInHiddenDirs(boolean scan_in_hidden_dirs) {
		this.scan_in_hidden_dirs = scan_in_hidden_dirs;
		return this;
	}
	
	public boolean isScanInSymboliclinkDirs() {
		return scan_in_symboliclink_dirs;
	}
	
	public WatchedDirectory setScanInSymboliclinkDirs(boolean scan_in_symboliclink_dirs) {
		this.scan_in_symboliclink_dirs = scan_in_symboliclink_dirs;
		return this;
	}
	
	public File getObservedDirectory() {
		return observed_directory;
	}
	
	// TODO trace activity !
	
	private void asyncRecursiveScan(File directory, Consumer<File> onFile, Consumer<File> onDir) {
		service.getExecutor().execute(() -> {
			Arrays.asList(directory.listFiles()).forEach(f -> {
				if (f.isDirectory()) {
					if (scan_in_hidden_dirs == false) {
						if (f.isHidden() | f.getName().startsWith(".")) {
							return;
						}
					} else if (scan_in_symboliclink_dirs == false) {
						try {
							BasicFileAttributes attrs = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
							if (attrs.isSymbolicLink()) {
								return;
							}
						} catch (IOException e) {
							log.warn("Can't read attributes for \"" + f + "\"", e);
						}
					}
					
					if (f.canRead() == false) {
						log.debug("Can't read \"" + f + "\"");
						return;
					} else if (f.canExecute() == false) {
						log.debug("Can't read (execute) \"" + f + "\"");
						return;
					}
					onDir.accept(f);
					asyncRecursiveScan(f, onFile, onDir);
				} else {
					onFile.accept(f);
				}
			});
		});
	}
	
	public WatchedDirectory registerEventCallback(WatchfolderEvent event_callback) throws IOException {
		synchronized (callbacks) {
			boolean start_scan = callbacks.isEmpty();
			callbacks.add(event_callback);
			
			if (start_scan) {
				service.addToScanDirectoryListInLocalFileSystem(observed_directory, this);
				
				asyncRecursiveScan(observed_directory, file -> {
					if (callback_in_first_scan) {
						onEvent(file, EventKind.FIRST_DETECTION);
					}
				}, dir -> {
					service.addToScanDirectoryListInLocalFileSystem(observed_directory, this);
					if (callback_in_first_scan) {
						onEvent(dir, EventKind.FIRST_DETECTION);
					}
				});
			}
		}
		return this;
	}
	
	public boolean unRegisterEventCallback(WatchfolderEvent event_callback) {
		synchronized (callbacks) {
			return callbacks.remove(event_callback);
		}
	}
	
	// TODO2 switch in internal/external (with db)
	
	void onEvent(File event_file, EventKind kind) {
		if (callbacks.isEmpty()) {
			return;
		}
		
		if (file_event_retention_time > 0) {
			final long actual_size = event_file.length();
			final long actual_date = event_file.lastModified();
			final boolean isdir = event_file.isDirectory();
			
			service.getSchService().schedule(() -> {
				if (event_file.exists() == false) {
					log.trace("Lost file: " + event_file);
					return;
				} else if (event_file.isDirectory() != isdir) {
					if (event_file.isDirectory()) {
						log.trace("File is now a directory ! " + event_file);
					} else {
						log.trace("Directory is now a regular file ! " + event_file);
					}
					return;
				} else if (event_file.length() != actual_size) {
					log.trace("File is modified: " + event_file + " (" + actual_size + " and now " + event_file.length() + ")");
					return;
				} else if (event_file.lastModified() != actual_date) {
					log.trace("File is modified: " + event_file + " (" + Logtoolkit.dateLog(actual_date) + " and now " + Logtoolkit.dateLog(event_file.lastModified()) + ")");
					return;
				}
				asyncDispatchEvent(event_file, kind);
			}, file_event_retention_time, TimeUnit.MILLISECONDS);
		} else {
			asyncDispatchEvent(event_file, kind);
		}
		
		if (event_file.isDirectory()) {
			/**
			 * Register or remove register for a directory, and sub dirs
			 */
			if (kind == EventKind.CREATE) {
				service.addToScanDirectoryListInLocalFileSystem(event_file, this);
				asyncRecursiveScan(event_file, f -> {
				}, d -> {
					service.addToScanDirectoryListInLocalFileSystem(d, this);
				});
			} else if (kind == EventKind.DELETE) {
				service.removeToScanDirectoryListInLocalFileSystem(event_file, this);
			}
		}
	}
	
	private void asyncDispatchEvent(File validated_event_file, EventKind kind) {
		try {
			if (policy.internalApplyFilter(validated_event_file, this, kind) == false) {
				log.debug("File activity, but not in policy: " + validated_event_file + " (" + kind + ")");
				return;
			}
		} catch (IOException e) {
			log.error("Can't validate file " + validated_event_file, e);
			return;
		}
		
		log.debug("File activity: " + validated_event_file + " (" + kind + ")");
		
		callbacks.forEach(c -> {
			service.getExecutor().execute(() -> {
				try {
					c.onEvent(observed_directory, validated_event_file, kind, this);
				} catch (Exception e) {
					log.error("Can't propagate event for " + validated_event_file + "(" + c + ")", e);
				}
			});
		});
	}
	
}
