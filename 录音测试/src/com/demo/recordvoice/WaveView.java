package com.demo.recordvoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View{

	public Paint paint;
	public Path path;
	private int t;
	
	public WaveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}

	@SuppressLint("DrawAllocation") @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0xffF0E68C); 
		
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
		
		int width = getWidth();
		int height = getHeight();
	
		drawWave(t);
		
		float Y = (float) (height/2*(1+Math.sin(t*Math.PI/180)));
		
		canvas.drawLine(0, Y, width, Y, paint);
		

	}
	
	/**
	 * 根据数据的变化重绘波形
	 */
	public void drawWave(int t) {
		
		this.t = MainActivity.a;
		invalidate();
	}

}
