import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

/**
 * 
 * <Replace this with a short description of the class.>
 * 
 * @author Giulio
 */
public class PlaySound {

    private InputStream waveStream;
    private AudioInputStream audioInputStream;
    private Clip audioClip;
    private int currentFrame = 0;
    
    private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

    /**
     * CONSTRUCTOR
     */
    public PlaySound(InputStream is)
    {
    	waveStream = is;
    	try
    	{
			loadSound();
		}
    	catch (PlayWaveException e)
    	{
			e.printStackTrace();
		}
    }

    private void loadSound() throws PlayWaveException
    {
    	audioInputStream = null;
    	try
    	{
    		audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(this.waveStream));
    	}
    	catch (UnsupportedAudioFileException e1)
    	{
    	    throw new PlayWaveException(e1);
    	}
    	catch (IOException e1)
    	{
    	    throw new PlayWaveException(e1);
    	}

    	try
    	{
			audioClip = AudioSystem.getClip();
			audioClip.open(audioInputStream);
		}
    	catch (LineUnavailableException e)
    	{
			e.printStackTrace();
		}
    	catch (IOException e)
    	{
			e.printStackTrace();
		}
    }
    
    public void stop()
    {
    	audioClip.stop();
    	currentFrame = 0;
    }
    
    public void pause()
    {
    	audioClip.stop();
    	currentFrame = audioClip.getFramePosition();
    }
    
    public void play()
    {
    	audioClip.setFramePosition(currentFrame);
    	audioClip.start();
    }
}
