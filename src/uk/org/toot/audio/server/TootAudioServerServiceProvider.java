// Copyright (C) 2007 Steve Taylor.
// Distributed under the Toot Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.toot.org/LICENSE_1_0.txt)


package uk.org.toot.audio.server;

import java.util.List;

import uk.org.toot.audio.server.spi.AudioServerServiceProvider;
import uk.org.toot.audio.id.ProviderId;
import uk.org.toot.service.ServiceDescriptor;

import com.synthbot.jasiohost.*;

public class TootAudioServerServiceProvider extends AudioServerServiceProvider
{
    public TootAudioServerServiceProvider() {
        super(ProviderId.TOOT_PROVIDER_ID, "Toot Software", "Toot Audio Servers", "0.1");
//        add(JavaSoundAudioServer.class, "JavaSound (stereo)", "default stereo", "0.4");
        add(MultiIOJavaSoundAudioServer.class, "JavaSound (stereo)", "stereo", "0.1");
        // if ASIO available add ASIO providers
        addASIO();
    }
    
    public AudioServerConfiguration createServerConfiguration(AudioServer server) {
    	if ( server instanceof JavaSoundAudioServer ||
    		 server instanceof MultiIOJavaSoundAudioServer ) {
    		return new ExtendedAudioServerConfiguration((ExtendedAudioServer)server);
    	}
    	return null;
    }
    
    protected void addASIO() {
        String osName = System.getProperty("os.name");
        if ( !osName.contains("Windows") ) return;
    	List<String> driverNames = JAsioHost.getDriverNames();
    	for ( String name : driverNames ) {
    		add(ASIOAudioServer.class, name, "ASIO", "0.1");
    	}
    }
    
    public AudioServer createServer(String name) {
        for ( ServiceDescriptor d : servers ) {
            if ( d.getName().equals(name) && d.getServiceClass().equals(ASIOAudioServer.class) ) {
   	            return new ASIOAudioServer(name);
       	    }
        }
    	return super.createServer(name);
    }
}
