/*
 * Created on Feb 16, 2007
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

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.server.AudioClient;
import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.IOAudioProcess;

public class ChoppedAudioServerAdapter implements AudioServer,AudioClient  {

	JackAudioServer server;  // could be more abstract if you want to deal with the Jack case 

	Hashtable<String,IOAudioProcess> outputMap=new Hashtable<String,IOAudioProcess>(); 
	Hashtable<String,IOAudioProcess> inputMap=new Hashtable<String,IOAudioProcess>(); 
	
	Vector<ChoppedOutAudioProcessWrapper> outputs=new Vector<ChoppedOutAudioProcessWrapper>();
	AudioClient client;
	int choppedBufferSize=128;
	int outBufferSize;
	private int bigBufferPtr;
	
	public ChoppedAudioServerAdapter(JackAudioServer server) {
		this.server=server;
		outBufferSize=server.createAudioBuffer("choppedaccumulator").getSampleCount();
		assert(outBufferSize%choppedBufferSize == 0);
		server.setClient(this);
	}
	
	public void setClient(AudioClient client) {
		this.client=client;
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server. stop() ;
		
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public List<String> getAvailableOutputNames() {
		return server.getAvailableOutputNames();
	}

	public List<String> getAvailableInputNames() {
		return server. getAvailableInputNames();
	}

	public IOAudioProcess openAudioOutput(String name,String label) {
		IOAudioProcess p=outputMap.get(name);
		System.out.println(name + "   " + p );
		if (p == null )
			try {
				outputMap.put(name,p=new ChoppedOutAudioProcessWrapper(server.openAudioOutput(name, label)));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return p;
	}

	
	public IOAudioProcess openAudioInput(String name,String label) throws Exception {
		IOAudioProcess p;
		if (name == null) return null;
		if ((p=inputMap.get(name)) == null) 
			inputMap.put(name,p=new ChoppedInAudioProcessWrapper(server.openAudioInput(name, label)));
		return p;
	}

	

	public AudioBuffer createAudioBuffer(String name) {

		AudioBuffer buffer = new AudioBuffer(name,2, choppedBufferSize,
				server.getSampleRate());

		System.out.println("Chopped Audio buffer created ");
		return buffer;
	}

	//	public AudioBuffer createAudioBuffer() {
//		return server.createAudioBuffer();
//	}

	public float getSampleRate() {
		return server.getSampleRate();
	}

	public int getSampleSizeInBits() {
		return server.getSampleSizeInBits();
	}

	public float getLoad() {
		return server.getLoad();
		// TODO Auto-generated method stub
	}
	
	class ChoppedInAudioProcessWrapper implements IOAudioProcess {
		AudioBuffer inBuffer=server.createAudioBuffer("chopInAccumulator");
		IOAudioProcess process;
		int openCount=0;
		
		public ChoppedInAudioProcessWrapper(IOAudioProcess process) {
			this.process=process;
		}

		public void open() {
			if (openCount== 0 ) process.open();
			openCount++;
		}

		/**
		 * The underlying server only needs to read once. Then we feed off the buffer.
		 * 
		 */
		public int processAudio(AudioBuffer buffer) {
			
			
			if (bigBufferPtr == 0 ) process.processAudio(inBuffer); 

			int nch = process.getChannelFormat().getCount();
			for(int i=0;i<nch;i++) {
				float out[]=buffer.getChannel(i);
				float in[]=inBuffer.getChannel(i);
				System.arraycopy(in, bigBufferPtr, out, 0, choppedBufferSize);
			}
			return AUDIO_OK;		
		}

		
		public void close() {
			openCount--;
			if (openCount==0) process.close();
			
		}

//		public int getChannels() {
//		
//			return process.getChannels();
//		}

		public ChannelFormat getChannelFormat() {
			return process.getChannelFormat();
		}

		public String getName() {
			// TODO Auto-generated method stub
			return process.getName();
		}
		
		
	}

	
	class ChoppedOutAudioProcessWrapper implements IOAudioProcess {
		
		AudioBuffer outBuffer=server.createAudioBuffer("CHoppedOuitAcccumulator");
		IOAudioProcess process;
		int openCount=0;

		
		public ChoppedOutAudioProcessWrapper(IOAudioProcess process) {
			this.process=process;
			open();  // no one seems to call this so I betteer had do so.

		}

		public void open() {
			if (openCount== 0 ) {
				process.open();
				outputs.add(this);
			}
			openCount++;
		}

		public int processAudio(AudioBuffer buffer) {
			for(int i=0;i<2;i++) {
				float in[]=buffer.getChannel(i);
				float out[]=outBuffer.getChannel(i);
				System.arraycopy(in, 0, out, bigBufferPtr, choppedBufferSize);
			}
			return AUDIO_OK;
		}

		
		void forward() {
			process.processAudio(outBuffer);
		}
		
		public void close() {
			openCount--;
			if (openCount==0) {
				process.close();
				outputs.remove(this);
			}
		}

//		public int getChannels() {
//		
//			return process.getChannels();
//		}

		public ChannelFormat getChannelFormat() {
			return process.getChannelFormat();
		}

		public String getName() {
			// TODO Auto-generated method stub
			return process.getName();
		}
		
		
	}

	public void closeAudioInput(IOAudioProcess input) {
		// TODO Auto-generated method stub
		
	}

	public void closeAudioOutput(IOAudioProcess output) {
		// TODO Auto-generated method stub
		
	}

	public int getInputLatencyFrames() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getOutputLatencyFrames() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getTotalLatencyInFrames() {
		// TODO Auto-generated method stub
		return 0;
	}

	boolean doSleeping=false;
	
	public void work(int size) {
		long fact=outBufferSize/choppedBufferSize;
		long chopBufferNanos = server.bufferNanos/fact;
		long expire=server.startTime;
		int choppedUnderruns=0;
		for(bigBufferPtr=0;bigBufferPtr<outBufferSize;bigBufferPtr+=choppedBufferSize) {
			client.work(choppedBufferSize);
			if (doSleeping) {
			expire += chopBufferNanos;
			long sleep=expire - System.nanoTime();
			if (sleep > 0) {
				sleep *= .6;
				try {
					Thread.sleep(sleep/1000000, (int) (sleep%1000000));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				choppedUnderruns++;
			}
			}
		}
	
		for (ChoppedOutAudioProcessWrapper out:outputs) {	
			out.forward();
		}		
		
		if (doSleeping && choppedUnderruns != 0) {
		//	System.out.println(" CHopped underruns =" + choppedUnderruns);
		}
	}

	public String getConfigKey() {
		// TODO Auto-generated method stub
		return ((ConfigurableAudioServer)server).getConfigKey();
	}
}
