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

import java.text.SimpleDateFormat;

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
 Dxf d1 = null;
 boolean dbgFlag;
 String tracks = "tracks";
 String rapid = "rapid";
 String trackNum = "trackNum";
 String pads = "pads";
 String padNum = "padNum";
 String outline = "outline";
 boolean dxf = false;
 SimpleDateFormat sdf = new SimpleDateFormat("EEE LLL dd HH:mm:ss yyyy");

 public static final boolean CSV = false;
 public static final double BIT_RADIUS = .010;

 double depth;
 double retract;
 double linearFeed;
 double circularFeed;

 boolean variables;
 boolean metric;
 
 boolean probeFlag;
 String probeFile;
 Probe probe = new Probe();
 
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

 public void setVariables(boolean val)
 {
  this.variables = val;
 }

 public void setMetric(boolean val)
 {
  this.metric = val;
 }

 public void setProbe(boolean val, String probeFile)
 {
  this.probeFlag = val;
  this.probeFile = probeFile;
 }

 public void setDepth(double val)
 {
  this.depth = val;
 }

 public void setRetract(double val)
 {
  this.retract = val;
 }

 public void setLinear(double val)
 {
  this.linearFeed = val;
 }

 public void setCircular(double val)
 {
  this.circularFeed = val;
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
 @SuppressWarnings("CallToPrintStackTrace")
 public void process(String inputFile, boolean r, boolean m, double x, double y,
		     boolean debug, boolean dxfFlag, boolean bmp)
 {
  rotate = r;
  mirror = m;
  xSize = (int) (x * SCALE);
  ySize = (int) (y * SCALE);
  dxf = dxfFlag;
  dbgFlag = debug;
  apertureList = new ApertureList(this);
  padList = new PadList(this);
  trackList = new TrackList(this);

  openFiles(inputFile, debug);

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
    padList.mirror(xSize, ySize);
    trackList.mirror(xSize, ySize);
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
      d.circle(tx, ty, w);
     }
     else if (pad.ap.type == ApertureList.Aperture.SQUARE)
     {
      double h = pad.ap.val2 / 2.0;
      d.rectangle(tx - w, ty - h, tx + w, ty + h);
     }
     d.setLayer(padNum);
     d.text(tx + w + .003, ty + .005, 0.010, String.format("%d", pad.index));
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
     d.line(tx0, ty0, tx1, ty1);
     d.setLayer(trackNum);
     d.text(tx0 + (tx1 - tx0) / 2 + .005, ty0 + (ty1 - ty0) / 2 + .005, 0.010,
	    String.format("%d", track.index));
    }
   }
   trackList.print();

   xMax = (int) (xMax / scale);
   yMax = (int) (yMax / scale);
//   xMax += 2;
//   yMax += 1;
   System.out.printf("x max %4d y max %4d\n", xMax, yMax);
   image = new Image(this, xMax, yMax, scale);
   image.setVariables(variables);
   image.setMetric(metric);
   image.setDepth(depth);
   image.setRetract(retract);
   image.setLinear(linearFeed);
   image.setCircular(circularFeed);
   image.setProbe(probeFlag, probe);
   image.setDxf(dxf, d1);
   image.setMirror(mirror, xSize, ySize);
   probe.setDebug(dbgFlag, dbg);

   image.getData();
   try
   {
    padList.draw(image);
   }
   catch (Exception e)
   {
    System.err.printf("failure %s\n", e.toString());
    image.setData();
    image.write(image.data, baseFile + "00");
    closeFiles();
    return;
   }
   image.setData();

   image.drawTracks();

   image.getData();
   boolean overlap = image.adjacentPads(false);
   image.getData();
   overlap |= image.adjacentPads(true);
   if (overlap)
   {
    System.exit(1);
   }
   image.getData();
   image.padTrack();
   image.adjacentFix();

//   image.getData();
//   padList.check(image);

   try
   {
    image.process(bmp);
   }
   catch (Exception e)
   {
    System.err.printf("failure %s\n", e.toString());
    e.printStackTrace();
    image.setData();
    image.write(image.data, baseFile + "00");
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
       if (x > 100000)
       {
	x /= 100;
       }
       if (x > xMax)
       {
	xMax = x;
       }
      }
      if (yStr.length() != 0)
      {
       int y = Integer.parseInt(yStr);
       if (y > 100000)
       {
	y /= 100;
       }
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
   System.out.printf("Size x %5.3f y %5.3f\n", xMax / SCALE, yMax / SCALE);
  }
 }

 /**
  * Opens input an output files
  * 
  * @param inputFile the input file name
  * @param debug
  */
 public void openFiles(String inputFile, boolean debug)
 {
  if (!inputFile.contains("."))
  {
   inputFile += ".gbr";
  }

  String boardFile = inputFile.replaceAll("_[bt]", "");
  boardSize(boardFile);

  baseFile = inputFile.split("\\.")[0];

  if (probeFlag)
  {
   if (probeFile.length() == 0)
   {
    probeFile = inputFile.replaceFirst("\\.gbr$", ".prb");
   }
   boolean flag = probe.readFile(probeFile);
   if (!flag)
   {
    System.err.printf("disabling probe file\n");
    probeFlag = false;
   }
  }

  if (dxf)
  {
   d = new Dxf();
   if (d.init(baseFile + ".dxf"))
   {
    d.layer(tracks, Dxf.RED);
    d.layer(trackNum, Dxf.RED);
    d.layer(pads, Dxf.BLUE);
    d.layer(padNum, Dxf.BLUE);
    d.layer(outline, Dxf.BLUE);
    d.setLayer(outline);
    d.rectangle(0.0, 0.0, xMax / SCALE, yMax / SCALE);

    d1 = new Dxf();
    if (d1.init(baseFile + "1.dxf"))
    {
     d1.layer(tracks, Dxf.RED);
     d1.layer(trackNum, Dxf.RED);
     d1.layer(pads, Dxf.BLUE);
     d1.layer(padNum, Dxf.BLUE);
     d1.layer(rapid, Dxf.GREEN);
     d1.rectangle(0.0, 0.0, xMax / SCALE, yMax / SCALE);
    }
    else
    {
     dxf = false;
    }
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
     dbg.printf("(%s %s)\n", fIn.getAbsolutePath(), sdf.format(new Date()));
     dbgFlag = true;
     padList.setDebug(dbg);
     apertureList.setDebug(dbg);
     trackList.setDebug(dbg);
    }
    else
    {
     fDbg.delete();
     String[] ext = {"0", "1", "00"};
     for (String e : ext)
     {
      File f = new File(baseFile + e + ".png");
      if (f.exists())
      {
       f.delete();
      }       
     }
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
   d1.end();
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
  out.printf("(%s %s)\n", fIn.getAbsolutePath(), sdf.format(new Date()));
  double mScale = 1.0;
  if (!metric)
  {
   out.printf("g20       (inch units)\n");
  }
  else
  {
   out.printf("g21       (metric units)\n");
   mScale = 25.4;
  }
  out.printf("g61       (exact path mode)\n");
  if (this.variables)
  {
   out.printf("#1 = %7.4f (depth)\n", this.depth);
   out.printf("#2 = %7.4f (retract)\n", this.retract);
   if (mirror)
   {
    out.printf("#30 = %5.3f  (offset)\n", xOffset * mScale);
    out.printf("#31 = %5.3f  (offset)\n", yOffset * mScale);
    out.printf("#4 = -1      (mirror)\n");
   }
   else
   {
    out.printf("#4 = 1       (mirror)\n");
   }
   out.printf("#5 = %4.1f    (linear feed rate)\n", this.linearFeed);
   out.printf("#6 = %4.1f    (circle feed rate)\n", this.circularFeed);
  }
  else
  {
   out.printf("\n");
   out.printf("(depth   = %7.4f)\n", this.depth);
   out.printf("(retract = %7.4f)\n", this.retract);
   if (mirror)
   {
    out.printf("(xOffset = %5.3f)\n",xOffset * mScale);
    out.printf("(yOffset = %5.3f)\n",yOffset * mScale);
    out.printf("(mirror = -1)\n");
   }
   else
   {
    out.printf("(mirror = 1)\n");
   }
   out.printf("(linearFeed   = %4.1f)\n", this.linearFeed);
   out.printf("(circularFeed = %4.1f)\n", this.circularFeed);
  }
  out.printf("\n");

  if (probeFlag)
  {
   File f = new File(probeFile);
   out.printf("(%s %s)\n", f.getAbsolutePath(), sdf.format(f.lastModified()));
   double[][] zMatrix = probe.zMatrix;
   int len = zMatrix[0].length;
   out.printf("(         ");
   for (int i = 0; i < zMatrix.length; i++)
   {
    out.printf("   %2d   ", i);
   }
   out.printf(")\n");
   out.printf("(         ");
   for (int i = 0; i < zMatrix.length; i++)
   {
    out.printf(" %6.3f ", probe.margin + probe.xStep * i);
   }
   out.printf(")\n");
   for (int j = len - 1; j >= 0; j--)
   {
    out.printf("(%2d %6.3f", j, probe.margin + probe.yStep * j);
    for (int i = 0; i < zMatrix.length; i++)
    {
     out.printf(" %7.4f", zMatrix[i][j]);
    }
    out.printf(")\n");
   }
   out.printf("\n");
  }
  
  out.printf("g0 z%5.3f        (move tool above clamps)\n", 0.500 * mScale);
  out.printf("g0 x%5.3f y%5.3f (move tool away from clamps)\n", 
	     0.250 * mScale,  0.250 * mScale);
  out.printf("\n");
  if (probeFlag)
  {
   out.printf("g0 x%6.4f y%6.4f (start position)\n",
	      probe.margin * mScale, probe.margin * mScale);
   out.printf("m0	(pause to check position)\n");
   out.printf("g0 z%6.4f\n", probe.retract * mScale);
   out.printf("g38.2 z%6.4f f%3.1f\n", probe.depth * mScale, 1.0 * mScale);
   out.printf("g10 L20 P0 z0.000 (zero z)\n");
   out.printf("g0 z%6.4f\n\n", probe.retract * mScale);
  }
  out.printf("s25000    (set spindle speed)\n");
  out.printf("m3        (start spindle)\n");
  out.printf("g4 p2.0   (wait for spindle to come to speed)\n");
  if (this.variables)
  {
   out.printf("f[#5]     (set feed rate)\n");
  }
  else
  {
   out.printf("f%4.1f    (set feed rate)\n", this.linearFeed);
  }
  out.printf("\n");
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
     switch (c)
     {
      case 'C':
       double size = in.getFVal();
       apertureList.add(apertureNo, size);
       break;
      case 'R':
       {
        double size0 = in.getFVal();
        double size1 = in.getFVal();
        apertureList.add(apertureNo, size0, size1);
        break;
       }
      default:
       {
        System.out.printf("aperture %d not rectangular or curcular\n",
			  apertureNo);
        double size0 = in.getFVal();
        double size1 = in.getFVal();
        apertureList.add(apertureNo, size0, size1);
        break;
       }
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
//      if (xVal > 100000)
      {
       xVal /= 100;
      }
      xVal = ((xVal + 5) / 10) * 10;
     }
     else if (in.check('Y'))
     {
      yVal = in.getVal();
//      if (yVal > 100000)
      {
       yVal /= 100;
      }
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
        System.out.printf("null aperture %d\n", dCode);
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
      System.out.printf("negatve value %f\n", tmp);
     }
     return (tmp);
    }
    c = b[pos++];
   }
   return (0.0);
  }
 }
}
