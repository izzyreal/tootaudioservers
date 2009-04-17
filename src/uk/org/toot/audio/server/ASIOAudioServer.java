// Copyright (C) 2009 Steve Taylor.
// Distributed under the Toot Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.toot.org.uk/LICENSE_1_0.txt)

package uk.org.toot.audio.server;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.ChannelFormat;

import com.synthbot.jasiohost.*;

public class ASIOAudioServer implements AudioServer
{
	private AsioDriver driver;
	private AsioDriverListener listener;
	private AudioClient client;
	private List<AudioBuffer> buffers = new java.util.ArrayList<AudioBuffer>();
	private Set<AsioChannelInfo> activeChannels = new HashSet<AsioChannelInfo>();
	private int bufferFrames;
	private String driverName;
	private String sampleTypeName = null;
	
    private long startTimeNanos;
	private long endTimeNanos;
	private long prevStartTimeNanos;
	private float load;

	public ASIOAudioServer(String driverName) {
		driver = JAsioHost.getAsioDriver(driverName);
		listener = new DriverListener();
		driver.addAsioDriverListener(listener);
		bufferFrames = driver.getBufferPreferredSize();
		this.driverName = driverName;
		System.out.println(driverName+": "+bufferFrames+" frames @ "+(int)getSampleRate()+"Hz");
	}
	
	public AsioDriver getDriver() {
		return driver;
	}
	
	public String getDriverName() {
		return driverName;
	}
	
	public String getSampleTypeName() {
		return sampleTypeName;
	}
	
	protected void createSampleTypeName(AsioChannelInfo info) {
		sampleTypeName = info.getSampleType().name().substring(6);
	}
	
	public List<String> getAvailableInputNames() {
		List<String> names = new java.util.ArrayList<String>();
		AsioChannelInfo info;
		int num = driver.getNumChannelsInput();
		for ( int i = 0; i < num; i += 2 ) { // !!!
			info = driver.getChannelInfoInput(i);
			names.add(info.getChannelName());
		}
		return names;
	}

	public List<String> getAvailableOutputNames() {
		List<String> names = new java.util.ArrayList<String>();
		AsioChannelInfo info;
		int num = driver.getNumChannelsOutput();
		for ( int i = 0; i < num; i += 2 ) { // !!!
			info = driver.getChannelInfoOutput(i);
			names.add(info.getChannelName());
		}
		return names;
	}

	public IOAudioProcess openAudioInput(String name, String label) throws Exception {
		AsioChannelInfo info, info2;
		int num = driver.getNumChannelsInput();
		for ( int i = 0; i < num; i += 2 ) { // !!!
			info = driver.getChannelInfoInput(i);
			if ( name == null || info.getChannelName().equals(name) ) {
				info2 = driver.getChannelInfoInput(1+i);
				if ( name == null ) name = info.getChannelName();
				IOAudioProcess process = 
					new ChannelInputProcess(label, info, info2, name);
				process.open();
				return process;
			}
		}
		return null;
	}

	public IOAudioProcess openAudioOutput(String name, String label) throws Exception {
		AsioChannelInfo info, info2;
		int num = driver.getNumChannelsOutput();
		for ( int i = 0; i < num; i += 2 ) { // !!!
			info = driver.getChannelInfoOutput(i);
			if ( name == null || info.getChannelName().equals(name) ) {
				info2 = driver.getChannelInfoOutput(1+i);
				IOAudioProcess process = 
					new ChannelOutputProcess(label, info, info2);
				process.open();
				if ( sampleTypeName == null ) createSampleTypeName(info);
				return process;
			}
		}
		return null;
	}

	public void closeAudioInput(IOAudioProcess input) {
		if ( input != null ) {
			try {
				input.close();		
			} catch ( Exception e ) {
				//
			}
		}
	}

	public void closeAudioOutput(IOAudioProcess output) {
		if ( output != null ) {
			try {
				output.close();		
			} catch ( Exception e ) {
				//
			}
		}
	}

	public AudioBuffer createAudioBuffer(String name) {
		AudioBuffer buffer = new AudioBuffer(name, 2, bufferFrames, getSampleRate());
		buffers.add(buffer);
		return buffer;
	}

    protected void resizeBuffers() {
        for ( AudioBuffer buffer : buffers ) {
            buffer.changeSampleCount(bufferFrames, false); // don't keep old
        }
    }
    
	public float getLoad() {
		return load;
	}

	public int getInputLatencyFrames() {
		return driver.getLatencyInput();
	}

	public int getOutputLatencyFrames() {
		return driver.getLatencyOutput();
	}

	public float getSampleRate() {
		return (float)driver.getSampleRate();
	}

	public int getTotalLatencyFrames() {
		return driver.getLatencyInput()+driver.getLatencyOutput();
	}

	public boolean isRunning() {
		return driver.getState().equals(AsioDriverState.RUNNING);
	}

	public void setClient(AudioClient client) {
		this.client = client;
	}

	public void setSampleRate(float sampleRate) {
		// we silently ignore this
		// must be set from native control panel
	}

	public void start() {
		driver.createBuffers(activeChannels);
		driver.start();
	}

	public void stop() {
		driver.stop();
		driver.disposeBuffers();
	}

	protected void work() {
		prevStartTimeNanos = startTimeNanos;
        startTimeNanos = System.nanoTime();
		int bf = driver.getBufferPreferredSize();
		if ( bufferFrames != bf ) {
			bufferFrames = bf;
			resizeBuffers();
		}
		if ( client == null ) return;
		client.work(bufferFrames);		
        endTimeNanos = System.nanoTime();

        // calculate client load
        load = 100 * (float)(endTimeNanos - startTimeNanos) / (startTimeNanos - prevStartTimeNanos);
	}
	
	protected class DriverListener implements AsioDriverListener
	{
		public void bufferSwitch(Set<AsioChannelInfo> activeChannels) {
			work();
		}

		public void latenciesChanged(int inputLatency, int outputLatency) {
			// ! does not imply buffer size has changed
			System.out.println("ASIO Latencies Changed: in "+inputLatency+", out "+outputLatency);		
		}

		public void resetRequest() {
			System.out.println("ASIO Reset Request");
		}

		public void resyncRequest() {
			System.out.println("ASIO Resync Request");
		}

		public void sampleRateDidChange(double sampleRate) {
			System.out.println("ASIO Sample rate Changed: sampleRate");
		}		
	}
	
	protected abstract class ChannelProcess implements IOAudioProcess
	{
		private String name;
		protected AsioChannelInfo info[] = new AsioChannelInfo[2];
		
		public ChannelProcess(String name, AsioChannelInfo info0, AsioChannelInfo info1) {
			this.name = name;
			info[0] = info0;
			info[1] = info1;
			System.out.println(name);
			System.out.println("  Left : "+info0.getChannelName()+", "+info0.getChannelIndex());
			System.out.println("  Right: "+info1.getChannelName()+", "+info1.getChannelIndex());
		}
		
		public ChannelFormat getChannelFormat() {
			return ChannelFormat.STEREO;
		}

		public String getName() {
			return name;
		}

		public void open() throws Exception {
			activeChannels.add(info[0]);
			activeChannels.add(info[1]);
		}

		public void close() throws Exception {
			// this won't have any effect until next server stop/start
			activeChannels.remove(info[0]);
			activeChannels.remove(info[1]);
		}
	}
	
	protected class ChannelInputProcess extends ChannelProcess
	{
		private AudioBuffer.MetaInfo metaInfo;
		
		public ChannelInputProcess(String name, AsioChannelInfo info0, AsioChannelInfo info1, String location) {
			super(name, info0, info1);
            metaInfo = new AudioBuffer.MetaInfo(name, location);
		}

		public int processAudio(AudioBuffer buffer) {
            if ( !buffer.isRealTime() ) return AUDIO_DISCONNECT;
            buffer.setMetaInfo(metaInfo);
			int nchan = buffer.getChannelCount();
			for ( int chan = 0; chan < nchan; chan++ ) {
				float[] samples = buffer.getChannel(chan);
				AsioChannelInfo channelInfo = info[chan];
//				if ( !channelInfo.isActive() ) continue; // !!!
				ByteBuffer byteBuffer = channelInfo.getByteBuffer();
				if ( byteBuffer == null ) continue; // !!!
				switch (channelInfo.getSampleType()) {
				case ASIOSTFloat64MSB:
				case ASIOSTFloat64LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						samples[i] = (float)byteBuffer.getDouble();
					}
					break;
				}
				case ASIOSTFloat32MSB:
				case ASIOSTFloat32LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						samples[i] = byteBuffer.getFloat();
					}
					break;
				}
				case ASIOSTInt32MSB:
				case ASIOSTInt32LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putInt((int) (samples[i] * (double) Integer.MAX_VALUE));
						samples[i] = (float)(byteBuffer.getInt() / (double)Integer.MAX_VALUE);
					}
					break;
				}
				case ASIOSTInt32MSB16:
				case ASIOSTInt32LSB16: {
					// should dither
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putInt((int) (samples[i] * (double) 0x00007FFF));
						samples[i] = (float)(byteBuffer.getInt() / (double)0x00007FFF);
					}
					break;
				}
				case ASIOSTInt32MSB18:
				case ASIOSTInt32LSB18: {
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putInt((int) (samples[i] * (double) 0x0001FFFF));
						samples[i] = (float)(byteBuffer.getInt() / (double)0x0001FFFF);
					}
					break;
				}
				case ASIOSTInt32MSB20:
				case ASIOSTInt32LSB20: {
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putInt((int) (samples[i] * (double) 0x0007FFFF));
						samples[i] = (float)(byteBuffer.getInt() / (double)0x0007FFFF);
					}
					break;
				}
				case ASIOSTInt32MSB24:
				case ASIOSTInt32LSB24: {
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putInt((int) (samples[i] * (double) 0x007FFFFF));
						samples[i] = (float)(byteBuffer.getInt() / (double)0x007FFFFF);
					}
					break;
				}
				case ASIOSTInt16MSB:
				case ASIOSTInt16LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
//						byteBuffer.putShort((short) (samples[i] * (double) Short.MAX_VALUE));
						samples[i] = (float)(byteBuffer.getShort() / (double)Short.MAX_VALUE);
					}
					break;
				}
				case ASIOSTInt24MSB: {
					// bytes have no endian-ness, and must therefore be placed manually
					for ( int i = 0; i < bufferFrames; i++ ) {
/*						int sampleValueInt = (int) (samples[i] * (double) 0x007FFFFF);
						byteBuffer.put((byte) ((sampleValueInt >> 16) & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 8) & 0x000000FF));
						byteBuffer.put((byte) (sampleValueInt & 0x000000FF)); */
						// TODO
					}
					break;
				}
				case ASIOSTInt24LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
/*						int sampleValueInt = (int) (samples[i] * (double) 0x007FFFFF);
						byteBuffer.put((byte) (sampleValueInt & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 8) & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 16) & 0x000000FF)); */
						// TODO
					}
					break;
				}
				case ASIOSTDSDInt8MSB1:
				case ASIOSTDSDInt8LSB1:
				case ASIOSTDSDInt8NER8: {
					// not supported. silence.
				}
				} // end case

			}
			return AUDIO_OK;
		}
		
	}

	protected class ChannelOutputProcess extends ChannelProcess
	{
		public ChannelOutputProcess(String name, AsioChannelInfo info0, AsioChannelInfo info1) {
			super(name, info0, info1);
		}

		public int processAudio(AudioBuffer buffer) {
            if ( !buffer.isRealTime() ) return AUDIO_DISCONNECT;
			int nchan = buffer.getChannelCount();
			for ( int chan = 0; chan < nchan; chan++ ) {
				float[] samples = buffer.getChannel(chan);
				AsioChannelInfo channelInfo = info[chan];
//				if ( !channelInfo.isActive() ) continue; // !!!
				ByteBuffer byteBuffer = channelInfo.getByteBuffer();
				if ( byteBuffer == null ) continue; // !!!
				switch (channelInfo.getSampleType()) {
				case ASIOSTFloat64MSB:
				case ASIOSTFloat64LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putDouble(samples[i]);
					}
					break;
				}
				case ASIOSTFloat32MSB:
				case ASIOSTFloat32LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putFloat(samples[i]);
					}
					break;
				}
				case ASIOSTInt32MSB:
				case ASIOSTInt32LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putInt((int) (samples[i] * (double) Integer.MAX_VALUE));
					}
					break;
				}
				case ASIOSTInt32MSB16:
				case ASIOSTInt32LSB16: {
					// should dither
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putInt((int) (samples[i] * (double) 0x00007FFF));
					}
					break;
				}
				case ASIOSTInt32MSB18:
				case ASIOSTInt32LSB18: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putInt((int) (samples[i] * (double) 0x0001FFFF));
					}
					break;
				}
				case ASIOSTInt32MSB20:
				case ASIOSTInt32LSB20: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putInt((int) (samples[i] * (double) 0x0007FFFF));
					}
					break;
				}
				case ASIOSTInt32MSB24:
				case ASIOSTInt32LSB24: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putInt((int) (samples[i] * (double) 0x007FFFFF));
					}
					break;
				}
				case ASIOSTInt16MSB:
				case ASIOSTInt16LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
						byteBuffer.putShort((short) (samples[i] * (double) Short.MAX_VALUE));
					}
					break;
				}
				case ASIOSTInt24MSB: {
					// bytes have no endian-ness, and must therefore be placed manually
					for ( int i = 0; i < bufferFrames; i++ ) {
					int sampleValueInt = (int) (samples[i] * (double) 0x007FFFFF);
						byteBuffer.put((byte) ((sampleValueInt >> 16) & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 8) & 0x000000FF));
						byteBuffer.put((byte) (sampleValueInt & 0x000000FF));
					}
					break;
				}
				case ASIOSTInt24LSB: {
					for ( int i = 0; i < bufferFrames; i++ ) {
					int sampleValueInt = (int) (samples[i] * (double) 0x007FFFFF);
						byteBuffer.put((byte) (sampleValueInt & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 8) & 0x000000FF));
						byteBuffer.put((byte) ((sampleValueInt >> 16) & 0x000000FF));
					}
					break;
				}
				case ASIOSTDSDInt8MSB1:
				case ASIOSTDSDInt8LSB1:
				case ASIOSTDSDInt8NER8: {
					// not supported. silence.
				}
				} // end case
			}
			return AUDIO_OK;
		}
	}
}
