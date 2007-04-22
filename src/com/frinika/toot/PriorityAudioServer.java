/*
 * Created on Apr 13, 2007
 *
 * Copyright (c) 2006-2007 P.J.Leonard
 * 
 * http://www.frinika.com
 * 
 * This file is part of Frinika.
 * 
 * Frinika is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * Frinika is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Frinika; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.frinika.toot;

import com.frinika.priority.Priority;

import uk.org.toot.audio.server.BasicAudioServer;

public abstract class PriorityAudioServer extends BasicAudioServer  {

	private static boolean isLinux =
		System.getProperty("os.name").equals("Linux");
	
	private int priorityRequested = -1;
	private int priority = 0;

	public PriorityAudioServer() {
	}

	public void work() {
	
		if (priorityRequested != -1) {
			if (isLinux) {
				try {
					int prio=priorityRequested;
						if (prio > 0) {
							Priority.setPriorityRR(prio);
						} else {
							Priority.setPriorityOTHER(prio);
						}					
						Priority.display();					
				} catch (Throwable e) {
					System.err
							.println("WARN: Problem setting priority "
									+ e.toString());
				}
			}
			priority=priorityRequested;
			priorityRequested=-1;
		}
		super.work();
	}

	public void requestPriority(int i) {
		priorityRequested=i;
	}

	public int getPriority() {
		if (priorityRequested != -1) return priorityRequested;
		return priority;
	}

	public void assertPriority() {
		priorityRequested=priority;
	}
	
}
