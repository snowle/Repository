package com.demo.recordvoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class FFTView extends View{
	
	public Paint paint;
	public Path path;
	private double[] fftData;

	public FFTView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@SuppressLint("DrawAllocation") @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);		
		canvas.drawColor(Color.BLACK);
		
		int width = getWidth();
		int height = getHeight();		
		
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.YELLOW);
		paint.setStyle(Paint.Style.STROKE);	
		
		canvas.drawText("频谱图", width-50, 15, paint);
		
		//drawWave(fftData);
		path = new Path(); 
		path.moveTo(0, height-10);
		if (fftData!=null) {
			for (int i = 0; i < fftData.length; i++) {
				float j = i*width/fftData.length;
				path.moveTo(j, height-10);
				path.lineTo(j, (float)(height-10-Math.abs(fftData[i])/16));				
			}
		}else {
			path.lineTo(width, height-10);
		}
		canvas.drawPath(path, paint);	
		
		drawFilterWindows(canvas);
				
	}
	
//	/**
//	 * 根据数据的变化重绘波形
//	 */
//	public void drawWave(double[] fftData) {
//		
//		this.fftData = {1,2};
//		invalidate();
//	}
	
	public void drawFilterWindows(Canvas canvas) {
		Paint paint2;
		Path path2;
		
		int width = getWidth();
		int height = getHeight();
		int baseline = height - 10;
		
		paint2 = new Paint();
		paint2.setAntiAlias(true);
		paint2.setColor(Color.RED);
		paint2.setStyle(Paint.Style.STROKE);
		
		path2 = new Path();
		Calcs calcs = new Calcs();
		
		path2.moveTo(0, baseline);
		for (int i = 0; i < width; i++) {
			path2.lineTo(i, (float) (baseline - 100*calcs.windowCac(0, 28, 89, i)));
		}
		
		path2.moveTo(0, baseline);
		for (int i = 0; i < width; i++) {
			path2.lineTo(i, (float) (baseline - 100*calcs.windowCac(26, 89, 154, i)));
		}
		
		path2.moveTo(0, baseline);
		for (int i = 0; i < width; i++) {
			path2.lineTo(i, (float) (baseline - 100*calcs.windowCac(26, 89, 154, i)));
		}
		
		path2.moveTo(0, baseline);
		for (int i = 0; i < width; i++) {
			path2.lineTo(i, (float) (baseline - 100*calcs.windowCac(89, 154, 224, i)));
		}
		
		path2.moveTo(0, baseline);
		for (int i = 0; i < width; i++) {
			path2.lineTo(i, (float) (baseline - 100*calcs.windowCac(154, 224, 300, i)));
		}
		canvas.drawPath(path2, paint2);
	}

}
