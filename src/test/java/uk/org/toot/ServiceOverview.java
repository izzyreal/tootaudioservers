package uk.org.toot;

import junit.framework.TestCase;
import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.AudioServerServices;
import uk.org.toot.service.ServicePrinter;
import uk.org.toot.service.ServiceVisitor;

/**
 * Run this to see which audio servers are available
 */

public class ServiceOverview extends TestCase {

    private ServiceVisitor serviceVisitor = new ServicePrinter();

    public void testAudioServices() {
        System.out.println("\nAudioServerServices:");
        AudioServerServices.accept(serviceVisitor, AudioServer.class);
    }

}
