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
import java.lang.Thread.State;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

/**
 * Scan filesystem activity
 */
public class WatchfolderService {
	private static Logger log = Logger.getLogger(WatchfolderService.class);
	
	private final ArrayList<WatchedDirectory> watched_directory_list;
	private volatile boolean want_to_stop;
	private final ThreadPoolExecutor executor;
	private final ScheduledExecutorService sch_service;
	private final Thread service;
	private final WatchService watcher;
	private final HashMap<File, List<WatchedDirectory>> local_watched_dirs;
	private final HashMap<File, WatchKey> all_active_watchkeys_by_directories;
	private final HashMap<WatchKey, File> all_active_directories_by_watchkeys;
	
	public WatchfolderService() throws IOException {
		watched_directory_list = new ArrayList<>();
		
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		executor.setThreadFactory(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("WatchfolderFileEvent");
			t.setPriority(Thread.MIN_PRIORITY + 1);
			return t;
		});
		
		sch_service = Executors.newScheduledThreadPool(1, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("WatchfolderRegularEvent");
			t.setPriority(Thread.MIN_PRIORITY + 1);
			return t;
		});
		
		service = new Thread(new Service(), "WatchfolderService");
		service.setPriority(Thread.MIN_PRIORITY);
		service.setDaemon(false);
		
		watcher = FileSystems.getDefault().newWatchService();
		local_watched_dirs = new HashMap<>();
		all_active_watchkeys_by_directories = new HashMap<>();
		all_active_directories_by_watchkeys = new HashMap<>();
	}
	
	private Optional<WatchedDirectory> getFromDir(File new_directory) {
		return watched_directory_list.stream().filter(watched_directory -> {
			return watched_directory.getObservedDirectory().equals(new_directory);
		}).findFirst();
	}
	
	// TODO trace activity !
	
	public WatchedDirectory watchDirectory(File directory, WatchfolderPolicy policy, boolean callback_in_first_scan, long file_event_retention_time, TimeUnit unit) throws IOException {
		if (directory == null) {
			throw new NullPointerException("\"directory\" can't to be null");
		} else if (policy == null) {
			throw new NullPointerException("\"policy\" can't to be null");
		}
		
		File new_directory = directory.getCanonicalFile();
		
		synchronized (watched_directory_list) {
			Optional<WatchedDirectory> o_previously_added_watched_directory = getFromDir(new_directory);
			
			if (o_previously_added_watched_directory.isPresent()) {
				return o_previously_added_watched_directory.get();
			} else {
				WatchedDirectory new_watched_directory = new WatchedDirectory(new_directory, policy, callback_in_first_scan, unit.toMillis(file_event_retention_time), this);
				watched_directory_list.add(new_watched_directory);
				return new_watched_directory;
			}
		}
	}
	
	ThreadPoolExecutor getExecutor() {
		return executor;
	}
	
	ScheduledExecutorService getSchService() {
		return sch_service;
	}
	
	/**
	 * All pending events for this directory will be triggered, but no new scans will be added.
	 */
	public boolean unWatchDirectory(File directory) {
		synchronized (watched_directory_list) {
			Optional<WatchedDirectory> o_previously_added_watched_directory = getFromDir(directory);
			if (o_previously_added_watched_directory.isPresent() == false) {
				return false;
			}
			
			return watched_directory_list.remove(o_previously_added_watched_directory.get());
		}
	}
	
	public synchronized WatchfolderService start() {
		if (service.isAlive()) {
			return this;
		}
		
		service.start();
		
		want_to_stop = false;
		return this;
	}
	
	public synchronized WatchfolderService stop() {
		if (service.isAlive() == false) {
			return this;
		}
		log.info("Ask to stop all watchfolder(s)");
		
		want_to_stop = true;
		
		if (service.getState() == State.WAITING) {
			service.interrupt();
		}
		
		while (service.isAlive()) {
			Thread.onSpinWait();
		}
		
		sch_service.shutdown();
		try {
			sch_service.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.warn("Can't wait executor shutdown", e);
		}
		
		executor.getQueue().clear();
		executor.shutdown();
		try {
			executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.warn("Can't wait executor shutdown", e);
		}
		
		return this;
	}
	
	/**
	 * Non recursive. No one checks will be made.
	 */
	void addToScanDirectoryListInLocalFileSystem(File directory, WatchedDirectory asked_by) {
		synchronized (local_watched_dirs) {
			if (local_watched_dirs.containsKey(directory) == false) {
				local_watched_dirs.put(directory, new ArrayList<>(1));
				synchronized (all_active_watchkeys_by_directories) {
					try {
						WatchKey new_wk = directory.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);
						all_active_watchkeys_by_directories.put(directory, new_wk);
						all_active_directories_by_watchkeys.put(new_wk, directory);
					} catch (IOException e) {
						throw new RuntimeException("Can't register " + directory, e);
					}
				}
			}
			
			local_watched_dirs.get(directory).add(asked_by);
		}
	}
	
	/**
	 * Recursive. No one checks will be made.
	 */
	void removeToScanDirectoryListInLocalFileSystem(File directory, WatchedDirectory asked_by) {
		synchronized (local_watched_dirs) {
			List<WatchedDirectory> wdir_list = local_watched_dirs.get(directory);
			if (wdir_list == null) {
				/**
				 * Do nothing.
				 */
			} else if (wdir_list.isEmpty()) {
				local_watched_dirs.remove(directory);
			} else if (wdir_list.size() == 1) {
				if (wdir_list.get(0).equals(asked_by)) {
					local_watched_dirs.remove(directory);
					
					synchronized (all_active_watchkeys_by_directories) {
						WatchKey old_wk = all_active_watchkeys_by_directories.remove(directory);
						if (old_wk != null) {
							all_active_directories_by_watchkeys.remove(old_wk);
							old_wk.cancel();
						}
					}
				}
			} else {
				wdir_list.remove(asked_by);
			}
			
			if (directory.exists() == false) {
				local_watched_dirs.keySet().stream().filter(d -> {
					return d.getAbsolutePath().startsWith(directory.getAbsolutePath());
				}).collect(Collectors.toList()).forEach(d -> {
					local_watched_dirs.remove(d);
				});
			}
		}
	}
	
	private class Service implements Runnable {
		
		public void run() {
			try {
				while (want_to_stop == false) {
					/*if (all_active_directories_by_watchkeys.isEmpty()) {
						Thread.onSpinWait();
						continue;
					}*/
					
					WatchKey key = watcher.take();
					
					File scanned_root_dir = all_active_directories_by_watchkeys.get(key);
					
					log.debug("Found activity in " + scanned_root_dir + ", " + key.pollEvents().size() + " event(s)");
					
					key.pollEvents().stream().forEach(event -> {
						WatchEvent.Kind<?> kind = event.kind();
						
						if (kind == StandardWatchEventKinds.OVERFLOW) {
							return;
						}
						
						@SuppressWarnings("unchecked")
						Path filename = ((WatchEvent<Path>) event).context();
						
						File event_from_file = scanned_root_dir.toPath().resolve(filename).toFile();
						
						if (kind == StandardWatchEventKinds.ENTRY_MODIFY && event_from_file.isDirectory()) {
							log.warn("MODIFY a directory ?! " + event_from_file);
						} else {
							log.warn("Unknown event " + kind + " for " + event_from_file);
							return;
						}
						
						local_watched_dirs.get(scanned_root_dir).forEach(wd -> {
							wd.onEvent(event_from_file, EventKind.onWatchEvent(kind));
						});
					});
					
					if (key.reset() == false) {
						break;
					}
				}
			} catch (InterruptedException e) {
				log.error("Cancel watchfolder in local file systems", e);
			}
		}
		
	}
	
}
