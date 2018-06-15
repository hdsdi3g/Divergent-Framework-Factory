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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import tv.hd3g.divergentframework.factory.Logtoolkit;
import tv.hd3g.divergentframework.factory.configuration.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnAfterUpdateConfiguration;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnBeforeRemovedInConfiguration;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnBeforeUpdateConfiguration;
import tv.hd3g.divergentframework.factory.configuration.validation.FileIsDirectoryAndExistsValidator;
import tv.hd3g.divergentframework.factory.configuration.validation.ForbiddenConfiguratorValidator;
import tv.hd3g.divergentframework.factory.configuration.validation.NotEmptyNotZeroValidator;
import tv.hd3g.divergentframework.factory.configuration.validation.NotNullValidator;

public class WatchFolder {
	private static Logger log = Logger.getLogger(WatchFolder.class);
	
	private final ArrayList<WatchfolderEvent> callbacks;
	private final ConcurrentHashMap<File, WatchedDirectory> watched_directories; // XXX external storage
	private final AtomicBoolean pending_configuration_update;
	@ConfigurableValidator(ForbiddenConfiguratorValidator.class)
	private volatile boolean closed;
	
	@ConfigurableValidator(FileIsDirectoryAndExistsValidator.class)
	private File observed_directory;
	
	@ConfigurableValidator(NotNullValidator.class)
	private WatchfolderPolicy policy;
	
	@ConfigurableValidator(NotEmptyNotZeroValidator.class)
	private long file_detection_time;
	@ConfigurableValidator(NotEmptyNotZeroValidator.class)
	private long scan_period;
	
	private boolean scan_in_hidden_dirs;
	private boolean scan_in_symboliclink_dirs;
	private boolean search_in_subfolders;
	private boolean callback_in_first_scan;
	
	@ConfigurableValidator(ForbiddenConfiguratorValidator.class)
	private ThreadPoolExecutor executor;
	@ConfigurableValidator(ForbiddenConfiguratorValidator.class)
	private ScheduledExecutorService sch_service;
	
	public WatchFolder() {
		callbacks = new ArrayList<>();
		watched_directories = new ConcurrentHashMap<>();
		pending_configuration_update = new AtomicBoolean(false);
		
		policy = new WatchfolderPolicy().setupRegularFiles(false);
		file_detection_time = 1000;
		scan_period = 10000;
		scan_in_hidden_dirs = false;
		scan_in_symboliclink_dirs = true;
		search_in_subfolders = true;
		callback_in_first_scan = true;
		
		closed = false;
		
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
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		});
	}
	
	/**
	 * On the first push, start watchfolder
	 */
	public synchronized WatchFolder registerCallback(WatchfolderEvent callback) {
		if (closed) {
			throw new RuntimeException("Closed watchfolder for " + observed_directory);
		} else if (callbacks.contains(callback)) {
			return this;
		}
		
		callbacks.add(callback);
		
		if (callbacks.size() == 1) {
			executor.execute(() -> {
				if (closed) {
					return;
				}
				while (pending_configuration_update.get()) {
					Thread.onSpinWait();
				}
				pushNewDirectory(observed_directory, true);
			});
		}
		return this;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public synchronized boolean unRegisterCallback(WatchfolderEvent callback) {
		return callbacks.remove(callback);
	}
	
	public String toString() {
		return "Watchfoler \"" + observed_directory + "\" on " + watched_directories.mappingCount() + " sub dirs";
	}
	
	public String internalToString() {
		return "watchfoler \"" + observed_directory + "\"";
	}
	
	private String computeRelativeName(File file) {
		if (observed_directory == null) {
			return "\"" + file.getPath() + "\"";
		} else if (file.equals(observed_directory)) {
			return "/";
		} else if (file.getPath().startsWith(observed_directory.getPath()) == false) {
			return "\"" + file.getPath() + "\"";
		} else {
			return "\"" + file.getPath().substring(observed_directory.getPath().length()) + "\"";
		}
	}
	
	private void pushNewDirectory(File new_dir, boolean first_scan) {
		if (watched_directories.containsKey(new_dir) == false) {
			log.debug("New directory " + computeRelativeName(new_dir) + " in " + toString());
		}
		
		watched_directories.computeIfAbsent(new_dir, dir -> {
			return new WatchedDirectory(dir, first_scan);
		});
	}
	
	private void cancelOldDirectory(File old_dir) {
		WatchedDirectory old_watched_directory = watched_directories.remove(old_dir);
		
		if (old_watched_directory != null) {
			log.debug("Cancel directory " + computeRelativeName(old_dir) + " in " + toString());
			
			if (old_watched_directory.pending_next_update != null) {
				old_watched_directory.pending_next_update.cancel(true);
			}
			old_watched_directory.items.stream().filter(item -> item.is_directory).map(item -> item.item).forEach(item -> {
				cancelOldDirectory(item);
			});
		}
	}
	
	private enum State {
		DETECTED, IN_MODIFICATION, IDLE;
	}
	
	class WatchedDirectory {
		private final File directory;
		private final ArrayList<WatchedFile> items;
		private volatile Future<?> pending_next_update;
		private volatile boolean first_scan;
		// private long last_update_date;
		
		private WatchedDirectory(File directory, boolean first_scan) {
			this.directory = directory;
			this.items = new ArrayList<>();
			this.first_scan = first_scan;
			update();
			this.first_scan = false;
		}
		
		CompletableFuture<Void> cancelNextScan() {
			synchronized (directory) {
				if (pending_next_update == null) {
					return CompletableFuture.completedFuture(null);
				}
				
				return CompletableFuture.runAsync(() -> {
					pending_next_update.cancel(false);
					
					while (pending_next_update.isDone() == false) {
						Thread.onSpinWait();
					}
				});
			}
		}
		
		void update() {
			if (directory.exists() == false) {
				cancelOldDirectory(directory);
				return;
			}
			
			synchronized (directory) {
				log.trace("Update directory " + computeRelativeName(directory) + " in " + internalToString());
				
				Set<File> actual_items_in_dir = Arrays.asList(directory.listFiles()).stream().filter(f -> {
					if (f.isDirectory() == false) {
						/**
						 * Files are always welcome.
						 */
						return true;
					}
					
					if (search_in_subfolders == false) {
						return false;
					}
					
					/**
					 * Remove "bad" directories
					 */
					if (scan_in_hidden_dirs == false) {
						if (f.isHidden() | f.getName().startsWith(".")) {
							return false;
						}
					} else if (scan_in_symboliclink_dirs == false) {
						try {
							BasicFileAttributes attrs = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
							if (attrs.isSymbolicLink()) {
								return false;
							}
						} catch (IOException e) {
							throw new RuntimeException("Can't read attributes for \"" + f + "\"", e);
						}
					}
					
					if (f.canRead() == false) {
						log.debug("Can't read \"" + f + "\"");
						return false;
					} else if (f.canExecute() == false) {
						log.debug("Can't read (execute) \"" + f + "\"");
						return false;
					}
					return true;
				}).map(f -> {
					try {
						return f.getCanonicalFile();
					} catch (IOException e) {
						if (f.isDirectory()) {
							cancelOldDirectory(f);
						}
						throw new RuntimeException("Can't access to " + f, e);
					}
				}).collect(Collectors.toSet());
				
				/**
				 * Compute deltas
				 */
				List<File> added_item_list = actual_items_in_dir.stream().filter(f -> {
					return items.stream().noneMatch(item -> item.item.equals(f));
				}).collect(Collectors.toList());
				
				List<WatchedFile> deleted_item_list = items.stream().filter(item -> {
					return actual_items_in_dir.contains(item.item) == false;
				}).collect(Collectors.toList());
				
				/*List<WatchedFile> modified_item_list = items.stream().filter(item -> {
					return actual_items_in_dir.contains(item.item);
				}).filter(item -> {
					return item.hasChanged();
				}).collect(Collectors.toList());*/
				
				/**
				 * Commit deltas
				 */
				items.removeAll(deleted_item_list);
				deleted_item_list.forEach(item -> {
					asyncDispatchEvent(item.item, EventKind.DELETE);
					if (item.is_directory) {
						cancelOldDirectory(item.item);
					}
				});
				
				items.forEach(item -> {
					item.update(first_scan);
				});
				
				items.addAll(added_item_list.stream().peek(f -> {
					if (f.isDirectory()) {
						pushNewDirectory(f, first_scan);
					}
				}).map(f -> {
					return new WatchedFile(f);
				}).collect(Collectors.toList()));
				
				pending_next_update = sch_service.schedule(() -> {
					try {
						update();
					} catch (Exception e) {
						log.error("Can't update " + directory + ", remove scan on it for " + internalToString(), e);
						cancelOldDirectory(directory);
					}
				}, scan_period, TimeUnit.MILLISECONDS);
			}
		}
		
		class WatchedFile {
			final File item;
			long size;
			long date;
			boolean is_directory;
			long last_activity;
			State state;
			
			private WatchedFile(File item) {
				this.item = item;
				size = item.length();
				date = item.lastModified();
				is_directory = item.isDirectory();
				last_activity = System.currentTimeMillis();
				state = State.DETECTED;
				
				log.trace("Store " + computeRelativeName(item) + " in " + internalToString());
				
				if (file_detection_time == 0) {
					state = State.IDLE;
				}
			}
			
			private boolean hasChanged() {
				return item.length() != size | item.lastModified() != date | item.isDirectory() != is_directory;
			}
			
			private boolean inGracePeriod() {
				return System.currentTimeMillis() - last_activity < file_detection_time;
			}
			
			private void sync() {
				if (is_directory != item.isDirectory()) {
					if (is_directory) {
						throw new RuntimeException("Type error: the file was a dir: " + item);
					} else {
						throw new RuntimeException("Type error: the dir was a file: " + item);
					}
				}
				
				if (log.isTraceEnabled()) {
					ArrayList<String> message = new ArrayList<String>();
					if (size != item.length()) {
						message.add("size change: " + size + " and now " + item.length());
					}
					if (date != item.lastModified()) {
						message.add("date change: " + Logtoolkit.dateLog(date) + " and now " + Logtoolkit.dateLog(item.lastModified()));
					}
					if (message.isEmpty() == false) {
						log.trace("File update " + item + " " + message + ", last activity is " + Logtoolkit.dateLog(last_activity));
					}
				}
				
				size = item.length();
				date = item.lastModified();
				is_directory = item.isDirectory();
				last_activity = System.currentTimeMillis();
			}
			
			private void update(boolean first_scan) {
				if (state == State.DETECTED | state == State.IN_MODIFICATION) {
					if (hasChanged()) {
						sync();
					} else if (inGracePeriod() == false) {
						if (state == State.DETECTED) {
							if (first_scan) {
								asyncDispatchEvent(item, EventKind.FIRST_DETECTION);
							} else {
								asyncDispatchEvent(item, EventKind.CREATE);
							}
						} else {
							/**
							 * IN_MODIFICATION
							 */
							asyncDispatchEvent(item, EventKind.MODIFY);
						}
						state = State.IDLE;
					}
				} else if (hasChanged()) {
					/**
					 * IDLE & hasChanged
					 */
					state = State.IN_MODIFICATION;
					sync();
				}
			}
		}
		
	}
	
	private void asyncDispatchEvent(File validated_event_file, EventKind kind) {
		if (closed) {
			return;
		}
		if (callback_in_first_scan == false && EventKind.FIRST_DETECTION.equals(kind)) {
			return;
		}
		
		if (policy != null) {
			try {
				if (policy.internalApplyFilter(validated_event_file, this, kind) == false) {
					log.debug("File activity, but not in policy: " + validated_event_file + " (" + kind + ")");
					return;
				}
			} catch (IOException e) {
				log.error("Can't validate file " + validated_event_file, e);
			}
		}
		
		log.debug("File activity " + computeRelativeName(validated_event_file) + " (" + kind + ") on " + internalToString());
		
		final File dir = observed_directory;
		callbacks.forEach(c -> {
			executor.execute(() -> {
				if (closed) {
					return;
				}
				try {
					c.onEvent(dir, validated_event_file, kind, this);
				} catch (Exception e) {
					log.error("Can't propagate event for " + validated_event_file + "(" + c + ")", e);
				}
			});
		});
	}
	
	/**
	 * 1 sec by default
	 */
	public synchronized WatchFolder setFileDetectionTime(long detection_time, TimeUnit unit) {
		this.file_detection_time = unit.toMillis(detection_time);
		return this;
	}
	
	public synchronized long getFileDetectionTime(TimeUnit unit) {
		return unit.convert(file_detection_time, TimeUnit.MILLISECONDS);
	}
	
	public synchronized boolean isScanInHiddenDirs() {
		return scan_in_hidden_dirs;
	}
	
	/**
	 * false by default
	 */
	public synchronized WatchFolder setScanInHiddenDirs(boolean scan_in_hidden_dirs) {
		this.scan_in_hidden_dirs = scan_in_hidden_dirs;
		return this;
	}
	
	public synchronized boolean isScanInSymboliclinkDirs() {
		return scan_in_symboliclink_dirs;
	}
	
	/**
	 * true by default
	 */
	public synchronized WatchFolder setScanInSymboliclinkDirs(boolean scan_in_symboliclink_dirs) {
		this.scan_in_symboliclink_dirs = scan_in_symboliclink_dirs;
		return this;
	}
	
	/**
	 * 10 sec by default
	 */
	public synchronized WatchFolder setScanPeriod(long scan_period, TimeUnit unit) {
		this.scan_period = unit.toMillis(scan_period);
		return this;
	}
	
	public synchronized long getScanPeriod(TimeUnit unit) {
		return unit.convert(scan_period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * true by default
	 */
	public synchronized WatchFolder setSearchInSubfolders(boolean search_in_subfolders) {
		this.search_in_subfolders = search_in_subfolders;
		return this;
	}
	
	public synchronized boolean isSearchInSubfolders() {
		return search_in_subfolders;
	}
	
	public synchronized File getObservedDirectory() {
		return observed_directory;
	}
	
	/**
	 * @see new WatchfolderPolicy().setupRegularFiles(false)
	 */
	public synchronized WatchFolder setPolicy(WatchfolderPolicy policy) {
		if (policy == null) {
			throw new NullPointerException("\"policy\" can't to be null");
		}
		this.policy = policy;
		return this;
	}
	
	public synchronized WatchfolderPolicy getPolicy() {
		return policy;
	}
	
	/**
	 * true by default
	 */
	public synchronized WatchFolder setCallbackInFirstScan(boolean callback_in_first_scan) {
		this.callback_in_first_scan = callback_in_first_scan;
		return this;
	}
	
	public synchronized boolean isCallbackInFirstScan() {
		return callback_in_first_scan;
	}
	
	public synchronized WatchFolder setScheduledService(ScheduledExecutorService sch_service) {
		this.sch_service = sch_service;
		if (sch_service == null) {
			throw new NullPointerException("\"sch_service\" can't to be null");
		}
		return this;
	}
	
	public synchronized ScheduledExecutorService getScheduledService() {
		return sch_service;
	}
	
	public synchronized WatchFolder setExecutor(ThreadPoolExecutor executor) {
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		return this;
	}
	
	public synchronized ThreadPoolExecutor getExecutor() {
		return executor;
	}
	
	public synchronized WatchFolder setObservedDirectory(File observed_directory) throws IOException {
		if (this.observed_directory != null & observed_directory != null) {
			if (this.observed_directory.equals(observed_directory) == false) {
				beforeUpdateConfiguration();
				afterUpdateConfiguration();
			}
		}
		
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
		return this;
	}
	
	/**
	 * Do a clean stop
	 */
	@OnBeforeUpdateConfiguration
	private void beforeUpdateConfiguration() {
		pending_configuration_update.set(true);
		
		stopAllWatchedDirectories();
	}
	
	@OnAfterUpdateConfiguration
	private void afterUpdateConfiguration() {
		if (closed) {
			return;
		}
		
		pending_configuration_update.set(false);
		
		if (observed_directory == null) {
			throw new NullPointerException("observed_directory can't to be null");
		} else {
			/**
			 * Search the previous parent dir
			 */
			File parent = watched_directories.reduceKeys(10_000, (l, r) -> {
				if (l.getPath().startsWith(r.getPath())) {
					return r;
				} else if (l.equals(r)) {
					return l;
				} else if (r.getPath().startsWith(l.getPath())) {
					return l;
				} else if (l.getParentFile().equals(r.getParentFile())) {
					return l.getParentFile();
				} else {
					log.warn("Invalid compare: " + l + " <-> " + r);
					return l;
				}
			});
			
			if (observed_directory.equals(parent) == false) {
				watched_directories.clear();
				if (callbacks.isEmpty() == false) {
					executor.execute(() -> {
						if (closed) {
							return;
						}
						pushNewDirectory(observed_directory, true);
					});
				}
			} else {
				/**
				 * Keep the actual path tree, and restart all scans
				 */
				watched_directories.values().stream().forEach(w_d -> {
					w_d.pending_next_update = sch_service.submit(() -> {
						try {
							w_d.update();
						} catch (Exception e) {
							log.error("Can't update " + w_d.directory + ", remove scan on it for " + internalToString(), e);
							cancelOldDirectory(w_d.directory);
						}
					});
				});
			}
		}
	}
	
	/**
	 * Blocking
	 */
	private void stopAllWatchedDirectories() {
		List<CompletableFuture<Void>> cancel_tasks = watched_directories.values().stream().map(watched_dir -> {
			return watched_dir.cancelNextScan();
		}).collect(Collectors.toList());
		
		if (cancel_tasks.isEmpty()) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		CompletableFuture<Void>[] cancel_tasks_array = new CompletableFuture[cancel_tasks.size()];
		for (int pos = 0; pos < cancel_tasks_array.length; pos++) {
			cancel_tasks_array[pos] = cancel_tasks.get(pos);
		}
		try {
			CompletableFuture.allOf(cancel_tasks_array).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Can't cancel some tasks", e);
		}
	}
	
	/**
	 * Will no touch to internal sch_service and executor, just clean active an current task for this watchfolder.
	 */
	@OnBeforeRemovedInConfiguration
	public synchronized void close() {
		log.info("Ask to stop watchfolder " + toString());
		
		closed = true;
		
		stopAllWatchedDirectories();
		callbacks.forEach(c -> {
			c.onStop(observed_directory, this);
		});
		callbacks.clear();
		
		watched_directories.clear();
	}
}
