/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gdraw;

import java.io.PrintWriter;

import java.util.ArrayList;

import gdraw.Util.Pt;

/**
 *
 * @author Eric
 */
public class PolygonList extends ArrayList<PolygonList.Polygon>
{
 GDraw gerber;
 
 public PolygonList(GDraw g)
 {
  gerber = g;
 }

 public PolygonList.Polygon newPolygon()
 {
  PolygonList.Polygon polygon = new PolygonList.Polygon();
  add(polygon);
  return(polygon);
 }

 public class Polygon extends ArrayList<Pt>
 {

  public Polygon()
  {
  }
 }
}
