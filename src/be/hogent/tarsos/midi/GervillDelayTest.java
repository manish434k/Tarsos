package be.hogent.tarsos.midi;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;

import be.hogent.tarsos.ui.PianoTestFrame;


public class GervillDelayTest {
	
	public static void main(String[] args) throws Exception{
		Receiver recv;
		MidiDevice outputDevice = PlayAlong.getMidiDeviceInfo("Gervill",true);
		outputDevice.open();
		recv = outputDevice.getReceiver();
		MidiDevice midiInputDevice = PlayAlong.getMidiDeviceInfo("LoopBe Internal MIDI",false);
		midiInputDevice.open();
		Transmitter	midiInputTransmitter = midiInputDevice.getTransmitter();
		
		recv = new ReceiverSink(true,recv , new DumpReceiver(System.out));
		midiInputTransmitter.setReceiver(recv);
		
		
		ShortMessage msg = new ShortMessage();
		
		double tunings[] = new double[128];
		for(int i= 1 ;i < 128 ; i++){
			tunings[i] = tunings[i-1] + 240;
		}
		
		PianoTestFrame.sendTunings(recv, 0, 0, "test", tunings);
		PianoTestFrame.sendTuningChange(recv, 0, 0);
		msg.setMessage(ShortMessage.NOTE_ON,0,69,100);		
		recv.send(msg, -1);		
		msg.setMessage(ShortMessage.NOTE_OFF,0,69,100);
		recv.send(msg, -1);
		new JFrame().setVisible(true);		
	}
}
