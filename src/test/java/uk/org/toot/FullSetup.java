package uk.org.toot;

import junit.framework.TestCase;
import uk.org.toot.audio.core.AudioControlsChain;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.fader.FaderControl;
import uk.org.toot.audio.mixer.AudioMixer;
import uk.org.toot.audio.mixer.MixerControls;
import uk.org.toot.audio.mixer.MixerControlsFactory;
import uk.org.toot.audio.server.IOAudioProcess;
import uk.org.toot.audio.server.JavaSoundAudioServer;
import uk.org.toot.audio.system.MixerConnectedAudioSystem;
import uk.org.toot.control.CompoundControl;
import uk.org.toot.midi.core.DefaultConnectedMidiSystem;
import uk.org.toot.synth.SynthRack;
import uk.org.toot.synth.SynthRackControls;
import uk.org.toot.synth.channels.valor.ValorSynthControls;
import uk.org.toot.synth.synths.multi.MultiMidiSynth;
import uk.org.toot.synth.synths.multi.MultiSynthControls;

import javax.sound.midi.ShortMessage;

/**
 * Play soft note from Valor Synth on Default Audio Device using JavaSoundAudioServer
 */

public class FullSetup extends TestCase {

    private MixerControls mainMixerControls;
    private MixerConnectedAudioSystem audioSystem;
    private JavaSoundAudioServer audioServer = new JavaSoundAudioServer();

    private DefaultConnectedMidiSystem midiSystem = new DefaultConnectedMidiSystem();
    private MultiMidiSynth multiMidiSynth;

    public void testFullSetup() {
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialize() throws Exception {
        mainMixerControls = new MixerControls("Mixer");

        final ChannelFormat channelFormat = ChannelFormat.STEREO;

        mainMixerControls.createAuxBusControls("AUX#1", channelFormat);
        mainMixerControls.createAuxBusControls("AUX#2", channelFormat);
        mainMixerControls.createAuxBusControls("AUX#3", channelFormat);
        mainMixerControls.createAuxBusControls("AUX#4", channelFormat);

        final int returnCount = 2;
        MixerControlsFactory.createBusStrips(mainMixerControls, "L-R", channelFormat, returnCount);

        final int channelCount = 32;
        MixerControlsFactory.createChannelStrips(mainMixerControls, channelCount);

        AudioMixer audioMixer = new AudioMixer(mainMixerControls, audioServer);

        audioSystem = new MixerConnectedAudioSystem(audioMixer);
        audioSystem.setAutoConnect(true);

        setFaderLevel("L-R", 5);

        IOAudioProcess defaultAudioOutput = audioServer.openAudioOutput(audioServer.getAvailableOutputNames().get(0), "Default Audio Device");
        audioMixer.getMainBus().setOutputProcess(defaultAudioOutput);

        insertSynth();

        audioServer.setClient(audioMixer);

        audioServer.start();

        multiMidiSynth.getMidiInputs().get(0).transport(new ShortMessage(), 0);

        Thread.sleep(1000);

        audioServer.stop();

    }

    private void setFaderLevel(String faderName, int i) {
        AudioControlsChain sc = mainMixerControls.getStripControls(faderName);
        CompoundControl cc = (CompoundControl) sc.getControls().get(0);
        ((FaderControl) cc.getControls().get(2)).setValue(i);
    }

    private void insertSynth() {
        MultiSynthControls multiSynthControls = new MultiSynthControls();
        SynthRackControls synthRackControls = new SynthRackControls(1);
        SynthRack synthRack = new SynthRack(synthRackControls, midiSystem, audioSystem);
        synthRackControls.setSynthControls(0, multiSynthControls);
        multiMidiSynth = (MultiMidiSynth) synthRack.getMidiSynth(0);

        for (int i = 0; i < 4; i++) {
            multiSynthControls.setChannelControls(i, new ValorSynthControls());
        }
    }
}
