/* Generated by Together */

package com.frinika.toot;

import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.AudioServerConfiguration;
import uk.org.toot.audio.server.ExtendedAudioServer;
import uk.org.toot.audio.server.ExtendedAudioServerConfiguration;
import uk.org.toot.audio.server.spi.AudioServerServiceProvider;
import uk.org.toot.audio.id.ProviderId;

import com.frinika.toot.javasoundmultiplexed.MultiplexedJavaSoundAudioServer;

public class FrinikaAudioServerServiceProvider extends AudioServerServiceProvider
{
    public FrinikaAudioServerServiceProvider() {
        super(ProviderId.FRINIKA_PROVIDER_ID, "Frinika", "Frinika Audio Servers", "0.1");
		// Multiplexed isn't possible on Windows, just a Linux issue? Or OS X too?
        if (System.getProperty("os.name").equals("Linux")) {
			add(MultiplexedJavaSoundAudioServer.class, "JavaSound (multiplexed)", "multiplexed", "0.2");
		}	
    }

    public AudioServerConfiguration createServerConfiguration(AudioServer server) {	
    	if ( server instanceof MultiplexedJavaSoundAudioServer ) {
    		return new ExtendedAudioServerConfiguration((ExtendedAudioServer)server);
    	}
    	return null; // try another provider
    }
}
