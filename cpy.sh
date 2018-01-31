#!/bin/bash

cd /cygdrive/c/Development/java/GDraw/dist
chmod -x *.jar
chmod +rw *.jar
scp GDraw.jar eric@10.0.0.10:java
cd /cygdrive/c/Development/java/Drill/dist
chmod -x *.jar
chmod +rw *.jar
scp Drill.jar eric@10.0.0.10:java
cd /cygdrive/c/Development/java/CNC/dist
chmod -x *.jar
chmod +rw *.jar
scp CNC.jar eric@10.0.0.10:java/lib
