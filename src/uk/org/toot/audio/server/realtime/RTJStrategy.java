package uk.org.toot.audio.server.realtime;

import javax.realtime.AbsoluteTime;
import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

import uk.org.toot.audio.server.AudioTimingStrategy;
import uk.org.toot.audio.server.SleepTimingStrategy;
import uk.org.toot.audio.server.Strategy;

public class RTJStrategy extends Strategy {

	Thread thread;
//	AudioTimingStrategy timingStrategy;
//	HighResolutionTime time;
	Clock clock;
	AbsoluteTime time;
	static final long ONE_MILLION=1000000;

	public RTJStrategy(){
		clock=Clock.getRealtimeClock();// new AbsoluteTime();
		System.out.println(" Clock resolution is " + clock.getResolution());
		time=new AbsoluteTime();
	}
	/* (non-Javadoc)
	 * @see uk.org.toot.audio.server.Strategy#run(java.lang.Runnable, java.lang.String)
	 */
	public void run(final Runnable runner,String name) {
		
	    PriorityParameters sched = new PriorityParameters(PriorityScheduler.instance().getMaxPriority());
      	thread = new RealtimeThread(sched){
      		public void run() {
      			runner.run();
      		}  		
      	};
       	thread.start();	
	}

	/* (non-Javadoc)
	 * @see uk.org.toot.audio.server.Strategy#setPriority()
	 */
	public void setPriority() {
	//	thread.setPriority(timingStrategy.getThreadPriority());
	}

	/* (non-Javadoc)
	 * @see uk.org.toot.audio.server.Strategy#nanoTime()
	 */
	public long nanoTime() {		
		clock.getTime(time);
		long milliTime = 1000000*time.getMilliseconds();
		return milliTime+time.getNanoseconds(); //.nanoTime();
	}

	@Override
	public void block(long nowNanos, long blockNanos) {
		
		try {
			RealtimeThread.sleep(new RelativeTime(blockNanos / ONE_MILLION, (int)(blockNanos % ONE_MILLION)));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//	     try {
//		        Thread.sleep(blockNanos / ONE_MILLION, (int)(blockNanos % ONE_MILLION));
//	        } catch ( InterruptedException ie ) {
//	        }
	
		
	}
	
	
}
