/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import gdraw.Util.Pt;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 File fIn;
 PrintWriter out;
 PrintWriter dbg = null;
 PrintWriter csv;
 String baseFile;
 ApertureList apertureList;
 TrackList trackList;
 PadList padList;
 Image image;
 boolean mirror = false;
 int ySize = 0;
 int xMax = 0;
 int yMax = 0;
 float scale = 10.0f;

 public static final boolean CSV = false;
 public static final boolean DBG = false;

 /**
  * Scale factor for converting interger input to correct position
  */
 public static final double SCALE = 10000.0;

 /**
  * Opens the input file, output files, and creates all the classes to process
  * a gerber file.
  *  
  * @param inputFile
  * @param m
  * @param y
  */
 public GDraw(String inputFile, boolean m, double y)
 {
  mirror = m;
  ySize = (int) (y * SCALE);
  openFiles(inputFile);
  apertureList = new ApertureList(this);
  trackList = new TrackList(this);
  padList = new PadList(this);
 }

 /**
  * Enables creating a mirror image of the output file.
  * 
  * @param y the size to use for creating the mirror image
  */
 public void setMirror(double y)
 {
  mirror = true;
  ySize = (int) (y * SCALE);
 }

 /**
  * Enables creating a mirror image of the output file.
  * 
  * @param fileName name of board outline file
  */
 private void boardSize(String fileName)
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
  * Process a gerber file
  */
 public void process()
 {
  if (fIn.isFile())
  {
   readInput();
   apertureList.addBitRadius(0.010);

   apertureList.print();
   padList.print();
   trackList.print();

   xMax = (int) (xMax / scale);
   yMax = (int) (yMax / scale);
//   xMax += 2;
//   yMax += 1;
//   System.out.printf("x max %4d y max %4d\n",xMax,yMax);
   image = new Image(this,xMax,yMax,scale);

   image.getData();
   padList.draw(image);
   image.setData();

//   trackList.draw(image);
//   image.testDraw();

   image.drawTracks();

//   image.adjacentPads();

//   image.getData();
//   padList.check(image);

    image.process();

   closeFiles();
  }
 }

 /**
  * Opens input an output files
  * 
  * @param inputFile the input file name
  */
 private void openFiles(String inputFile)
 {
  if (inputFile.indexOf(".") < 0)
  {
   inputFile += ".gbr";
  }

  String boardFile = inputFile.replaceAll("_[bt]","");
  boardSize(boardFile);

  baseFile = inputFile.split("\\.")[0];

  fIn = new File(inputFile);
  if (fIn.isFile())
  {
   String outputFile = baseFile + ".ngc";
   File fOut = new File(outputFile);

   try
   {
    out = new PrintWriter(new BufferedWriter(new FileWriter(fOut)));
    ncHeader(mirror, ((double) ySize) / SCALE);

    String dbgFile = baseFile + ".dbg";
    File fDbg = new File(dbgFile);
    if (DBG)
    {
     dbg = new PrintWriter(new BufferedWriter(new FileWriter(fDbg)));
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

  if (DBG)
  {
   dbg.close();
  }
  if (CSV)
  {
   csv.close();
  }
 }

 /**
  * Creates header information for nc output file
  * 
  * @param mirror flag to indicate if y axis shold be mirrored
  * @param offset offset to use to mirror y axis if mirror flag is true
  */
 public void ncHeader(boolean mirror, double offset)
 {
  out.printf("(%s %s)\n",fIn.getAbsolutePath(),(new Date()).toString());
  out.printf("#1 = -0.009 (depth)\n");
  out.printf("#2 = 0.020  (retract)\n");
  if (mirror)
  {
   out.printf("#3 = %5.3f  (offset)\n",offset);
   out.printf("#4 = -1     (mirror)\n");
  }
  else
  {
   out.printf("#4 = 1      (mirror)\n");
  }
  out.printf("#5 = 7.0     (linear feed rate)\n");
  out.printf("#6 = 3.0     (circle feed rate)\n");

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
   if (DBG)
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
      if (mirror)
      {
       yVal = ySize - yVal;
      }
     }
     else if (in.check('D'))
     {
      dCode = in.getVal();
      if (dCode == 1)
      {
       trackList.add(new Pt(lastX, lastY),
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
      else if (dCode == 2)
      {
       lastX = xVal;
       lastY = yVal;
      }
      else if (dCode == 3)
      {
       padList.add(new Pt(xVal, yVal), currentAperture);
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
     return (Double.valueOf(s));
    }
    c = b[pos++];
   }
   return (0.0);
  }
 }
}
