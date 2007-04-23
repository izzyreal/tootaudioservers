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

/**
 * 
 * @author Paul J. Leonard
 * @author Peter J. Salomonsen
 *
 */
public abstract class PriorityAudioServer extends BasicAudioServer  {

	private int priorityRequested = -1;
	private int priority = 0;

	/**
	 * The watchDogTimestamp should be as close to system.currentTimeMillis() as possible. If the difference is more than 100ms then the priority 
	 * thread should go to sleep as long as the watchDogTimestamp differs.
	 */
	private static long watchDogTimestamp;
	/**
	 * Create the watchdog thread - a low priority thread that updates the watchDogTimestamp every millisecond. If the realtime priority thread
	 * starts occupying the system resources - the watchdog timestamp will not be updated. Thus the realtime priority thread should check
	 * that the watchDogTimestamp does not differ more than 100ms from System.currentTimeMillis() - and if it does, the realtime thread should
	 * sleep until the watchDogTimestamp is updated again.
	 */
	static
	{
	
		Thread thr = new Thread()
		{
			public void run()
			{
				while(true)
				{
					watchDogTimestamp = System.currentTimeMillis();
					try {
						sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
				}
			}
		};
		thr.setPriority(Thread.MIN_PRIORITY);
		thr.start();
	}
	
	public PriorityAudioServer() {
	}

	public void work() {
		/**
		 * Sleep while System.currentTimeMillis differs more than 100 ms from watchDogTimestamp
		 */
		while((System.currentTimeMillis()-watchDogTimestamp)>100)
		{
			try {
				System.out.println("PRIORITY THREAD WATCHDOG: System was blocked for at least "+(System.currentTimeMillis()-watchDogTimestamp)+" ms, freeing resources");
				Thread.sleep(1);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if (priorityRequested != -1) {
			if (System.getProperty("os.name").equals("Linux")) {
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
