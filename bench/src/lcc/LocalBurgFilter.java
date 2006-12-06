/****************************************************************************
Copyright (c) 2006, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is 
available at http://www.eclipse.org/legal/cpl-v10.html
****************************************************************************/
package lcc;

import edu.mines.jtk.dsp.*;
import edu.mines.jtk.util.*;
import static edu.mines.jtk.util.MathPlus.*;

/**
 * Local Burg prediction error filtering for 2-D and 3-D images.
 * @author Dave Hale, Colorado School of Mines
 * @version 2006.11.28
 */
public class LocalBurgFilter {

  public LocalBurgFilter(double sigma) {
    _rgf = new RecursiveGaussianFilter(sigma);
    _lcf = new LocalCorrelationFilter(
      LocalCorrelationFilter.Type.SYMMETRIC,
      LocalCorrelationFilter.Window.GAUSSIAN,
      sigma);
  }

  /**
   * Applies filter.
   * The input and output arrays f and g can be the same array.
   * @param m filter order.
   * @param x the input array.
   * @param y the output array.
   */
  public void applyQ1(
    int m, float[][] x, float[][] y, 
    float[][][] c1, float[][][] c2) 
  {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = y;
    float[][] b = new float[n2][n1];
    float[][] r00 = new float[n2][n1];
    float[][] rpm = new float[n2][n1];
    float[][] rp0 = new float[n2][n1];
    float[][] r0p = new float[n2][n1];
    float[][] rxx = new float[n2][n1];
    Array.copy(x,b);
    Array.copy(x,f);
    for (int k=0; k<m; ++k) {
      _lcf.setInputs(b,b);
      _lcf.correlate(0,0,r00);
      _lcf.correlate(1,-1,rpm);
      if (k>0) {
        _lcf.setInputs(f,f);
        _lcf.correlate(0,0,rxx);
        Array.add(rxx,r00,r00);
        _lcf.correlate(1,-1,rxx);
        Array.add(rxx,rpm,rpm);
      } else {
        Array.mul(2.0f,r00,r00);
        Array.mul(2.0f,rpm,rpm);
      }
      _lcf.setInputs(b,f);
      _lcf.correlate(1,0,rp0);
      Array.mul(2.0f,rp0,rp0);
      _lcf.correlate(0,1,r0p);
      Array.mul(2.0f,r0p,r0p);
      for (int i2=n2-1; i2>k; --i2) {
        for (int i1=n1-1; i1>k; --i1) {
          double b1 = rp0[i2][i1];
          double b2 = r0p[i2][i1];
          double a11 = r00[i2][i1];
          double a21 = rpm[i2][i1];
          double a22 = a11;
          double l11 = sqrt(a11);
          double l21 = a21/l11;
          double d22 = a22-l21*l21;
          double x1 = 0.0;
          double x2 = 0.0;
          if (d22>0.0) {
            double l22 = sqrt(d22);
            double v1 = b1/l11;
            double v2 = (b2-l21*v1)/l22;
            x2 = v2/l22;
            x1 = (v1-l21*x2)/l11;
          } else {
            System.out.println("not pd: i1="+i1+" i2="+i2);
          }
          float c1k = (float)x1;
          float c2k = (float)x2;
          float ca = abs(c1k)+abs(c2k);
          if (ca>CMAX) {
            float cs = CMAX/ca;
            c1k *= cs;
            c2k *= cs;
          }
          if (c1!=null) {
            c1[k][i2][i1] = c1k;
            c2[k][i2][i1] = c2k;
          }
          float bmm = b[i2-1][i1-1];
          float b0m = b[i2-1][i1];
          float bm0 = b[i2][i1-1];
          float f00 = f[i2][i1];
          float f0m = f[i2-1][i1];
          float fm0 = f[i2][i1-1];
          b[i2][i1] = bmm-c2k*f0m-c1k*fm0;
          f[i2][i1] = f00-c1k*bm0-c2k*b0m;
        }
      }
    }
  }
  public void applyForward(
    float[][][] c1, float[][][] c2, 
    float[][] x, float[][] y) 
  {
    int m = c1.length;
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = new float[m+1][n1];
    float[][] b = new float[m+1][n1];
    float[][] f2 = new float[m+1][n1];
    float[][] b2 = new float[m+1][n1];
    for (int i2=0; i2<n2; ++i2) {
      float[] f0 = f[0];
      float[] b0 = b[0];
      for (int i1=0; i1<n1; ++i1) {
        f0[i1] = x[i2][i1];
        b0[i1] = x[i2][i1];
      }
      for (int k=1; k<=m; ++k) {
        float[] fk = f[k];
        float[] bk = b[k];
        float[] fkm = f[k-1];
        float[] bkm = b[k-1];
        float[] f2m = f2[k-1];
        float[] b2m = b2[k-1];
        float[] c1k = c1[k-1][i2];
        float[] c2k = c2[k-1][i2];
        int i1 = k-1;
        fk[i1] = fkm[i1]-c2k[i1]*b2m[i1];
        bk[i1] =        -c1k[i1]*f2m[i1];
        for (i1=k; i1<n1; ++i1) {
          float c1i = c1k[i1];
          float c2i = c2k[i1];
          fk[i1] = fkm[i1  ]-c1i*bkm[i1-1]-c2i*b2m[i1  ];
          bk[i1] = b2m[i1-1]-c1i*f2m[i1  ]-c2i*fkm[i1-1];
        }
      }
      for (int k=0; k<=m; ++k) {
        for (int i1=k; i1<n1; ++i1) {
          f2[k][i1] = f[k][i1];
          b2[k][i1] = b[k][i1];
        }
      }
      for (int i1=0; i1<n1; ++i1) {
        y[i2][i1] = f[m][i1];
      }
    }
  }
  public void applyInverse(
    float[][][] c1, float[][][] c2, 
    float[][] x, float[][] y) 
  {
    int m = c1.length;
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = new float[m+1][n1];
    float[][] b = new float[m+1][n1];
    float[][] f2 = new float[m+1][n1];
    float[][] b2 = new float[m+1][n1];
    for (int i2=0; i2<n2; ++i2) {
      float[] fm = f[m];
      for (int i1=0; i1<n1; ++i1) {
        fm[i1] = x[i2][i1];
      }
      for (int k=m; k>=1; --k) {
        float[] fk = f[k];
        float[] fkm = f[k-1];
        float[] bkm = b[k-1];
        float[] b2m = b2[k-1];
        float[] c1k = c1[k-1][i2];
        float[] c2k = c2[k-1][i2];
        int i1 = k-1;
        fkm[i1] = fk[i1]+c2k[i1]*b2m[i1];
        for (i1=k; i1<n1; ++i1) {
          fkm[i1] = fk[i1]+c1k[i1]*bkm[i1-1]+c2k[i1]*b2m[i1];
        }
      }
      float[] b0 = b[0];
      float[] f0 = f[0];
      for (int i1=0; i1<n1; ++i1) {
        b0[i1] = f0[i1];
      }
      for (int k=1; k<=m; ++k) {
        float[] bk = b[k];
        float[] fkm = f[k-1];
        float[] f2m = f2[k-1];
        float[] b2m = b2[k-1];
        float[] c1k = c1[k-1][i2];
        float[] c2k = c2[k-1][i2];
        int i1 = k-1;
        bk[i1] = -c1k[i1]*f2m[i1];
        for (i1=k; i1<n1; ++i1) {
          bk[i1] = b2m[i1-1]-c1k[i1]*f2m[i1]-c2k[i1]*fkm[i1-1];
        }
      }
      for (int k=0; k<=m; ++k) {
        for (int i1=k; i1<n1; ++i1) {
          f2[k][i1] = f[k][i1];
          b2[k][i1] = b[k][i1];
        }
      }
      for (int i1=0; i1<n1; ++i1) {
        y[i2][i1] = f[0][i1];
      }
    }
  }

  public float[][][] applyX(int m, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = y;
    float[][] b = new float[n2][n1];
    float[][] t = new float[n2][n1];
    float[][] ffk = new float[n2][n1];
    float[][] fb1 = new float[n2][n1];
    float[][] fb2 = new float[n2][n1];
    float[][] bb1 = new float[n2][n1];
    float[][] bb2 = new float[n2][n1];
    float[][][] c = new float[m][n2][n1];
    Array.copy(x,f);
    Array.copy(x,b);
    for (int k=0; k<m; ++k) {
      for (int i2=1; i2<n2; ++i2) {
        for (int i1=1; i1<n1; ++i1) {
          float fk = f[i2][i1];
          float b1 = b[i2][i1-1];
          float b2 = b[i2-1][i1];
          ffk[i2][i1] = fk*fk;
          bb1[i2][i1] = b1*b1;
          bb2[i2][i1] = b2*b2;
          fb1[i2][i1] = fk*b1;
          fb2[i2][i1] = fk*b2;
        }
      }
      _rgf.apply0X(ffk,t);  _rgf.applyX0(t,ffk);
      _rgf.apply0X(bb1,t);  _rgf.applyX0(t,bb1);
      _rgf.apply0X(bb2,t);  _rgf.applyX0(t,bb2);
      _rgf.apply0X(fb1,t);  _rgf.applyX0(t,fb1);
      _rgf.apply0X(fb2,t);  _rgf.applyX0(t,fb2);
      for (int i2=n2-1; i2>0; --i2) {
        for (int i1=n1-1; i1>0; --i1) {
          float cn1 = 2.0f*fb1[i2][i1];
          float cn2 = 2.0f*fb2[i2][i1];
          float cd1 = ffk[i2][i1]+bb1[i2][i1];
          float cd2 = ffk[i2][i1]+bb2[i2][i1];
          float c1 = (cd1!=0.0f)?cn1/cd1:0.0f;
          float c2 = (cd2!=0.0f)?cn2/cd2:0.0f;
          float s1 = c1*c1;
          float s2 = c2*c2;
          float ss = 1.0f/(s1+s2);
          float w1 = s1*ss;
          float w2 = s2*ss;
          float fk = f[i2][i1];
          float b1 = b[i2][i1-1];
          float b2 = b[i2-1][i1];
          float f1 = fk-c1*b1;
          float f2 = fk-c2*b2;
                b1 = b1-c1*fk;
                b2 = b2-c2*fk;
          f[i2][i1] = w1*f1+w2*f2;
          b[i2][i1] = w1*b1+w2*b2;
          c[k][i2][i1] = c2;
        }
      }
    }
    return c;
  }

  public float[][][] applyA1(int m, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = y;
    float[][] b = new float[n2][n1];
    float[][] t = new float[n2][n1];
    float[][] fb = new float[n2][n1];
    float[][] ff = new float[n2][n1];
    float[][] bb = new float[n2][n1];
    float[][][] c = new float[m][n2][n1];
    Array.copy(x,f);
    Array.copy(x,b);
    for (int k=0; k<m; ++k) {
      int k1 = (k+2)/2;
      int k2 = (k+1)/2;
      int j1 = (k+1)%2;
      int j2 = (k+0)%2;
      for (int i2=k2; i2<n2; ++i2) {
        for (int i1=k1; i1<n1; ++i1) {
          float fk = f[i2][i1];
          float bk = b[i2-j2][i1-j1];
          fb[i2][i1] = fk*bk;
          ff[i2][i1] = fk*fk;
          bb[i2][i1] = bk*bk;
        }
      }
      _rgf.apply0X(fb,t);  _rgf.applyX0(t,fb);
      _rgf.apply0X(ff,t);  _rgf.applyX0(t,ff);
      _rgf.apply0X(bb,t);  _rgf.applyX0(t,bb);
      for (int i2=n2-1; i2>=k2; --i2) {
        for (int i1=n1-1; i1>=k1; --i1) {
          float cn = 2.0f*fb[i2][i1];
          float cd = ff[i2][i1]+bb[i2][i1];
          float ck = (cd!=0.0f)?cn/cd:0.0f;
          float fk = f[i2][i1];
          float bk = b[i2-j2][i1-j1];
          f[i2][i1] = fk-ck*bk;
          b[i2][i1] = bk-ck*fk;
          c[k][i2][i1] = ck;
        }
      }
    }
    return c;
  }

  public float[][][] applyA4(int m, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = y;
    float[][] b = new float[n2][n1];
    float[][] t = new float[n2][n1];
    float[][] fb = new float[n2][n1];
    float[][] ff = new float[n2][n1];
    float[][] bb = new float[n2][n1];
    float[][][] c = new float[m][n2][n1];
    Array.copy(x,f);
    Array.copy(x,b);
    for (int k=0; k<m; ++k) {
      int k2 = (k+2)/2;
      int k1 = (k+1)/2;
      int j2 = (k+1)%2;
      int j1 = (k+0)%2;
      for (int i2=0; i2<n2-k2; ++i2) {
        for (int i1=k1; i1<n1; ++i1) {
          float fk = f[i2][i1];
          float bk = b[i2+j2][i1-j1];
          fb[i2][i1] = fk*bk;
          ff[i2][i1] = fk*fk;
          bb[i2][i1] = bk*bk;
        }
      }
      _rgf.apply0X(fb,t);  _rgf.applyX0(t,fb);
      _rgf.apply0X(ff,t);  _rgf.applyX0(t,ff);
      _rgf.apply0X(bb,t);  _rgf.applyX0(t,bb);
      for (int i2=0; i2<n2-k2; ++i2) {
        for (int i1=n1-1; i1>=k1; --i1) {
          float cn = 2.0f*fb[i2][i1];
          float cd = ff[i2][i1]+bb[i2][i1];
          float ck = (cd!=0.0f)?cn/cd:0.0f;
          float fk = f[i2][i1];
          float bk = b[i2+j2][i1-j1];
          f[i2][i1] = fk-ck*bk;
          b[i2][i1] = bk-ck*fk;
          c[k][i2][i1] = ck;
        }
      }
    }
    return c;
  }

  public float[][][] applyA3(int m, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] f = y;
    float[][] b = new float[n2][n1];
    float[][] t = new float[n2][n1];
    float[][] fb = new float[n2][n1];
    float[][] ff = new float[n2][n1];
    float[][] bb = new float[n2][n1];
    float[][][] c = new float[m][n2][n1];
    Array.copy(x,f);
    Array.copy(x,b);
    for (int k=0; k<m; ++k) {
      int k1 = (k+2)/2;
      int k2 = (k+1)/2;
      int j1 = (k+1)%2;
      int j2 = (k+0)%2;
      for (int i2=0; i2<n2-k2; ++i2) {
        for (int i1=0; i1<n1-k1; ++i1) {
          float fk = f[i2][i1];
          float bk = b[i2+j2][i1+j1];
          fb[i2][i1] = fk*bk;
          ff[i2][i1] = fk*fk;
          bb[i2][i1] = bk*bk;
        }
      }
      _rgf.apply0X(fb,t);  _rgf.applyX0(t,fb);
      _rgf.apply0X(ff,t);  _rgf.applyX0(t,ff);
      _rgf.apply0X(bb,t);  _rgf.applyX0(t,bb);
      for (int i2=0; i2<n2-k2; ++i2) {
        for (int i1=0; i1<n1-k1; ++i1) {
          float cn = 2.0f*fb[i2][i1];
          float cd = ff[i2][i1]+bb[i2][i1];
          float ck = (cd!=0.0f)?cn/cd:0.0f;
          float fk = f[i2][i1];
          float bk = b[i2+j2][i1+j1];
          f[i2][i1] = fk-ck*bk;
          b[i2][i1] = bk-ck*fk;
          c[k][i2][i1] = ck;
        }
      }
    }
    return c;
  }

  ///////////////////////////////////////////////////////////////////////////
  // private

  private RecursiveGaussianFilter _rgf;
  private LocalCorrelationFilter _lcf;
  private static final float CMAX = 0.999f;
}