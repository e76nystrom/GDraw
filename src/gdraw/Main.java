/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

/**
 *
 * @author Eric Nystrom
 */
public class Main
{

 /**
  * @param args the command line arguments
  */
 public static void main(String[] args)
 {
  String inputFile = "";
  if (args.length >= 1)
  {
   inputFile = args[0];
  }

  double ySize = 0.0;
  boolean mirror = false;
  if (args.length >= 2)
  {
   mirror = true;
   ySize = Double.valueOf(args[1]);
  }

  if (inputFile.length() == 0)
  {
//   inputFile = "c:\\Development\\Circuits\\Strain Gage\\Strain_t.gbr";
//   inputFile = "c:\\Development\\Circuits\\Step Driver\\Step_b.gbr";
//   inputFile = "c:\\Development\\Circuits\\AD Converter\\Controller_b.gbr";
//   inputFile = "c:\\Development\\Circuits\\Test\\Test_t.gbr";
//   inputFile = "c:\\Development\\Circuits\\Accelerometer\\Accel_t.gbr";
//   inputFile = "c:\\Development\\Circuits\\Hall Effect\\Hall_t.gbr";
//   inputFile = "c:\\Development\\Circuits\\Amplifier\\Amp_b.gbr";
//   inputFile = "c:\\Development\\Circuits\\Motor Control\\Motor_b.gbr";
//   inputFile = "c:\\Development\\Circuits\\Power Connector\\Relay_b.gbr";
//   inputFile = "c:\\Development\\Circuits\\RLB\\RLB_t.gbr";
//   inputFile = "c:\\Development\\Circuits\\Gear Control 1\\BreakOut_b.gbr";
   inputFile = "c:\\Development\\Circuits\\Battery\\Reset\\Reset_b.gbr";
   if (false)
   {
    mirror = true;
    ySize = 1.689;
   }
/*
*/
  }

  if (inputFile.length() != 0)
  {
   System.out.printf("gdraw 02/26/2014\n");
   System.out.printf("Processing %s", inputFile);
   if (mirror)
   {
    System.out.printf(" Mirror %5.3f\n", ySize);
   }
   else
   {
    System.out.println();
   }
   GDraw gdraw = new GDraw(inputFile, mirror, ySize);
   gdraw.process();
  }
 }
}
