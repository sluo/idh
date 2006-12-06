import sys
from java.lang import *
from java.nio import *
from javax.swing import *

from edu.mines.jtk.awt import *
from edu.mines.jtk.dsp import *
from edu.mines.jtk.io import *
from edu.mines.jtk.mosaic import *
from edu.mines.jtk.util import *

from lcc import *

#############################################################################
# parameters

fontSize = 24
width = 640
height = 505
widthColorBar = 80
dataDir = "/data"
pngDir = None

n1 = 315
n2 = 315
order = 1
sigma = 8

#############################################################################
# functions

def main(args):
  goBurg()
  return

def goBurg():
  x = doImage()
  doBurg(x,order,sigma)

def doImage():
  x = readImage()
  #x = Array.transpose(x)
  plot(x,10.0,"x")
  return x

def doBurg(x,order,sigma):
  lbf = LocalBurgFilter(sigma)
  c1 = Array.zerofloat(n1,n2,order)
  c2 = Array.zerofloat(n1,n2,order)
  y = Array.zerofloat(n1,n2)
  lbf.applyQ1(order,x,y,c1,c2)
  plot(y,2.0,"y")
  z = Array.zerofloat(n1,n2)
  lbf.applyForward(c1,c2,x,z)
  plot(z,2.0,"z")
  w = Array.zerofloat(n1,n2)
  lbf.applyInverse(c1,c2,z,w)
  plot(w,10.0,"w")
  print "max diff =",Array.max(Array.sub(w,x))
  plot(Array.sub(w,x),10.0,"w-x")

def readImage():
  fileName = dataDir+"/seis/vg/junks.dat"
  ais = ArrayInputStream(fileName,ByteOrder.LITTLE_ENDIAN)
  f = Array.zerofloat(n1,n2)
  ais.readFloats(f)
  ais.close()
  return f

def flip2(f):
  n1 = len(f[0])
  n2 = len(f)
  g = Array.zerofloat(n1,n2)
  for i2 in range(n2):
    Array.copy(f[n2-1-i2],g[i2])
  return g

#############################################################################
# plot

def plot(f,clip=0.0,png=None):
  n1 = len(f[0])
  n2 = len(f)
  p = panel()
  s1 = Sampling(n1,1.0,0.0)
  s2 = Sampling(n2,1.0,0.0)
  if n1<50 and n2<50:
    s1 = Sampling(n1,1,-(n1-1)/2)
    s2 = Sampling(n2,1,-(n2-1)/2)
  pv = p.addPixels(s1,s2,f)
  if clip!=0.0:
    pv.setClips(-clip,clip)
  else:
    pv.setPercentiles(0.0,100.0)
  pv.setInterpolation(PixelsView.Interpolation.NEAREST)
  frame(p,png)

def panel():
  p = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  p.addColorBar()
  p.setColorBarWidthMinimum(widthColorBar)
  return p

def frame(panel,png=None):
  frame = PlotFrame(panel)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setFontSize(fontSize)
  frame.setSize(width,height)
  frame.setVisible(True)
  if png and pngDir:
    frame.paintToPng(200,6,pngDir+"/"+png+".png")
  return frame

#############################################################################
# Do everything on Swing thread.

class RunMain(Runnable):
  def run(self):
    main(sys.argv)
SwingUtilities.invokeLater(RunMain())