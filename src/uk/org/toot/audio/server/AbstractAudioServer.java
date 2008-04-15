// Copyright (C) 2006 Steve Taylor.
// Distributed under the Toot Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.toot.org/LICENSE_1_0.txt)

package uk.org.toot.audio.server;

import uk.org.toot.audio.server.realtime.RTJStrategy;

/**
 * AbstractAudioServer implements AudioServer to control the timing of an
 * AudioClient.
 * The buffer size, latency and timing strategy may be varied while running.
 * Note that changing latency may cause inputs to glitch.
 * 
 * @author Steve Taylor
 * @author Peter Johan Salomonsen
 */
abstract public class AbstractAudioServer 
    implements Runnable, ExtendedAudioServer
{
    /**
     * a single client, use Composite pattern for multi client
     * @link aggregation
     * @supplierCardinality 0..1 
     */
    protected AudioClient client;
    protected boolean isRunning = false;
    protected boolean hasStopped = false;

    private static long ONE_MILLION = 1000000;

    private float bufferMilliseconds = 2f;
    private float requestedBufferMilliseconds = bufferMilliseconds; // for syncing

    private float latencyMilliseconds = 70;

    private float actualLatencyMilliseconds = 0;
    private float lowestLatencyMilliseconds = bufferMilliseconds;
    private float maximumJitterMilliseconds = 0;
    private int bufferUnderRuns = 0;
    private int bufferUnderRunThreshold = 0;

    private int outputLatencyFrames = 0;
//    private int inputLatencyFrames = 0;
    private int hardwareLatencyFrames = 0; // user needs to set 
    private int totalLatencyFrames = -1;
    
    private long totalTimeNanos;

    private boolean requestResetMetrics = false;

    protected float maximumLatencyMilliseconds = 140f; // default Linux constraint
    
    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
 //   private AudioTimingStrategy timingStrategy;

    /**
     * @supplierCardinality 0..1 */
   private boolean requestedTimingStrategy;

    private Strategy strategy;
    
    private float load = 0; // normalised load, 1 = 100% of available time
    private float peakLoad = 0;

//	private Thread thread;

    /**
     * @link aggregation
     * @supplierCardinality 0..1 
     */
    protected AudioSyncLine syncLine;

    private boolean startASAP = false;

    protected boolean started = false;
    protected int stableCount = 0;
 
    protected int stableThreshold = 1000;

    public AbstractAudioServer() {
    	this(new RTJStrategy());
    }
    
    public AbstractAudioServer(Strategy strategy) { //throws Exception {
    	this.strategy=strategy;
        totalTimeNanos = (long)(bufferMilliseconds * ONE_MILLION);
        try {
	        Runtime.getRuntime().addShutdownHook(
	            new Thread() {
	            	public void run() {
	                	AbstractAudioServer.this.stop();
	            	}
	        	}
	    	);
        } catch ( Exception e ) {
        	System.out.println("AbstractAudioServer Failed to add Shutdown Hook");
        }
        // estimate buffer underrun threshold for os
        String osName = System.getProperty("os.name");
        if ( osName.contains("Windows") ) {
            // only correct for DirectSound !!!
            bufferUnderRunThreshold = 30;
        }
        requestedTimingStrategy = true; // strategy.getSleepStrategy();
        //new SleepTimingStrategy();
    }

    public void setClient(AudioClient client) {
        this.client = client;
        checkStart(); // start may be delayed waiting for a client to be set
    }

    abstract protected void work();

    protected void checkStart() {
        if ( startASAP ) {
			if ( canStart() ) {
            	startImpl();
            } else {
//                System.out.println("AudioServer start still delayed");
            }
        }
    }

    protected boolean canStart() {
        return client != null && syncLine != null;
    }

    public void start() {
        if ( isRunning ) return;
        if ( canStart() ) {
            startImpl();
        } else {
	       	System.out.println("AudioServer start requested but delayed");
            startASAP = true;
        }
    }

    protected void startImpl() {
        started = false;
        startASAP = false;
        stableCount = 0;
       	System.out.println("AudioServer starting");
       	strategy.run(this,THREAD_NAME);
//      	thread = new Thread(this, THREAD_NAME);
//       	thread.start();
   }

    public void stop() {
        if ( !isRunning ) return;
        stopImpl();
        while (!hasStopped) {
            try {
	            Thread.sleep(10);
            } catch ( InterruptedException ie ) {
            }
        }
    }

    protected void stopImpl() {
       	System.out.println("AudioServer stopping");
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Return the hardware latency in frames which is assumed to be the same
     * for both input and output.
     */
    public int getHardwareLatencyFrames() {
    	return hardwareLatencyFrames;
    }
    
    /**
     * Set the hardware latency in frames which is assumed to be the same
     * for both input and output.
     */
    public void setHardwareLatencyFrames(int frames) {
    	hardwareLatencyFrames = frames;
    }
    
    /**
     * Return the total latency from analogue input to analogue output.
     */
    public int getTotalLatencyFrames() {
    	return totalLatencyFrames + 2 * hardwareLatencyFrames;
    }
    
    public void run() {
        try {
            hasStopped = false;
            isRunning = true;
            client.setEnabled(true);
            long startTimeNanos;
			long endTimeNanos;
            long expiryTimeNanos = strategy.nanoTime(); // =System.nanoTime(); // init required for jitter
            long compensationNanos = 0;
            float jitterMillis;
            float lowLatencyMillis;

            while (isRunning) {
                startTimeNanos = strategy.nanoTime();

                // calculate timing jitter
                jitterMillis = (float)(startTimeNanos - expiryTimeNanos) / ONE_MILLION;
                if ( jitterMillis > maximumJitterMilliseconds ) {
                    maximumJitterMilliseconds = jitterMillis;
                }

                sync(); // e.g. resize buffers if requested
                work();
                endTimeNanos = strategy.nanoTime();
                assert(endTimeNanos >= startTimeNanos);
                // calculate client load
                load = (float)(endTimeNanos - startTimeNanos) / totalTimeNanos;
                strategy.notifyLoad(startTimeNanos,endTimeNanos,totalTimeNanos);
                if ( load > peakLoad ) {
                    peakLoad = load;
                }

                // calculate actual latency
				outputLatencyFrames = syncLine.getLatencyFrames();
		    	totalLatencyFrames = outputLatencyFrames + getInputLatencyFrames();
				actualLatencyMilliseconds = 1000 * outputLatencyFrames / getSampleRate();
                lowLatencyMillis = actualLatencyMilliseconds - bufferMilliseconds;
                if ( lowLatencyMillis < bufferUnderRunThreshold ) {
                    if ( started ) {
                    	bufferUnderRuns += 1;
                    	stableCount = 0;
                    }
                } else {
                    stableCount +=1;
                    if ( stableCount == stableThreshold ) { // !!! OK and every 49 days !!!
	                    started = true;
                        controlGained();
                    }
                }
                if ( lowLatencyMillis < lowestLatencyMilliseconds ) {
                    lowestLatencyMilliseconds = lowLatencyMillis;
                }
				if ( stableCount == 0 ) continue; // fast control stabilisation

                // calculate the latency control loop
                compensationNanos = (long)(ONE_MILLION * (actualLatencyMilliseconds - latencyMilliseconds));
                expiryTimeNanos = startTimeNanos + totalTimeNanos + compensationNanos;

                // block
                long now = strategy.nanoTime();
                long sleepNanos = expiryTimeNanos - now;

                // never block for more than 20ms
                if ( sleepNanos > 20000000 ) {
                    sleepNanos = 20000000;
                    expiryTimeNanos = now + sleepNanos;
                }
                if ( sleepNanos > 500000 ) {
                  //  timingStrategy.block(now, sleepNanos);
                	strategy.block(now,sleepNanos);
                } else {
                    expiryTimeNanos = now;
                }
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        hasStopped = true;
        client.setEnabled(false);
//        System.out.println("Thread stopped");
    }

    /**
     * Called synchronously with the server to simplify concurrency issues.
     */
    protected void sync() {
        if ( bufferMilliseconds != requestedBufferMilliseconds ) {
            bufferMilliseconds = requestedBufferMilliseconds;
            totalTimeNanos = (long)(bufferMilliseconds * 1000000);
            resizeBuffers();
        }
        if ( requestedTimingStrategy ) {
     //        timingStrategy = requestedTimingStrategy;
             strategy.setPriority();
   //          thread.setPriority(timingStrategy.getThreadPriority());
             requestedTimingStrategy = false;
        }
        if ( requestResetMetrics ) {
            reset();
            requestResetMetrics = false;
        }
    }

    /**
     * Called when the control loop gains control.
     */
    protected void controlGained() {
        resetMetrics(false);
    }

    public void resetMetrics(boolean resetUnderruns) {
        requestResetMetrics = true;
        if ( resetUnderruns ) {
        	bufferUnderRuns = 0;
        }
    }

    protected void reset() {
        lowestLatencyMilliseconds = actualLatencyMilliseconds;
        maximumJitterMilliseconds = 0;
        // underruns can't be reset here because underruns cause reset
        peakLoad = 0;
    }

    /**
     * Set the software output latency request in milliseconds.
     * This is the demand to the control loop.
     */
    public void setLatencyMilliseconds(float ms) {
        latencyMilliseconds = ms;
        // reset other metrics synchronously
        resetMetrics(false);
    }

    /**
     * Return the requested software output latency in milliseconds.
     */
    public float getLatencyMilliseconds() {
        return latencyMilliseconds;
    }

    /**
     * Return the actual instantaneous software output latency in milliseconds.
     * This is the controlled amount which will diverge from the requested
     * amount due to instantaneous control error caused by timing jitter.
     */
    public float getActualLatencyMilliseconds() {
        return actualLatencyMilliseconds;
    }

    /**
     * Because latency is measured just after writing a buffer and represents
     * the maximum latency, the lowest latency has to be compensated by the
     * duration of the buffer.
     * This might not be the best place to do the compensation but it is the
     * cheapest. While bufferMilliseconds is effectively immutable it's ok.
     */
    public float getLowestLatencyMilliseconds() {
        return lowestLatencyMilliseconds;
    }

    /**
     * Return the minimum software output latency which may be requested.
     */
    public float getMinimumLatencyMilliseconds() {
        return bufferUnderRunThreshold + 5f;
    }

    /**
     * Return the maximum software output latency which may be requested.
     */
    public float getMaximumLatencyMilliseconds() {
    	return maximumLatencyMilliseconds;
    }

    /**
     * Return the maximum control error due to timing jitter, in milliseconds.
     */
    public float getMaximumJitterMilliseconds() {
        return maximumJitterMilliseconds;
    }

    /**
     * Return the number of buffer underruns which may have resulted in an audio
     * glitch.
     */
    public int getBufferUnderRuns() {
        return bufferUnderRuns;
    }

    /**
     * Return the current normalised CPU load caused by the server (0..1)
     */
    public float getLoad() {
        return load;
    }

    /**
     * Return the peak normalised CPU load caused by the server (0..1)
     */
    public float getPeakLoad() {
        return peakLoad;
    }

    /**
     * Return the duration of the buffers in milliseconds.
     */
    public float getBufferMilliseconds() {
        return bufferMilliseconds;
    }

    /**
     * Set the duration of the buffers in milliseconds.
     */
    public void setBufferMilliseconds(float ms) {
        requestedBufferMilliseconds = ms;
    }

    /**
     * Set the timing strategy used by the control loop.
     * @param strategy
     */
//    public void setTimingStrategy(AudioTimingStrategy strategy) {
//        requestedTimingStrategy = strategy;
//    }

    /**
     * Called when buffers are resized.
     */
    protected abstract void resizeBuffers();
}
