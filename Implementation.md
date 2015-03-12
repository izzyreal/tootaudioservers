Implementing AudioServer Services

# Introduction #

It is necessary to implement various classes to provide a plugin AudioServer. Although there are several classes many of them are quite simple and abstract implementations are often provided.

# Details #

  * Implement uk.org.toot.audio.server.AudioServer
> You should be able extend AbstractAudioServer which encapsulates everything regarding AudioClient and AudioBuffer management and delayed start. TimedAudioServer provides a latency control loop with user adjustable latency. JavaSoundAudioServer concretizes AudioServer to provide inputs and outputs using JavaSound.
  * Concretize uk.org.toot.audio.server.AudioServerConfiguration for setup and configuration use, setup being optional.
> See JavaSoundAudioServerSetup for a typical setup implementation.
> See ExtendedAudioServerConfiguration for a typical configuration implementation.
  * Implement uk.org.toot.audio.server.spi.AudioServerServiceProvider
> See TootAudioServerServiceProvider for a typical implementation.
> Override createServerConfiguration() and optionally createServerSetup(), returning null if the passed AudioServer is not provided by you.
  * Implement UIs
> The UI is a JComponent, typically a JPanel.
> See JavaSoundAudioServerSetupPanel for a setup implementation.
> See AudioServerPanel for a rich configuration implementation.
  * Implement uk.org.toot.swingui.audioui.serverui.spi.AudioServerUIServiceProvider
> See TootAudioServerUIServiceProvider for a typical implementation.
> Override createServerUI() to provide the configuration UI and optionally override createSetupUI() to provide the setup UI, returning null if the passed AudioServer is not provided by you.
  * Add META-INF/services files
> uk.org.toot.audio.server.spi.AudioServerServiceProvider and
> uk.org.toot.swingui.audioui.serverui.spi.AudioServerUIServiceProvider