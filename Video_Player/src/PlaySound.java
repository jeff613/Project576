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
    private long currentTime = 0;
    
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
    	currentTime = 0;
    }
    
    public void pause()
    {
    	audioClip.stop();
    	currentFrame = audioClip.getFramePosition();
    	currentTime = audioClip.getMicrosecondPosition();
    }
    
    public void play()
    {
    	audioClip.setFramePosition(currentFrame);
    	audioClip.setMicrosecondPosition(currentTime);
    	audioClip.start();
    }
    
    public long getLength() 
    {
    	return audioClip.getMicrosecondLength();
    }
    
    public long getPos() 
    {
    	return audioClip.getMicrosecondPosition();
    }
    
    public void setPos(long currPos)
    {
    	currentTime = currPos;
    	audioClip.setMicrosecondPosition(currentTime);
    }
    
    public void flush()
    {
    	audioClip.flush();
    }
    
    public float getLevel()
    {
    	return audioClip.getLevel();
    }
    
    public void sleep(long miliseconds)
    {
    	try {
    		synchronized (audioClip) {
    			audioClip.wait(miliseconds);
    		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
