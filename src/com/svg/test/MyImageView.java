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
	
	int pictureHeight, pictureWidth;
	int viewerHeight, viewerWidth;
	
	float initialPicX, initialPicY;
    Picture picture;
    
    public static final int KEY_TRANS_X = 0, KEY_TRANS_Y = 1;
	
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
		
		this.setBackgroundColor(Color.WHITE);
		setOnTouchListener(this);
		
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.example_map);
	    picture = svg.getPicture();
	    pictureWidth = picture.getWidth();
	    pictureHeight = picture.getHeight();
	    
	    // for wrap_content viewer and picture size will be same. need to implement seperate logic for other cases.
	    viewerHeight = pictureHeight;
	    viewerWidth = pictureWidth;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(matrix == null) {
			matrix = canvas.getMatrix();
			matrix.getValues(f);
			initialPicX = f[Matrix.MTRANS_X];
			initialPicY = f[Matrix.MTRANS_Y];
		}
		
	    canvas.setMatrix(matrix);
	    canvas.drawPicture(picture);
	}
	@Override
	public boolean isInEditMode() {
		return super.isInEditMode();
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(pictureWidth, pictureHeight);
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
					
					
					float[] desiredTranslations = new float[2];
					
					
					desiredTranslations[KEY_TRANS_X] = event.getX() - startX;
					desiredTranslations[KEY_TRANS_Y] = event.getY() - startY;
					
					
					Log.d("2359", "Desired Translation x: " + desiredTranslations[KEY_TRANS_X] +" y:"+ desiredTranslations[KEY_TRANS_Y]);
					
					
					calculateRestrictedTranslations(desiredTranslations);
					
					Log.d("2359", "Translating x: " + desiredTranslations[KEY_TRANS_X] +" y:"+ desiredTranslations[KEY_TRANS_Y]);
					matrix.postTranslate(desiredTranslations[KEY_TRANS_X], desiredTranslations[KEY_TRANS_Y]);
					
					
					
				}
				else if (mode == ZOOM) {
					float newDist = spacing(event);
					if (newDist > 10f) {
						matrix.set(savedMatrix);
						scale = newDist / oldDist;
						// restrict scale
						scale = calculateRestrictedScale(scale, 1.0f, 3.0f);
						
						Log.d("2359", "Changed scale:"+scale);
						matrix.postScale(scale, scale, midX, midY);
						
						adjustXYAfterScaling();
						
						
						matrix.getValues(f);
						totalScale = f[Matrix.MSCALE_X];
					}
				}
				invalidate();
			 break;
		 }

		 return true;
	}
	
	private void adjustXYAfterScaling(){
		float[] mValues = new float[9];
		matrix.getValues(mValues);
		
		float currentX = mValues[Matrix.MTRANS_X];
		float currentY = mValues[Matrix.MTRANS_Y];
		float adjustX = 0, adjustY = 0;
		
		
		float currentWidth = (int) (pictureWidth * mValues[Matrix.MSCALE_X]);
		float currentHeight = (int) (pictureHeight * mValues[Matrix.MSCALE_Y]); 
		
		float maxX = initialPicX; // we supporting min zoom 1, increasing from this X or Y value means we are creating white space on left or top.
		float maxY = initialPicY; // min zoom is 1
		
		float minX = (initialPicX + viewerWidth - currentWidth);
		float minY = (initialPicY + viewerHeight - currentHeight);
		
		
		if(currentX < minX) {
			adjustX =  (minX - currentX);
		}
		if(currentY < minY){
			adjustY = (minY - currentY);
		}
		if(currentX > maxX) {
			adjustX = (maxX - currentX);
		}
		if(currentY > maxY) {
			adjustY = (maxY - currentY);
		}
		
		
		matrix.postTranslate(adjustX, adjustY);
	}
	
	private void calculateRestrictedTranslations(float[] desiredTranslations){
		float[] mValues = new float[9];
		matrix.getValues(mValues);
		float currentWidth = (int) (pictureWidth * mValues[Matrix.MSCALE_X]);
		float currentHeight = (int) (pictureHeight * mValues[Matrix.MSCALE_Y]); 
		
		
		
		float maxX = initialPicX; // we supporting min zoom 1, increasing from this X or Y value means we are creating white space on left or top.
		float maxY = initialPicY; // min zoom is 1
		
		// if viewerWidth is 100, and image's currentWidth (after scale etc) is 200.. then we need to set the X position to -100 to see the ending portion of image i.e. from 101 to 200px part.
		// initialPicX and initialPicY variable is introduced coz, initial X and Y may not be zero in all cases. for understanding of concept ignore them.
		float minX = (initialPicX + viewerWidth - currentWidth);
		float minY = (initialPicY + viewerHeight - currentHeight);
		
		// getting projected values, will check them if they comply with our boundries.. otherwise will adjust user's desired translation.
		float projectedX = mValues[Matrix.MTRANS_X] + desiredTranslations[KEY_TRANS_X];
		float projectedY = mValues[Matrix.MTRANS_Y] + desiredTranslations[KEY_TRANS_Y];
		
		if(projectedX < minX) {
			desiredTranslations[KEY_TRANS_X] = desiredTranslations[KEY_TRANS_X] + (minX - projectedX);
		}
		if(projectedY < minY){
			desiredTranslations[KEY_TRANS_Y] = desiredTranslations[KEY_TRANS_Y] + (minY - projectedY);
		}
		if(projectedX > maxX) {
			desiredTranslations[KEY_TRANS_X] = desiredTranslations[KEY_TRANS_X] - (projectedX - maxX);
		}
		if(projectedY > maxY) {
			desiredTranslations[KEY_TRANS_Y] = desiredTranslations[KEY_TRANS_Y] - (projectedY - maxY);
		}
	}
	
	private float calculateRestrictedScale(float desiredScale, float minZoom, float maxZoom){
		// restricting scale
		float[] tempMatrixVals = new float[9];
		float currentScale, predictedZoom;
		matrix.getValues(tempMatrixVals);
		currentScale = tempMatrixVals[Matrix.MSCALE_X];
		
		predictedZoom = desiredScale * currentScale;
		
		if(predictedZoom > maxZoom) {
			desiredScale = maxZoom / currentScale;
		}
		if(predictedZoom < minZoom){
			desiredScale =  minZoom / currentScale;
		}
		
		return desiredScale;
	}
	
	private float spacing(MotionEvent event) {
		   float x = event.getX(0) - event.getX(1);
		   float y = event.getY(0) - event.getY(1);
		   return FloatMath.sqrt(x * x + y * y);
		}
	
}
