import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Timer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class VideoProcessor {

	private String folderPath;
	private String[] videoFileNames;

	private int width = 352;
	private int height = 288;
	private double scale = 0.2;
	private int miniWidth;
	private int miniHeight;
	
	private int frameCount = 720;
	private int interval = 40;

	private byte[][] framesCache;

	private JFrame frame;
	private JPanel imagePanel;
	private JLabel infoLabel;

	public static void main(String[] args) 
	{
	   	String folderPath = args[0];

	   	VideoProcessor processor = new VideoProcessor(folderPath);
	}
	
	public VideoProcessor(String fPath)
	{
		folderPath = fPath;
		loadColorIndex(folderPath + "\\vdo1.color");
		
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.startsWith("vdo") && name.endsWith(".rgb");
		    }
		};
		File dir = new File(folderPath);
		videoFileNames = dir.list(filter);
		
		miniWidth = (int) (width * scale);
		miniHeight = (int) (height * scale);
		// Use a label to display the image
	    frame = new JFrame();
	    frame.setSize(500, 300);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	    JPanel infoPanel = new JPanel();
	    infoPanel.setSize(400, 100);
	    infoLabel = new JLabel();
	    infoPanel.add(infoLabel);
	    frame.getContentPane().add(infoPanel, BorderLayout.CENTER);
	    
	    imagePanel = new JPanel();
	    imagePanel.setSize(400, 100);
	    frame.getContentPane().add(imagePanel, BorderLayout.SOUTH);

	    // Buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setSize(new Dimension(400, 200));
	    frame.getContentPane().add(buttonPanel, BorderLayout.NORTH);

		JButton stripButton = new MyButton("Generate Strips");
		buttonPanel.add(stripButton, BorderLayout.SOUTH);

		JButton colorButton = new MyButton("Color Indexing");
		buttonPanel.add(colorButton, BorderLayout.SOUTH);

		JButton motionButton = new MyButton("Motion Indexing");
		buttonPanel.add(motionButton, BorderLayout.SOUTH);

		JButton soundButton = new MyButton("Sound Indexing");
		buttonPanel.add(soundButton, BorderLayout.SOUTH);

		frame.setVisible(true);
		frame.pack();
		frame.setResizable(true);
	}
	
	private void loadFrames(String fPath)
	{
		framesCache = new byte[frameCount][];

		try
	    {
		    File file = new File(fPath);
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
	
	private void displayFrame(byte[] img, int w, int h)
	{
		imagePanel.removeAll();
		imagePanel.setLayout(new FlowLayout());

		BufferedImage originalImg = getFrameFromCache(0, 0, w, h, w, h, img);
	    JLabel videoLabel = new JLabel(new ImageIcon(originalImg));
	    videoLabel.setPreferredSize(new Dimension(w, h));
	    imagePanel.add(videoLabel, BorderLayout.CENTER);

	    frame.pack();
	}

	private BufferedImage getFrameFromCache(int xl, int yl, int nw, int nh, int w, int h, byte[] cache)
	{
		BufferedImage img = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
		int index;
		for(int y = yl; y < yl + nh; y++)
		{			
			for(int x = xl; x < xl + nw; x++)
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
	
	private byte[] resize(byte[] cache, int w, int h)
	{
		float row = 0;
		float col = 0;
		int i = 0;
		int j = 0;
		float step = (float) width / (float) w;	//scale <= 1
		int range =  (int) (step - 1);
		int len1 = w * h;
		int len2 = width * height;
		byte[] cacheNew = new byte[len1 * 3];
		while(j * w + i < len1)
		{
			int start1 = w * j;
			int start2 = width * (int) row;
			cacheNew[start1 + i] = cache[(int) (start2 + col)];
			cacheNew[start1 + i + len1] = cache[(int) (start2 + col + len2)];
			cacheNew[start1 + i + len1 + len1] = cache[(int) (start2 + col + len2 + len2)];
			/*cacheNew[start1 + i] = smooth((int) row, (int) col, width, height, range, 0, cache);
			cacheNew[start1 + i + len1] = smooth((int) row, (int) col, width, height, range, len2, cache);
			cacheNew[start1 + i + len1 + len1] = smooth((int) row, (int) col, width, height, range, len2 + len2, cache);*/
			
			if(i > w - 1)
			{
				col = 0;
				i = 0;
				row += step;
				row = row >= height ? height - 1 : row;
				j++;
			}
			else
			{
				col += step;
				col = col >= width ? width - 1 : col;
				i++;
			}
		}
		
		return cacheNew;
	}
	
	private byte smooth(int r, int c, int w, int h, int range, int offset, byte[] cache)
	{	
		int sum = 0;
		int total = 0;
		int xl = c - range >= 0 ? c - range : 0;
		int xr = c + range < w ? c + range : w - 1;
		int yl = r - range >= 0 ? r - range : 0;
		int yr = r + range < h ? r + range : h - 1;
		
		for(int y = yl; y <= yr; y++)
		{
			for(int x = xl; x <= xr; x++)
			{
				if(x == c && y == r)
				{
					sum += 3 * (int) (cache[x + y * w + offset] & 0xff);
					total += 3;
				}
				else
				{
					sum += (int) (cache[x + y * w + offset] & 0xff);
					total++;
				}
			}
		}
		
		return (byte) (sum / total);
	}
	
	public class GenStripsTask implements Runnable
	{
		public void run()
		{
			try
			{
				for (int i = 0; i < videoFileNames.length; i++)
				{
					String videoPath = folderPath + "\\" + videoFileNames[i];
					String outputStripPath = videoPath.substring(0, videoPath.length() - 4) + ".strip";
					infoLabel.setText("Generating Video Strip for: " + videoPath);
					FileOutputStream fos = new FileOutputStream(outputStripPath);
					loadFrames(videoPath);
					byte[] buffer;
					for (int j = 0; j < frameCount; j += interval)
					{
						buffer = resize(framesCache[j], miniWidth, miniHeight);
						displayFrame(framesCache[j], width, height);
						fos.write(buffer);
					}
					fos.close();
					infoLabel.setText("Video Strip All Generated");
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
	}
	
	public void generateStrips()
	{
		// need to create a new thread for time consuming task
		// because of Swing's single thread ui system
		Thread thr = new Thread(new GenStripsTask());
		thr.start();
	}
	
	private int toInt(byte b)
	{
		return 0x00000000 | (b & 0xff);
	}
	
	// search functions in videoplayer search, put here for test for now
	/*private ulong CalEuclidDistance(int[] c, int[] tc, int num)
    {
        ulong dis = 0;
        for (int i = 0; i < num; i++)
        {
        	dis += (ulong)((tc[i] - c[i]) * (tc[i] - c[i]));
        }
        return dis;
    }*/
	
	private int[][] loadColorIndex(String filePath)
	{
		Scanner s = null;
		try 
		{
			s = new Scanner(new BufferedReader(new FileReader(filePath)));
			int histoNum = s.nextInt();
			int[][] histos = new int[frameCount][3 * histoNum];
			for(int i = 0; i < frameCount; i++)
			{
				for(int j = 0; j < 3 * histoNum; j++)
				{
					histos[i][j] = s.nextInt();
				}
			}
			return histos;
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		finally
		{
			s.close();
		}
	}
	
	public void colorIndexing() throws IOException
	{
		final int colorIndexInterval = 1;
		final int histoNum = 10;
		float range = 1f / histoNum;
		
		for (int i = 0; i < videoFileNames.length; i++)
		{
			String videoPath = folderPath + "\\" + videoFileNames[i];
			String colorIndexPath = videoPath.substring(0, videoPath.length() - 4) + ".color";
			FileWriter fos = new FileWriter(colorIndexPath);
			fos.write(histoNum + " ");
			loadFrames(videoPath);
			for (int j = 0; j < frameCount; j += colorIndexInterval)
			{
				//fos.write("frame " + j + " ");
				int[][] histo = new int[3][histoNum];
				int index;
				for(int y = 0; y < height; y++)
				{			
					for(int x = 0; x < width; x++)
					{
						index = y * width + x;
						int r = toInt(framesCache[j][index]);
						int g = toInt(framesCache[j][index + height * width]);
						int b = toInt(framesCache[j][index + height * width * 2]); 
		
						float[] hsb = new float[3];
						Color.RGBtoHSB(r, g, b, hsb);	// h, s, b all between [0-1]
						for(int c = 0; c < 3; c++)
						{
							int area = (int) (hsb[c] / range);
							area = area == histoNum ? 9 : area;	// boundry condition, area should between [0-histoNum)
							//System.out.println(area);
							histo[c][area]++;
						}
					}
				}
				for(int m = 0; m < 3; m++)
				{
					for(int n = 0; n < histoNum; n++)
					{
						fos.write(histo[m][n] + " ");
					}
				}
			}
			fos.close();
		}
		infoLabel.setText("color indexing complete");
	}
	
	public void motionIndexing()
	{
		
	}
	
	public void soundIndexing()
	{
		
	}
	
	public void buttonPressed(String name)
	{
		if (name.equals("Generate Strips"))
		{
			generateStrips();
		}
		else if (name.equals("Color Indexing"))
		{
			try {
				colorIndexing();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (name.equals("Motion Indexing"))
		{
			motionIndexing();
		}
		else if (name.equals("Sound Indexing"))
		{
			soundIndexing();
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
