import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.*;
import java.io.*;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class VideoPlayer
{
	
	private String homePath;
	private String videoFilePath;
	private String audioFilePath;
	private String testStripPath;
	private long microsecondLength;
	
	private int width = 352;
	private int height = 288;
	private int fps = 24;
	private int frameCount = 720;
	private int frameNum = 0;
	private double scale = 0.2;
	private int frameInterval = 40;
	private int framesPerStrip = 18;
	private int stripFrameW = 70;
	private int stripFrameH = 57;
	
	private byte[][] framesCache;
	private PlaySound audioPlayer;
	
	private JFrame frame;
	private JPanel videoPanel;
	private JPanel stripPanel;
	
	private Timer timer;
	private DrawFrameTask drawFrameTask;
	
	public static void main(String[] args) 
	{
	   	String videoPath = args[0];
	   	String audioPath = args[1];
	   	//String testStripPath = args[2];
	   	
	   	VideoPlayer player = new VideoPlayer(videoPath, audioPath, "");
	}
	
	public VideoPlayer(String vPath, String aPath, String tPath)
	{
		videoFilePath = vPath;
		audioFilePath = aPath;
		testStripPath = tPath;
		
		// Use a label to display the image
	    frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    //frame.setLayout(new GridLayout(3, 2));
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
			audioPlayer.setPos(0);
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

		if (audioPlayer != null)
			audioPlayer.flush();
		audioPlayer = new PlaySound(inputStream);
		microsecondLength = audioPlayer.getLength();
		System.out.println("The length will be: " + microsecondLength);
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
		JButton[] button_arr = new JButton[18];
		stripPanel = new JPanel();
		stripPanel.setPreferredSize(new Dimension(width + 800, (int) (height * 1.5)));
		stripPanel.setLayout(new GridLayout(0, 18));
	    frame.getContentPane().add(stripPanel, BorderLayout.SOUTH);
		int count = 0;
		for (int i = 0; i < frameCount; i += frameInterval)
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
    		    	  int num_of_frame_set = Integer.parseInt(((JButton) (me.getSource())).getName());
    		    	  int num_of_frame = (int) num_of_frame_set * frameInterval;
    		    	  stop();
    		    	  long currPos = num_of_frame_set * (microsecondLength / framesPerStrip);
    		    	  audioPlayer.setPos(currPos);
    		    	  
    		    	  //System.out.println("Frame num" + num_of_frame);
    		    	  play(num_of_frame);
    		      }
    		});
    		
    		button_arr[count].setFocusPainted(false);
    		button_arr[count].setMargin(new Insets(0, 0, 0, 0));
    		button_arr[count].setContentAreaFilled(false);
    		button_arr[count].setBorderPainted(false);
    		button_arr[count].setOpaque(false);
    		
    		stripPanel.add(button_arr[count]);
    	    frame.pack();
    	    count++;
		}
	}
	
	private byte[][] loadStripFromFile(String matchVideo, int width, int height, int frameCount)
	{
		byte[][] stripCache = new byte[frameCount][];
		
		try
	    {
		    File file = new File(matchVideo);
		    InputStream is = new FileInputStream(file);
	
		    long len = file.length();
		    int frameLen = width * height * 3;
		    
		    for (int i = 0; i < frameCount; i++)
		    {
		    	int offset = 0;
		        int numRead = 0;
		        
		        stripCache[i] = new byte[frameLen];
		        
		        while(offset < frameLen && (numRead = is.read(stripCache[i], offset, frameLen - offset)) >= 0)
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
		
		return stripCache;
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
	
	private void copyToCache(byte[][] stripFrames)
	{
		for (int i = 0; i < stripFrames.length; i++)
		{
			for (int j = 0; j < stripFrames[0].length; j++)
			{
				this.framesCache[i][j] = stripFrames[i][j];
			}
		}
		
	}
	
	private void loadResults(String matchVideo, int matchFrame)
	{
		if (matchVideo != "")
		{
			final byte[][] stripFrames = loadStripFromFile(matchVideo, stripFrameW, stripFrameH, framesPerStrip);
			JButton[] button_arr = new JButton[framesPerStrip];
			int count = 0;
			BufferedImage newImg = null;
		
			//Calculate the frame to be selected and the offset
			int j = (int) (matchFrame / frameInterval) - 1;
			double div = (double) matchFrame / (double) frameInterval;
			double rem = div - j - 1;
			int offset = (int) (stripFrameW * rem);
		
			for (int i = 0; i < framesPerStrip; i += 1)
			{
				newImg = getFrameFromCache(0, 0, stripFrameW, stripFrameH, stripFrames[i]);
    		
				//Use JButton to make click-able images in the strip
				button_arr[count] = new JButton("",new ImageIcon(newImg));
				button_arr[count].setPreferredSize(new Dimension(stripFrameW, stripFrameH));
				button_arr[count].setName(String.valueOf(count));
				button_arr[count].addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						System.out.println("Frame clicked! Changing to new video..");
						
						//Momentarily stop the video
						stop();
						
						//Change videoFilePath and audioFilePath
						StringTokenizer st2 = new StringTokenizer(testStripPath, "\\");
						String stripName = "";
						homePath = "";
						int count_tok = 0;
						int total_tokens = st2.countTokens();
						while (st2.hasMoreTokens() && (count_tok < total_tokens))
						{
							String curr = st2.nextToken();
							if (count_tok < (total_tokens - 1))
								homePath += curr + "/";
							stripName = curr;
							count_tok++;
						}
						StringTokenizer st = new StringTokenizer(stripName, ".");
						int num_video = Integer.parseInt(st.nextToken().substring(5));
						System.out.println("Changing to video number: " + num_video);
						videoFilePath = homePath + "vdo" + num_video + ".rgb";
						audioFilePath = homePath + "vdo" + num_video + ".wav";
						
						//Set the current frame and audio correctly
						int num_of_frame_set = Integer.parseInt(((JButton) (me.getSource())).getName());
						int num_of_frame = (int) num_of_frame_set * frameInterval;
						
						loadFrames();
						loadAudio();
						long currPos = num_of_frame_set * (microsecondLength / framesPerStrip);
	    		    	audioPlayer.setPos(currPos);
						play(num_of_frame);
    		      }
				});
				button_arr[count].setFocusPainted(false);
				button_arr[count].setMargin(new Insets(0, 0, 0, 0));
				button_arr[count].setContentAreaFilled(false);
				button_arr[count].setBorderPainted(false);
				button_arr[count].setOpaque(false);
				stripPanel.add(button_arr[count]);
				frame.pack();
				count++;
    	    
				if (i == j)
				{
					RectangularShape rs = new Rectangle();
					rs.setFrame(offset, 0, stripFrameW, stripFrameH);
    			
					Graphics2D g2d = newImg.createGraphics();
					g2d.setStroke (new BasicStroke(6));
					g2d.setColor (Color.red);
					g2d.draw(rs);
				}
    	    
			}
		}
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
				index = y * w + x;
				byte a = 0;
				byte r = cache[index];
				byte g = cache[index + h * w];
				byte b = cache[index + h * w * 2]; 
				
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
		else if (name.equals("Search"))
		{
			loadResults(testStripPath, 256);
		}
	}
	
	class DrawFrameTask extends TimerTask
	{
		private int count = 0;
		private int GOP = 1;
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
				count++;
				//Check every other frame if we are out of sync
				if (count == frameInterval)
				{
					long expectedAudioPos = GOP * (frameInterval) * (microsecondLength / frameCount);
					long actualAudioPos = audioPlayer.getPos();
					//audioPlayer.setPos(expectedAudioPos);
					
					long difference = actualAudioPos - expectedAudioPos;
					if (difference > 0)
					{
						//System.out.println("The expected: " + expectedAudioPos + "the actual pos: " + actualAudioPos);
						//System.out.println("The difference was: " + difference);
						if (difference > (((frameInterval) * (microsecondLength / frameCount)) / 20))
							audioPlayer.sleep((((frameInterval) * (microsecondLength / frameCount)) / 20) / 1000);
						else
							audioPlayer.sleep(difference / 1000);
					}
					else
						//System.out.println("The difference was: " + difference);
					
					GOP++;
					count = 0;
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
