package com.svg.test;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

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
	private Picture picture;
	private HashMap<String, Properties> objects;
	private Paint paint2;
	private float objX;
	private float objY;
	private float objWidth;
	private float objHeight;
	
	int height, width;
	
	public MyImageView(Context context) {
		super(context);
        init(context);
		
	}

	public MyImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	
	protected void init(Context context){
		//  matrix = new Matrix();
		savedMatrix = new Matrix();
		
		f = new float[9];

		paint2 = new Paint();
		paint2.setColor(Color.RED);
		paint2.setAlpha(200);
		paint2.setStrokeWidth(2.0f);
		paint2.setStyle(Paint.Style.FILL);
		
	    SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.example_map2);
	    picture = svg.getPicture();
	    objects = SVGParser.getObjectsMap();

		this.setBackgroundColor(Color.WHITE);
		setOnTouchListener(this);

	    width = picture.getWidth();
	    height = picture.getHeight();
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if(matrix == null)
			matrix = canvas.getMatrix();
	    canvas.setMatrix(matrix);
	    canvas.drawPicture(picture);
	    canvas.drawRect(objX, objY, objX + objWidth, objY + objHeight, paint2);
	}
	@Override
	public boolean isInEditMode() {
		return super.isInEditMode();
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(width, height);
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
			 if(event.getX() == startX && event.getY()==startY){
				 if(isWithinBounds(event)){
					 invalidate();
				 }
			 }
			 break;
		 case MotionEvent.ACTION_POINTER_UP:
			 break;
		 case MotionEvent.ACTION_MOVE:

				if (mode == ZOOM) {
					float newDist = spacing(event);
					if (newDist > 10f) {
						matrix.set(savedMatrix);
						scale = newDist / oldDist;
						matrix.postScale(scale, scale, midX, midY);
						matrix.getValues(f);
						totalScale = f[Matrix.MSCALE_X];
					}
				}else if(mode==DRAG){
					mode = DRAG;
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - startX,
							event.getY() - startY);
					matrix.getValues(f);
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
	
	private boolean isWithinBounds(MotionEvent e){
		boolean ret = false;
		for(Entry<String, Properties> object : objects.entrySet()){
			Properties p = object.getValue();
			objX = (Float)p.get("x");
			objY = (Float)p.get("y");
			objWidth = (Float)p.get("width");
			objHeight = (Float)p.get("height");

			float x = (e.getRawX()-f[Matrix.MTRANS_X])/totalScale;
			float y = (e.getRawY()-f[Matrix.MTRANS_Y])/totalScale;
	        if ((x > objX) && (x < (objX + objWidth))) {
	            if ((y > objY) && (y < (objY + objHeight))) {
	            	Toast.makeText(getContext(), object.getKey().toString(), Toast.LENGTH_SHORT).show();;
	                return true;
	            }
	        }
		}
		return ret;
	}
}
