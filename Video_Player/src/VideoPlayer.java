import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.*;
import java.io.*;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class VideoPlayer
{
	private String videoFilePath;
	private String audioFilePath;
	
	private int width = 352;
	private int height = 288;
	private int fps = 24;
	private int frameCount = 720;
	private int frameNum = 0;
	private double scale = 0.13;
	
	private byte[][] framesCache;
	private PlaySound audioPlayer;
	
	private JFrame frame;
	private JPanel videoPanel;
	
	private Timer timer;
	private DrawFrameTask drawFrameTask;
	
	public static void main(String[] args) 
	{
	   	String videoPath = args[0];
	   	String audioPath = args[1];
	   	
	   	VideoPlayer player = new VideoPlayer(videoPath, audioPath);
	}
	
	public VideoPlayer(String vPath, String aPath)
	{
		videoFilePath = vPath;
		audioFilePath = aPath;
		
		// Use a label to display the image
	    frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    videoPanel = new JPanel();
	    frame.getContentPane().add(videoPanel, BorderLayout.CENTER);

	    // Buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setPreferredSize(new Dimension(400, height));
	    frame.getContentPane().add(buttonPanel, BorderLayout.EAST);
		
		MyButton splitButton = new MyButton("Play");
		buttonPanel.add(splitButton, BorderLayout.NORTH);

		MyButton initButton = new MyButton("Stop");
		buttonPanel.add(initButton, BorderLayout.NORTH);
		
		MyButton resetButton = new MyButton("Pause");
		buttonPanel.add(resetButton, BorderLayout.NORTH);
		
		MyButton closeButton = new MyButton("Search");
		buttonPanel.add(closeButton, BorderLayout.NORTH);
		
		frame.setVisible(true);
		frame.setResizable(false);
		
		loadFrames();
		loadAudio();
		loadStrip();
		
		play();
	}
	
	public void play()
	{
		playVideo();
		playAudio();
	}
	
	public void play(int num_of_frame)
	{
		this.frameNum = num_of_frame;
		playVideo();
		playAudio();
	}
	
	public int getFrameNum()
	{
		return this.frameNum;
	}
	
	private void playVideo()
	{
		
		timer = new Timer();
		drawFrameTask = new DrawFrameTask();
		timer.schedule(drawFrameTask, 0, 1000 / fps);
	}
	
	private void stopVideo()
	{
		if (timer != null)
		{
			timer.cancel();
			frameNum = 0;
			displayFrame(0);
			timer = null;	
		}
	}
	
	private void pauseVideo()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}
	
	private void playAudio()
	{

		audioPlayer.play();
	}
	
	private void stopAudio()
	{
		if (audioPlayer != null)
		{
			audioPlayer.stop();
		}
	}
	
	private void pauseAudio()
	{
		if (audioPlayer != null)
		{
			audioPlayer.pause();
		}
	}
	
	private void loadAudio()
	{
		FileInputStream inputStream;
		try
		{
		    inputStream = new FileInputStream(audioFilePath);
		}
		catch (FileNotFoundException e)
		{
		    e.printStackTrace();
		    return;
		}

		audioPlayer = new PlaySound(inputStream);
	}
	
	private void loadFrames()
	{
		framesCache = new byte[frameCount][];
		
		try
	    {
		    File file = new File(videoFilePath);
		    InputStream is = new FileInputStream(file);
	
		    long len = file.length();
		    int frameLen = width * height * 3;
		    
		    for (int i = 0; i < frameCount; i++)
		    {
		    	int offset = 0;
		        int numRead = 0;
		        
		        framesCache[i] = new byte[frameLen];
		        
		        while(offset < frameLen && (numRead = is.read(framesCache[i], offset, frameLen - offset)) >= 0)
		        {
		            offset += numRead;
		        }
		    }
		    
	    }
		catch (FileNotFoundException e)
		{
	      e.printStackTrace();
	    }
		catch (IOException e)
		{
	      e.printStackTrace();
	    }
	}
	
	private void loadStrip()
	{
		JButton[] button_arr = new JButton[15];
		JPanel stripPanel = new JPanel();
		stripPanel.setPreferredSize(new Dimension(width + 300, height / 4));
	    frame.getContentPane().add(stripPanel, BorderLayout.SOUTH);
		int count = 0;
		for (int i = 0; i < frameCount; i += 50)
		{
			BufferedImage originalImg = getFrameFromCache(0, 0, width, height, framesCache[i]);
			
			//First, sub-sample image to get the right size for the strip
			BufferedImage newImg;
			int newWidth = (int) (width * scale);
    		int newHeight = (int) (height * scale);
    		newImg = resizeImage(originalImg, newWidth, newHeight, true);
    		
    		//Use JButton to make click-able images in the strip
    		button_arr[count] = new JButton("",new ImageIcon(newImg));
    		button_arr[count].setPreferredSize(new Dimension(newWidth, newHeight));
    		button_arr[count].setName(String.valueOf(count));
    		button_arr[count].addMouseListener(new MouseAdapter() {
    		      public void mouseClicked(MouseEvent me) {
    		    	  //System.out.println("CLICKED: " + ((JButton) (me.getSource())).getName());
    		    	  int num_of_frame_set = Integer.parseInt(((JButton) (me.getSource())).getName());
    		    	  int num_of_frame = (int) num_of_frame_set * 50;
    		    	  stop();
    		    	  //System.out.println("Frame num" + num_of_frame);
    		    	  play(num_of_frame);
    		      }
    		});
    		stripPanel.add(button_arr[count]);
    	    frame.pack();
    	    count++;
		}
	}
	
	private BufferedImage resizeImage(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha)
	{
		int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, imageType);
		Graphics2D g = scaledImage.createGraphics();
		if (preserveAlpha)
				g.setComposite(AlphaComposite.Src);
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null); 
		g.dispose();
		return scaledImage;
	}
	
	private void displayFrame(int fNum)
	{
		videoPanel.removeAll();
		videoPanel.setLayout(new FlowLayout());
		
		BufferedImage originalImg = getFrameFromCache(0, 0, width, height, framesCache[fNum]);
	    JLabel videoLabel = new JLabel(new ImageIcon(originalImg));
	    videoLabel.setPreferredSize(new Dimension(width, height));
	    videoPanel.add(videoLabel, BorderLayout.CENTER);
	    
	    frame.pack();
	}
	
	private BufferedImage getFrameFromCache(int xl, int yl, int w, int h, byte[] cache)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int index;
		for(int y = yl; y < yl + h; y++)
		{			
			for(int x = xl; x < xl + w; x++)
			{
				index = y * width + x;
				byte a = 0;
				byte r = cache[index];
				byte g = cache[index + height * width];
				byte b = cache[index + height * width * 2]; 
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x - xl, y - yl, pix);
			}
		}
		return img;
	}
	
	public void stop()
	{
		stopVideo();
		stopAudio();
	}
	
	public void pause()
	{
		pauseVideo();
		pauseAudio();
	}
	
	public void buttonPressed(String name)
	{
		if (name.equals("Play"))
		{
			play();
		}
		else if (name.equals("Stop"))
		{
			stop();
		}
		else if (name.equals("Pause"))
		{
			pause();
		}
	}
	
	class DrawFrameTask extends TimerTask
	{
		public void run()
		{
		
			if (!EventQueue.isDispatchThread())
			{
				EventQueue.invokeLater(this);
			}
			else
			{
				displayFrame(frameNum);
				frameNum++;
				if (frameNum == frameCount)
				{
					stopVideo();
				}
			}
		}
	}
	
	class MyButton extends JButton
	{
		MyButton(String label)
		{
			setFont(new Font("Helvetica", Font.BOLD, 10));
			setText(label);
			addMouseListener(
				new MouseAdapter()
				{
	  				public void mousePressed(MouseEvent e) 
	  				{
						buttonPressed(getText());
					}
				}
			);
		}
		
		MyButton(String label, ImageIcon icon)
		{
			Image img = icon.getImage();
			Image scaleimg = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
			setIcon(new ImageIcon(scaleimg));
			setText(label);
			setFont(new Font("Helvetica", Font.PLAIN, 0));
			addMouseListener(
				new MouseAdapter()
				{
	  				public void mousePressed(MouseEvent e)
	  				{
						buttonPressed(getText());
					}
				}
			);
		}
	}
}
