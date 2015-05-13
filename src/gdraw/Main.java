/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.Scanner;

/**
 *
 * @author Eric Nystrom
 */
public class Main
{

 /**
  * @param args the command line arguments
  */
 @SuppressWarnings({"DeadBranch", "UnusedAssignment"})
 public static void main(String[] args)
 {
  double xSize = 0.0;
  double ySize = 0.0;
  boolean mirror = false;
  boolean rotate = false;
  boolean debug = false;
  boolean dxf = false;
  boolean bmp = false;
  String inputFile = "";
  Pattern pOption = Pattern.compile("-[a-zD]");
  String line = "";

  if (args.length == 0)
  {
   File f = new File("gdraw.txt");
   if (f.isFile())
   {
    try (BufferedReader in = new BufferedReader(new FileReader(f)))
    {
     while ((line = in.readLine()) != null)
     {
      if (line.length() != 0)
      {
       line = line.replaceAll(" +"," ");
       if (!line.startsWith("/"))
       {
	break;
       }
      }
     }
     in.close();
    }
    catch (IOException e)
    {
     System.out.printf("command read error\n");
    }
   }
  }
  else
  {
   for (String arg : args)
   {
    line += arg + " ";
   }
  }

  Scanner sc = new Scanner(line);
  while (sc.hasNext())
  {
   if (sc.hasNext(pOption))
   {
    String option = sc.next(pOption);
    char c = option.charAt(1);
    if (c == 'r')
    {
     rotate = true;
    }
    else if (c == 'x')
    {
     mirror = true;
     if (sc.hasNextDouble())
     {
      xSize = sc.nextDouble();
     }
    }
    else if (c == 'y')
    {
     mirror = true;
     if (sc.hasNextDouble())
     {
      ySize = sc.nextDouble();
     }
    }
    else if (c == 'd')
    {
     debug = true;
    }
    else if (c == 'c')
    {
     dxf = true;
    }
    else if (c == 'b')
    {
     bmp = true;
    }
    else if (c == 'D')
    {
     debug = true;
     dxf = true;
     bmp = true;
    }
   }
   else if (sc.hasNext())
   {
    if (inputFile.length() != 0)
    {
     inputFile += " ";
    }
    inputFile += sc.next();
   }
  }

  if (inputFile.length() != 0)
  {
   System.out.printf("gdraw 05/12/2015\n");
   System.out.printf("Processing %s", inputFile);
   if (mirror)
   {
    System.out.printf(" Mirror x %5.3f y %5.3f\n",xSize,ySize);
   }
   else
   {
    System.out.println();
   }
   GDraw gdraw = new GDraw();
   gdraw.process(inputFile, rotate, mirror, xSize, ySize, debug, dxf, bmp);
  }
 }
}
