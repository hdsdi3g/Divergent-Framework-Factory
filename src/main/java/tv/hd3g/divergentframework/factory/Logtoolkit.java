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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logtoolkit {
	
	private static final SimpleDateFormat date_log = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
	private static final SimpleDateFormat date_filename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
	
	public static final String dateLog(long date) {
		return date_log.format(new Date(date));
	}
	
	public static String numberFormat(long value) {
		return NumberFormat.getInstance().format(value); // also exists with double
	}
	
	public static final String dateFilename(long date) {
		return date_filename.format(new Date(date));
	}
	
}
