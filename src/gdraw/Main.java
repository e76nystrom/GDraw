/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.regex.MatchResult;
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
  boolean variables = false;
  boolean metric = false;
  boolean metricInput = false;
  boolean probe = false;
  String probeFile = "";
  double xSize = 0.0;
  double ySize = 0.0;
  boolean mirror = false;
  boolean rotate = false;
  boolean debug = false;
  boolean dxf = false;
  boolean bmp = false;
  boolean cutLines = true;
  String inputFile = "";
  Pattern pOption = Pattern.compile("-([a-zA-Z])=?([01]?)");
  Pattern p1Option = Pattern.compile("--([a-zA-Z]*)=?(\\S*)");
  String line = "";
  
  double depth = -0.006;
  double retract = 0.020;
  double linearFeed = 14.0;
  double circularFeed = 14.0;

  if (args.length == 0)
  {
   File f = new File("gdraw.txt");
   if (f.isFile())
   {
    try (BufferedReader in = new BufferedReader(new FileReader(f)))
    {
     while ((line = in.readLine()) != null)
     {
      line = line.trim();
      if (line.length() != 0)
      {
       line = line.replaceAll(" +", " ");
       if (!line.startsWith("/"))
       {
	System.out.println(line);
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
    MatchResult m = sc.match();
    int count = m.groupCount();
    if (count <= 2)
    {
     char c = m.group(1).charAt(0);
     // System.out.printf("%c\n", c);
     switch (c)
     {
     case 'r':			/* rotate */
      rotate = true;
      break;
     case 'x':			/* mirror x */
      mirror = true;
      if (sc.hasNextDouble())
      {
       xSize = sc.nextDouble();
      }
      break;
     case 'y':			/* mirror y */
      mirror = true;
      if (sc.hasNextDouble())
      {
       ySize = sc.nextDouble();
      }
      break;
     case 'd':			/* debug */
      debug = true;
      break;
     case 'c':			/* generate dxf file */
      dxf = true;
      break;
     case 'b':			/* generate extra png files */
      bmp = true;
      break;
     case 'D':			/* debug, dxf and png files */
      debug = true;
      dxf = true;
      bmp = true;
      break;
     case 'v':			/* use variables in output */
      variables = true;
      break;
     case 'm':
      metric = true;		/* metric */
      break;
     case 'p':			/* read probe data */
      probe = true;
     case 'M':			/* metric input */
      metricInput = true;
     case 't':			/* add cutlines around pads */
      String val = m.group(2);
      if (val.length() == 0)
      {
       cutLines = true;
      }
      else
      {
       if (val.equals("0"))
       {
	cutLines = false;
       }
       else if (val.equals("1"))
       {
	cutLines = true;
       }
      }
     default:
      break;
     }
    }
   }
   else if (sc.hasNext(p1Option))
   {
    String option = sc.next(p1Option);
    MatchResult m = sc.match();
    option = m.group(1);
    String tmp = m.group(2);
    if (option.equals("probe"))	/* probe with parameters */
    {
     probe = true;
     if (tmp.length() > 0)
     {
      probeFile = tmp;
     }
    }
    else
    {
     try
     {
      if (tmp.length() > 0)
      {
       double val = Double.valueOf(tmp);
       switch (option)
       {
       case "depth":		/* milling depth */
	depth = val;
	break;
       case "retract":		/* retract during milling */
	retract = val;
	break;
       case "linear":		/* linear feed rate */
	linearFeed = val;
	break;
       case "circular":		/* circular feed rate */
	circularFeed = val;
	break;
       default:
	break;
       }
      }
     }
     catch (NumberFormatException e)
     {
     }
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

  if (metric)
  {
   depth *= 25.4;
   retract *= 25.4;
   linearFeed *= 25.4;
   circularFeed *= 25.4;
  }

  if (inputFile.length() != 0)
  {
   System.out.printf("gdraw 03/18/2017\n");
   System.out.printf("Processing %s", inputFile);
   if (mirror)
   {
    System.out.printf(" Mirror x %5.3f y %5.3f\n", xSize, ySize);
   }
   else
   {
    System.out.println();
   }
   
   GDraw gdraw = new GDraw();
   gdraw.setVariables(variables);
   gdraw.setMetric(metric);
   gdraw.setMetricInput(metricInput);
   gdraw.setCutLines(cutLines);
   gdraw.setProbe(probe, probeFile);
   gdraw.setDepth(depth);
   gdraw.setRetract(retract);
   gdraw.setLinear(linearFeed);
   gdraw.setCircular(circularFeed);
   gdraw.process(inputFile, rotate, mirror, xSize, ySize, debug, dxf, bmp);
  }
 }
}
