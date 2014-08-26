package com.google.corp.productivity.specialprojects.android.samples.fft;

import android.util.Log;

import com.google.corp.productivity.specialprojects.android.fft.RealDoubleFFT;

// Short Time Fourier Transform
public class STFT {
  // data for frequency Analysis
  private double[] spectrumAmpOut;
  private double[] spectrumAmpOutDB;
  private double[] spectrumAmpIn;
  private double[] spectrumAmpInTmp;
  private double[] wnd;
  private int spectrumAmpPt;
  private double[][] spectrumAmpOutArray;
  private int spectrumAmpOutArrayPt = 0;                                   // Pointer for spectrumAmpOutArray
  private int nAnalysed = 0;
  private RealDoubleFFT spectrumAmpFFT;

  public STFT(int i_fftlen) {
    if (((-i_fftlen)&i_fftlen) != i_fftlen) {
      // error: i_fftlen should be power of 2
      throw new IllegalArgumentException("Currently, only power of 2 are supported in fftlen");
    }
    spectrumAmpOut   = new double[i_fftlen/2+1];
    spectrumAmpOutDB = new double[i_fftlen/2+1];
    spectrumAmpIn    = new double[i_fftlen];
    spectrumAmpInTmp = new double[i_fftlen];
    wnd              = new double[i_fftlen];
    spectrumAmpFFT   = new RealDoubleFFT(spectrumAmpIn.length);
    spectrumAmpOutArray = new double[2][];                       // 2 since half overlap
    for (int i = 0; i < spectrumAmpOutArray.length; i++) {
      spectrumAmpOutArray[i] = new double[i_fftlen/2+1];
    }
    
    double normalizeFactor = 0;
    for (int i=0; i<wnd.length; i++) {
      //wnd[i] = 1;
      // Hanning, hw=1
      //wnd[i] = 0.5*(1-Math.cos(2*Math.PI*i/(wnd.length-1.))) *2;  // *2 to preserve the peak
      // Blackman, hw=2
      //wnd[i] = 0.42-0.5*Math.cos(2*Math.PI*i/(wnd.length-1))+0.08*Math.cos(4*Math.PI*i/(wnd.length-1));
      // Blackman_Harris, hw=3
      wnd[i] = (0.35875-0.48829*Math.cos(2*Math.PI*i/(wnd.length-1))+0.14128*Math.cos(4*Math.PI*i/(wnd.length-1))-0.01168*Math.cos(6*Math.PI*i/(wnd.length-1))) *2;
      normalizeFactor += wnd[i];
    }
    normalizeFactor = wnd.length / normalizeFactor;
    for (int i=0; i<wnd.length; i++) {
      wnd[i] *= normalizeFactor;
    }
  }

  public void feedData(short[] ds) {
    feedData(ds, ds.length);
  }
  public void feedData(short[] ds, int dsLen) {
    if (dsLen > ds.length) {
      Log.e("STFT", "dsLen > ds.length !");
      dsLen = ds.length;
    }
    int dsPt = 0;           // input data point to be read
    while (dsPt < dsLen) {
      while (spectrumAmpPt < spectrumAmpIn.length && dsPt < dsLen) {
        spectrumAmpIn[spectrumAmpPt] = ds[dsPt] / 32768.0;
        spectrumAmpPt++;
        dsPt++;
      }
      if (spectrumAmpPt == spectrumAmpIn.length) {    // enough data for one FFT
        for (int i = 0; i < wnd.length; i++) {
          spectrumAmpInTmp[i] = spectrumAmpIn[i] * wnd[i];
        }
        spectrumAmpFFT.ft(spectrumAmpInTmp);
        fftToAmp(spectrumAmpOutArray[spectrumAmpOutArrayPt], spectrumAmpInTmp);
        spectrumAmpOutArrayPt = (spectrumAmpOutArrayPt+1) % spectrumAmpOutArray.length;
        nAnalysed++;
//        spectrumAmpPt = 0;                          // no overlap
        // half overlap
        int n2 = spectrumAmpIn.length / 2;
        for (int i=0; i<n2; i++) {
          spectrumAmpIn[i] = spectrumAmpIn[i + n2];
        }
        spectrumAmpPt = n2;
      }
    }
  }

  private void fftToAmp(double[] dataOut, double[] data) {
    // data.length should be even number
    double scaler = 2.0*2.0 / (data.length * data.length);  // *2 since there are positive and negative frequency part
    dataOut[0] = data[0]*data[0] * scaler / 4.0;
    int j = 1;
    for (int i = 1; i < data.length - 1; i += 2, j++) {
      dataOut[j] = (data[i]*data[i] + data[i+1]*data[i+1]) * scaler;
    }
    dataOut[j] = data[data.length-1]*data[data.length-1] * scaler / 4.0;
  }

  // return recently calculated spectrum 
  final public double[] pollSpectrumAmp() {
    if (nAnalysed == 0) {    // no new result
      return spectrumAmpOut;
    }
    nAnalysed = 0;
    // put average of spectrumAmpOutArray to spectrumAmpOut
    for (int j = 0; j < spectrumAmpOut.length; j++) {
      spectrumAmpOut[j] = 0;
    }
    for (int i = 0; i < spectrumAmpOutArray.length; i++) {
      for (int j = 0; j < spectrumAmpOut.length; j++) {
        spectrumAmpOut[j] += spectrumAmpOutArray[i][j];
      }
//      Log.i(AnalyzeActivity.TAG, "pollSpectrumAmp(): while loop, st[1] = " + Double.toString(spectrumAmpOutTmp[1]));
    }
    for (int j = 0; j < spectrumAmpOut.length; j++) {
      spectrumAmpOut[j] /= spectrumAmpOutArray.length;
    }
//    Log.i(AnalyzeActivity.TAG, "pollSpectrumAmp(): sz = " + Integer.toString(sz) + "  s[1]=" + Double.toString(spectrumAmpOut[1]));
    return spectrumAmpOut;
  }
  
  final public double[] getSpectrumAmp() {
    return pollSpectrumAmp();
  }
  
  final public double[] getSpectrumAmpDB() {
    if (nAnalysed != 0) {
      final double[] x = getSpectrumAmp();
      for (int i = 0; i < spectrumAmpOutDB.length; i++) {
        spectrumAmpOutDB[i] = 10.0 * Math.log10(x[i]);
      }
    }
//  Log.i(AnalyzeActivity.TAG, "pollSpectrumAmp():  s[1]=" + Double.toString(spectrumAmpOutTmp[1]));
    return spectrumAmpOutDB;
  }
  
  public int nElemSpectrumAmp() {
    return nAnalysed;
  }
  
  public void clear() {
    spectrumAmpPt = 0;
    for (int i=0; i<spectrumAmpOut.length; i++) {
      spectrumAmpOut[i] = 0;
    }
  }

}
