package uk.org.toot;

import junit.framework.TestCase;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.mixer.AudioMixer;
import uk.org.toot.audio.mixer.MixerControls;
import uk.org.toot.audio.mixer.MixerControlsFactory;
import uk.org.toot.audio.server.IOAudioProcess;
import uk.org.toot.audio.server.JavaSoundAudioServer;
import uk.org.toot.audio.system.AudioSystem;
import uk.org.toot.audio.system.MixerConnectedAudioSystem;
import uk.org.toot.midi.core.DefaultConnectedMidiSystem;
import uk.org.toot.midi.core.MidiSystem;
import uk.org.toot.synth.SynthRack;
import uk.org.toot.synth.SynthRackControls;
import uk.org.toot.synth.channels.valor.ValorSynthControls;
import uk.org.toot.synth.synths.multi.MultiMidiSynth;
import uk.org.toot.synth.synths.multi.MultiSynthControls;

import javax.sound.midi.ShortMessage;

/**
 * Play 1 second soft note from Valor Synth on Default Audio Device using JavaSoundAudioServer
 */

public class FullSetup extends TestCase {

    private JavaSoundAudioServer audioServer = new JavaSoundAudioServer();

    private MultiMidiSynth multiMidiSynth;

    public void testFullSetup() {
        try {
            MixerControls mainMixerControls = new MixerControls("Mixer");

            MixerControlsFactory.createBusStrips(mainMixerControls, "L-R", ChannelFormat.STEREO, 0);

            final int channelCount = 32;
            MixerControlsFactory.createChannelStrips(mainMixerControls, channelCount);

            AudioMixer audioMixer = new AudioMixer(mainMixerControls, audioServer);

            MixerConnectedAudioSystem audioSystem = new MixerConnectedAudioSystem(audioMixer);
            audioSystem.setAutoConnect(true);

            IOAudioProcess defaultAudioOutput = audioServer.openAudioOutput(audioServer.getAvailableOutputNames().get(0), "Default Audio Device");
            audioMixer.getMainBus().setOutputProcess(defaultAudioOutput);

            DefaultConnectedMidiSystem midiSystem = new DefaultConnectedMidiSystem();
            insertSynth(midiSystem, audioSystem);

            audioServer.setClient(audioMixer);

            audioServer.start();

            multiMidiSynth.getMidiInputs().get(0).transport(new ShortMessage(), 0);

            Thread.sleep(1000);

            audioServer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertSynth(MidiSystem midiSystem, AudioSystem audioSystem) {
        MultiSynthControls multiSynthControls = new MultiSynthControls();
        SynthRackControls synthRackControls = new SynthRackControls(1);
        SynthRack synthRack = new SynthRack(synthRackControls, midiSystem, audioSystem);
        synthRackControls.setSynthControls(0, multiSynthControls);
        multiMidiSynth = (MultiMidiSynth) synthRack.getMidiSynth(0);
        multiSynthControls.setChannelControls(0, new ValorSynthControls());
    }
}
