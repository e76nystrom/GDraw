/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import gdraw.Util.Pt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Dxf.Dxf;

/*
 * GDraw.java
 *
 * Created on Apr 8, 2008 at 7:04:23 AM
 *
 * @author Eric Nystrom
 *
 */
public class GDraw
{
 boolean rotate = false;
 boolean mirror = false;
 int ySize = 0;
 int xSize = 0;
 File fIn;
 PrintWriter out;
 PrintWriter dbg = null;
 PrintWriter csv;
 String baseFile;
 ApertureList apertureList;
 TrackList trackList;
 PadList padList;
 Image image;
 int xMax = 0;
 int yMax = 0;
 float scale = 10.0f;
 Dxf d = null;
 boolean dbgFlag;
 String tracks = "tracks";
 String trackNum = "trackNum";
 String pads = "pads";
 String padNum = "padNum";
 boolean dxf = false;

 public static final boolean CSV = false;
 public static final double BIT_RADIUS = .010;

 /**
  * Scale factor for converting interger input to correct position
  */
 public static final double SCALE = 10000.0;

 /**
  * Empty constructor
  *  
  */
 public GDraw()
 {
 }

 /**
  * Process a gerber file
  *
  * @param inputFile
  * @param r
  * @param m
  * @param x
  * @param y
  * @param debug
  * @param dxfFlag
  * @param bmp
  */
 public void process(String inputFile, boolean r, boolean m, double x, double y,
		     boolean debug, boolean dxfFlag, boolean bmp)
 {
  rotate = r;
  mirror = m;
  xSize = (int) (x * SCALE);
  ySize = (int) (y * SCALE);
  dxf = dxfFlag;
  openFiles(inputFile,debug);
  apertureList = new ApertureList(this);
  trackList = new TrackList(this);
  padList = new PadList(this);

  if (fIn.isFile())
  {
   readInput();

   if (rotate)
   {
    if (dbgFlag)
    {
     dbg.printf("\nrotate\n");
    }
    int tmp;
    tmp = xMax;
    xMax = yMax;
    yMax = tmp;
    apertureList.rotate();
    padList.rotate(xMax);
    trackList.rotate(xMax);
   }

   if (mirror)
   {
    if (dbgFlag)
    {
     dbg.printf("\nmirror\n\n");
    }
    padList.mirror(xSize,ySize);
    trackList.mirror(xSize,ySize);
   }

   apertureList.addBitRadius(BIT_RADIUS);

   apertureList.print();

   Collections.sort(padList);
   int i = 0;
   for (PadList.Pad pad : padList)
   {
    pad.gIndex = pad.index;
    pad.index = i++;
    if (dxf)
    {
     double tx = pad.pt.x / SCALE;
     double ty = pad.pt.y / SCALE;
     double w = pad.ap.val1 / 2.0;
     d.setLayer(pads);
     if (pad.ap.type == ApertureList.Aperture.ROUND)
     {
      d.circle(tx,ty,w);
     }
     else if (pad.ap.type == ApertureList.Aperture.SQUARE)
     {
      double h = pad.ap.val2 / 2.0;
      d.rectangle(tx - w,ty - h,tx + w,ty + h);
     }
     d.setLayer(padNum);
     d.text(tx + w + .003,ty + .005,.010,String.format("%d",pad.index));
    }
   }
   padList.print();

   Collections.sort(trackList);
   i = 0;
   for (TrackList.Track track : trackList)
   {
    track.gIndex = track.index;
    track.index = i++;
    if (dxf)
    {
     double tx0 = track.pt[0].x / SCALE;
     double ty0 = track.pt[0].y / SCALE;
     double tx1 = track.pt[1].x / SCALE;
     double ty1 = track.pt[1].y / SCALE;
     d.setLayer(tracks);
     d.line(tx0,ty0,tx1,ty1);
     d.setLayer(trackNum);
     d.text(tx0 + (tx1 - tx0) / 2 + .005,ty0 + (ty1 - ty0) / 2 + .005,.010,
	    String.format("%d",track.index));
    }
   }
   trackList.print();

   xMax = (int) (xMax / scale);
   yMax = (int) (yMax / scale);
//   xMax += 2;
//   yMax += 1;
   System.out.printf("x max %4d y max %4d\n",xMax,yMax);
   image = new Image(this,xMax,yMax,scale);

   image.getData();
   try
   {
    padList.draw(image);
   }
   catch (Exception e)
   {
    System.err.printf("failure\n");
    image.setData();
    image.write(image.data,baseFile + "00");
    closeFiles();
    return;
   }
   image.setData();

   image.drawTracks();

   image.getData();
   image.adjacentPads(false);
   image.getData();
   image.adjacentPads(true);
   image.adjacentFix();
   image.padTrack();

//   image.getData();
//   padList.check(image);

   try
   {
    image.process(bmp);
   }
   catch (Exception e)
   {
    System.err.printf("failure\n");
    image.setData();
    image.write(image.data,baseFile + "00");
    closeFiles();
    return;
   }
   closeFiles();
  }
 }

 /**
  * Enables creating a mirror image of the output file.
  * 
  * @param fileName name of board outline file
  */
 public void boardSize(String fileName)
 {
  Pattern p = Pattern.compile("[Xx]?([0-9]*)[Yy]?([0-9]*)");

  File f = new File(fileName);
  if (f.isFile())
  {
   try (BufferedReader in = new BufferedReader(new FileReader(f)))
   {
    String line;
    while ((line = in.readLine()) != null)
    {
     if (line.startsWith("%"))
     {
      continue;
     }
     Matcher m = p.matcher(line);
     if (m.find())
     {
      String xStr = m.group(1);
      String yStr = m.group(2);
      if (xStr.length() != 0)
      {
       int x = Integer.parseInt(xStr);
       if (x > xMax)
       {
	xMax = x;
       }
      }
      if (yStr.length() != 0)
      {
       int y = Integer.parseInt(yStr);
       if (y > yMax)
       {
	yMax = y;
       }
      }
     }
    }
   }
   catch (IOException e)
   {
    System.out.println(e);
   }
   System.out.printf("Size x %5.3f y %5.3f\n",xMax / SCALE,yMax / SCALE);
  }
 }

 /**
  * Opens input an output files
  * 
  * @param inputFile the input file name
  */
 public void openFiles(String inputFile, boolean debug)
 {
  if (!inputFile.contains("."))
  {
   inputFile += ".gbr";
  }

  String boardFile = inputFile.replaceAll("_[bt]","");
  boardSize(boardFile);

  baseFile = inputFile.split("\\.")[0];

  if (dxf)
  {
   d = new Dxf();
   if (d.init(baseFile + ".dxf"))
   {
    d.layer(tracks,Dxf.RED);
    d.layer(trackNum,Dxf.RED);
    d.layer(pads,Dxf.BLUE);
    d.layer(padNum,Dxf.BLUE);
   }
   else
   {
    dxf = false;
   }
  }

  fIn = new File(inputFile);
  if (fIn.isFile())
  {
   String outputFile = baseFile + ".ngc";
   File fOut = new File(outputFile);

   try
   {
    out = new PrintWriter(new BufferedWriter(new FileWriter(fOut)));
    ncHeader(mirror, ((double) xSize) / SCALE, ((double) ySize) / SCALE);

    String dbgFile = baseFile + ".dbg";
    File fDbg = new File(dbgFile);
    if (debug)
    {
     dbg = new PrintWriter(new BufferedWriter(new FileWriter(fDbg)));
     dbgFlag = true;
    }
    else
    {
     fDbg.delete();
    }

    if (CSV)
    {
     String csvFile = baseFile + ".csv";
     File fCsv = new File(csvFile);
     csv = new PrintWriter(new BufferedWriter(new FileWriter(fCsv)));
    }
   }
   catch (IOException err)
   {
    System.out.println("file open error " + err.getMessage());
   }
  }
  else
  {
   System.out.printf("Input file %s not found\n", inputFile);
  }
 }

 /**
  * Write trailer information to output files and close them all.
  */
 public void closeFiles()
 {
  out.printf("m5\n");
  out.printf("g0 z1.5\n");
  out.printf("g0 x0.0 y0.0\n");
  out.printf("m2\n");
  out.close();

  if (dbgFlag)
  {
   dbg.close();
  }
  if (CSV)
  {
   csv.close();
  }
  if (dxf)
  {
   d.end();
  }
 }

 /**
  * Creates header information for nc output file
  * 
  * @param mirror flag to indicate if y axis should be mirrored
  * @param xOffset offset to use to mirror y axis if mirror flag is true
  * @param yOffset offset to use to mirror x axis if mirror flag is true
  */
 public void ncHeader(boolean mirror, double xOffset, double yOffset)
 {
  out.printf("(%s %s)\n",fIn.getAbsolutePath(),(new Date()).toString());
  out.printf("#1 = -0.009 (depth)\n");
  out.printf("#2 = 0.020  (retract)\n");
  if (mirror)
  {
   out.printf("#30 = %5.3f  (offset)\n",xOffset);
   out.printf("#31 = %5.3f  (offset)\n",yOffset);
   out.printf("#4 = -1     (mirror)\n");
  }
  else
  {
   out.printf("#4 = 1      (mirror)\n");
  }
  out.printf("#5 = 14.0     (linear feed rate)\n");
  out.printf("#6 = 14.0     (circle feed rate)\n");

  out.printf("g0 z0.500        (move tool above clamps)\n");
  out.printf("g0 x0.250 y0.250 (move tool away from clamps)\n");
  out.printf("f[#5]     (set feed rate)\n");
  out.printf("g61       (exact path mode)\n");
  out.printf("s25000    (set spindle speed)\n");
  out.printf("m3        (start spindle)\n");
  out.printf("g4 p1.0   (wait for spindle to come to speed)\n");
 }

 /**
  * Read input file
  */
 public void readInput()
 {
  int gCode;
  int mCode;
  int dCode;
  int xVal = 0;
  int yVal = 0;
  int lastX = 0;
  int lastY = 0;
  ApertureList.Aperture currentAperture = null;
  int apW = 0;
  int apH = 0;

  InputBuf in = new InputBuf(fIn);
  while (in.read())
  {
   if (dbgFlag)
   {
    dbg.printf("%s\n", in.line);
    dbg.flush();
   }
   if (in.check('%'))
   {
    if (in.check("ADD"))
    {
     int apertureNo = in.getVal();
     char c = in.get();
     in.skip();
     if (c == 'C')
     {
      double size = in.getFVal();
      apertureList.add(apertureNo, size);
     }
     else if (c == 'R')
     {
      double size0 = in.getFVal();
      double size1 = in.getFVal();
      apertureList.add(apertureNo, size0, size1);
     }
     else
     {
      System.out.printf("aperture %d not rectangular or curcular\n",
			apertureNo);
      double size0 = in.getFVal();
      double size1 = in.getFVal();
      apertureList.add(apertureNo, size0, size1);
     }
    }
   }
   else
   {
    do
    {
     if (in.check('G'))
     {
      gCode = in.getVal();
     }
     else if (in.check('X'))
     {
      xVal = in.getVal();
      xVal = ((xVal + 5) / 10) * 10;
     }
     else if (in.check('Y'))
     {
      yVal = in.getVal();
      yVal = ((yVal + 5) / 10) * 10;
//      if (mirror)
//      {
//       yVal = ySize - yVal;
//      }
     }
     else if (in.check('D'))
     {
      dCode = in.getVal();
      if (dCode == 1)
      {
       if (currentAperture != null)
       {
	TrackList.Track trk = trackList.add(new Pt(lastX, lastY),
					    new Pt(xVal, yVal), currentAperture);
	lastX = xVal;
	lastY = yVal;
	int x = xVal + apW;
	if (x > xMax)
	{
	 xMax = x;
	}
	int y = yVal + apH;
	if (y > yMax)
	{
	 yMax = y;
	}
       }
      }
      else if (dCode == 2)
      {
       lastX = xVal;
       lastY = yVal;
      }
      else if (dCode == 3)
      {
       if ((xVal < 0)
       ||  (yVal < 0))
       {
	System.out.printf("negative\n");
       }
       if (currentAperture != null)
       {
	PadList.Pad pad = padList.add(new Pt(xVal, yVal), currentAperture);
	int x = xVal + apW;
	if (x > xMax)
	{
	 xMax = x;
	}
	int y = yVal + apH;
	if (y > yMax)
	{
	 yMax = y;
	}
       }
      }
      else if (dCode >= 10)
      {
       currentAperture = apertureList.get(dCode);
       if (currentAperture == null)
       {
        System.out.printf("null aperture %d\n",dCode);
       }
       else if (currentAperture.type == ApertureList.Aperture.ROUND)
       {
	apW = ((int) (currentAperture.val1 * GDraw.SCALE));
	apH = apW;
       }
       else if (currentAperture.type == ApertureList.Aperture.SQUARE)
       {
	apW = ((int) (currentAperture.val1 * GDraw.SCALE));
	apH = ((int) (currentAperture.val2 * GDraw.SCALE));
       }
      }
     }
     else if (in.check('M'))
     {
      mCode = in.getVal();
     }
     else
     {
      in.skip();
     }
    } while (!in.done());
   }
  }
  in.close();
 }

 /**
  * Class for processing input buffer
  */
 public class InputBuf
 {

  BufferedReader in;
  public String line;
  byte[] b;
  int length;
  int pos;
  byte c;

  /**
   * Open input buffer for a file
   * 
   * @param f is the file to open the input buffer for
   */
  public InputBuf(File f)
  {
   try
   {
    in = new BufferedReader(new FileReader(f));
   }
   catch (FileNotFoundException e)
   {
   }
  }

  /**
   * Close input file
   */
  public void close()
  {
   try
   {
    in.close();
   }
   catch (IOException e)
   {
   }
  }

  /**
   * Read a line from the open input file.
   * 
   * @return true if a line read, false if an input error
   */
  public boolean read()
  {
   try
   {
    if ((line = in.readLine()) != null)
    {
     pos = 0;
     b = line.getBytes();
     length = b.length;
     c = b[pos++];
     return (true);
    }
    return (false);
   }
   catch (IOException e)
   {
    return (false);
   }
  }

  /**
   * Check if pointer is at end of buffer
   * 
   * @return true if at end of buffer
   */
  public boolean done()
  {
   return (pos >= length);
  }

  /**
   * Check if next character in the input buffer matches the input parameter.
   * 
   * @param ch character to check for
   * @return true if character matches
   */
  public boolean check(char ch)
  {
   if (ch == c)
   {
    c = b[pos++];
    return (true);
   }
   return (false);
  }

  /**
   * Skip next character in the input buffer
   */
  public void skip()
  {
   c = b[pos++];
  }

  /**
   * Get the next character from input buffer
   * 
   * @return character read
   */
  public char get()
  {
   char tmp = (char) c;
   c = b[pos++];
   return (tmp);
  }

  /**
   * Check if a string is at current input buffer position.  If string found
   * advance buffer position past string.
   * 
   * @param s string to check
   * @return true if string at current buffer position
   */
  public boolean check(String s)
  {
   byte[] b0 = s.getBytes();
   int tmp = pos - 1;
   for (int i = 0; i < b0.length; i++)
   {
    if (b[tmp++] != b0[i])
    {
     return (false);
    }
   }
   pos = tmp;
   c = b[pos++];
   return (true);
  }

  /**
   * Read an integer value from input buffer
   * 
   * @return integer value
   */
  public int getVal()
  {
   int val;

   val = 0;
   while (pos < length)
   {
    if ((c < '0') || (c > '9'))
    {
     break;
    }
    val *= 10;
    val += c - '0';
    c = b[pos++];
   }
   if (val < 0)
   {
    System.out.printf("negative\n");
   }
   return (val);
  }

  /**
   * Read floating value from input buffer
   * 
   * @return return floating value
   */
  public double getFVal()
  {
   String s = "";

   while (pos < length)
   {
    if ((c >= '0') && (c <= '9'))
    {
     break;
    }
    c = b[pos++];
   }

   while (pos < length)
   {
    if ((c >= '0') && (c <= '9'))
    {
     s += (char) c;
    } else if (c == '.')
    {
     s += (char) c;
    } else
    {
     double tmp = Double.valueOf(s);
     if (tmp < 0.0)
     {
      System.out.printf("negatve value %f\n",tmp);
     }
     return (tmp);
    }
    c = b[pos++];
   }
   return (0.0);
  }
 }
}
