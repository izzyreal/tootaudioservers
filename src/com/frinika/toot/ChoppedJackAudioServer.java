package com.frinika.toot;

public class ChoppedJackAudioServer extends ChoppedAudioServerAdapter
{
	public ChoppedJackAudioServer() {
		super(new JackAudioServer());
	}
}
