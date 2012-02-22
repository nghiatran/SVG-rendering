package com.svg.test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class MyImageView extends ImageView implements OnTouchListener{
	private float scale = 1.f;
	private float totalScale = 1;
	private float[] f;
	
    static final int NONE = 0;
    static final int ZOOM = 1;
    static final int DRAG =2;
    int mode = NONE;

    float oldDist = 1f;
    String TAG ="";

	PointF mid = new PointF();
	
	private float startX;
	private float startY;

	private float midX;
	private float midY;
	
	private Matrix matrix;
	private Matrix savedMatrix;
	
	public MyImageView(Context context) {
		super(context);
        init(context);
		this.setBackgroundColor(Color.WHITE);
		setOnTouchListener(this);
	}

	public MyImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	
	protected void init(Context context){
		matrix = new Matrix();
		savedMatrix = new Matrix();
		
		f = new float[9];
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
	    // Parse the SVG file from the resource
	    SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.example_map);
	    // Get the picture
	    Picture picture = svg.getPicture();
	    canvas.setMatrix(matrix);
	    canvas.drawPicture(picture);
	}
	@Override
	public boolean isInEditMode() {
		return super.isInEditMode();
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		 switch (event.getAction() & MotionEvent.ACTION_MASK) {
		 case MotionEvent.ACTION_DOWN:
			 Log.d(TAG, "mode=DRAG");
			 savedMatrix.set(matrix);
			 startX = event.getX();
			 startY = event.getY();
			 mode=DRAG;
			 break;
		 case MotionEvent.ACTION_POINTER_DOWN:
			 oldDist = spacing(event);
			 if (oldDist > 10f) {
				 savedMatrix.set(matrix);
			     midX = ( event.getX(0) + event.getX(1)) /2;
			     midY = ( event.getY(0) + event.getY(1)) /2;
				 mode = ZOOM;
				 
			 }
			 break;
		 case MotionEvent.ACTION_UP:
		 case MotionEvent.ACTION_POINTER_UP:
			 mode = NONE;
			 break;
			case MotionEvent.ACTION_MOVE:
				if (mode == DRAG) {
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - startX,
							event.getY() - startY);
				}
				else if (mode == ZOOM) {
					float newDist = spacing(event);
					if (newDist > 10f) {
						matrix.set(savedMatrix);
						scale = newDist / oldDist;
						matrix.postScale(scale, scale, midX, midY);
						matrix.getValues(f);
						totalScale = f[Matrix.MSCALE_X];
					}
				}
				invalidate();
			 break;
		 }

		 return true;
	}
	
	private float spacing(MotionEvent event) {
		   float x = event.getX(0) - event.getX(1);
		   float y = event.getY(0) - event.getY(1);
		   return FloatMath.sqrt(x * x + y * y);
		}
	
}
