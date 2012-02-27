package com.svg.test;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;


public class MyImageView extends ImageView implements OnTouchListener{
	
	private static final float MIN_ZOOM_SCALE = 0.5f;
	private static final float MAX_ZOOM_SCALE = 10.0f; // 10x of whatever user has initially on screen
	
	private static final int NONE = 0;
	private static final int ZOOM = 1;
	private static final int DRAG = 2;
	
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
	private HashMap<String, Properties> objects; //this will contain all the object/shops (with its coordinates) parsed from the svg file
	private Paint highlightColor;
	private float objX;
	private float objY;
	private float objWidth;
	private float objHeight;
	String highlightType;
	Properties highlightObjProperties;

	int height, width;

	int pictureHeight, pictureWidth;
	int viewerHeight, viewerWidth;
	
	float initialPicX, initialPicY;

    float minSupportedZoom, maxSupportedZoom;
    
    public static final int KEY_TRANS_X = 0, KEY_TRANS_Y = 1;
    public static final String VAL_TYPE_PATH="path";
    public static final String VAL_TYPE_RECT="rect";
    public static final String PROP_KEY_TYPE="type";
    public static final String PROP_KEY_X="x";
    public static final String PROP_KEY_Y="y";
    public static final String PROP_KEY_PATH_OBJ="path_obj";
	public static final String PROP_KEY_WIDTH = "width";
	public static final String PROP_KEY_HEIGHT = "height";
    
    private HashMap<String, Integer > id_color_map;

    
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

		savedMatrix = new Matrix();
        this.setBackgroundColor(Color.WHITE);
        setOnTouchListener(this);
        
		highlightColor = new Paint();
		highlightColor.setColor(Color.RED);
		highlightColor.setAlpha(200);
		highlightColor.setStrokeWidth(2.0f);
		highlightColor.setStyle(Paint.Style.FILL);
		
	    SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.drawing2);
	    picture = svg.getPicture();
	    objects = SVGParser.getObjectsMap();

	    pictureWidth = picture.getWidth();
	    pictureHeight = picture.getHeight();
	    //getAllPAthPoints();
	    createTopMapCanvas();
	}


	@Override
	protected void onDraw(Canvas canvas) {
		if(matrix == null) {
			initializeMatrix(canvas);
		}
		
		super.onDraw(canvas);
	    canvas.setMatrix(matrix);
	    canvas.drawPicture(picture);
	    if(highlightType!=null && highlightType.equals(VAL_TYPE_RECT)){
	    	Log.d("", "draw rect");
			float x = (Float)highlightObjProperties.get(PROP_KEY_X);
			float y = (Float)highlightObjProperties.get(PROP_KEY_Y);
			float width = (Float)highlightObjProperties.get(PROP_KEY_WIDTH);
			float height = (Float)highlightObjProperties.get(PROP_KEY_HEIGHT);
			Log.d("", "x="+x);
			Log.d("", "y="+y);
			Log.d("", "width="+width);
			Log.d("", "height="+height);
			topCanvas.drawRect(x, y, x + width, y + height, highlightColor);
			
	    }else if(highlightType!=null && highlightType.equals(VAL_TYPE_PATH)){
	    	Path p = (Path) highlightObjProperties.get(PROP_KEY_PATH_OBJ);
	    	canvas.drawPath(p, highlightColor);
	    }
	    	
	}
	
	private void initializeMatrix(Canvas canvas){
		// Set matrix, set initial X and Y positions, set initial required zoom to fit to screen.
		
		float[] mValues = new float[9];
		matrix = canvas.getMatrix();
		matrix.getValues(mValues);
		initialPicX = mValues[Matrix.MTRANS_X];
		initialPicY = mValues[Matrix.MTRANS_Y];

		// calculateInitialZoomScale(): setting initial scale to fit image on screen, also setting min / max supported zoom scale.
		// applying initial zoom transformation to fit image on screen
		applyZoom(calculateInitialZoomScale());
		
		// explicitly setting image to center of screen. Though it will be done if zoom function above effectively change the size.
		adjustXYPositioning();
	}
	
	@Override
	public boolean isInEditMode() {
		return super.isInEditMode();
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		viewerWidth = measureWidth(widthMeasureSpec);
		viewerHeight = measureHeight(heightMeasureSpec);
		setMeasuredDimension(viewerWidth, viewerHeight);
	}

	/**
	 * Taken from google example: http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/LabelView.html
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = pictureWidth + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Taken from google example: http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/LabelView.html
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = (int) pictureHeight + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
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
				if (mode == DRAG) {

					matrix.set(savedMatrix);
					applyDraging(startX, event.getX(), startY, event.getY());

				}
				else if (mode == ZOOM) {
					float newDist = spacing(event);
					if (newDist > 10f) {
						matrix.set(savedMatrix);
						applyZoom(newDist / oldDist);
					}
				}
				invalidate();
			 break;
		 }

		 return true;
	}
	
	private void applyDraging(float startX, float endX, float startY, float endY){
		float[] desiredTranslations = new float[2];

		desiredTranslations[KEY_TRANS_X] = endX - startX;
		desiredTranslations[KEY_TRANS_Y] = endY - startY;

		calculateRestrictedTranslations(desiredTranslations);
		
		// A Bit of optimization, postTranslate matrix only if we have some values to adjust
		if(desiredTranslations[KEY_TRANS_X] != 0 || desiredTranslations[KEY_TRANS_Y] != 0) {
			matrix.postTranslate(desiredTranslations[KEY_TRANS_X], desiredTranslations[KEY_TRANS_Y]);
		}
	}
	
	private void applyZoom(float scale) {
		// making sure, the last scale set is according to our min / max scale
		scale = calculateRestrictedScale(scale, minSupportedZoom, maxSupportedZoom);
		
		// applying scaling transformation
		// A Bit of optimization, postScale matrix only if we have some scale to multiply
		if(scale != 1) {
			matrix.postScale(scale, scale, midX, midY);
			
			// adjusting image to center (usually when it was decreased scaling) [zoomout]
			if(scale <=1)
			   adjustXYPositioning();
		}
	}
	
	private float calculateInitialZoomScale(){
		float ratioWidth, ratioHeight, scale;
		ratioWidth = viewerWidth / (float) pictureWidth;
		ratioHeight = viewerHeight / (float) pictureHeight;
		
		if(ratioWidth < ratioHeight){
			scale = (float) ratioWidth;
			minSupportedZoom = (float) ratioWidth;
		}
		else {
			scale = (float) ratioHeight;
			minSupportedZoom = (float) ratioHeight;
		}
		
		minSupportedZoom = Math.min(MIN_ZOOM_SCALE, minSupportedZoom);
		maxSupportedZoom = minSupportedZoom * MAX_ZOOM_SCALE; // MAX_ZOOM_SCALE times of what displayed initially on user's screen
		
		midX = viewerWidth / 2; // set initial mid point to center of viewer
		midY = viewerHeight / 2;
		
		return scale;
	}
	
	private void adjustXYPositioning(){
		float[] mValues = new float[9];
		matrix.getValues(mValues);
		
		float currentX = mValues[Matrix.MTRANS_X];
		float currentY = mValues[Matrix.MTRANS_Y];
		float adjustX = 0, adjustY = 0;
		
		
		float currentWidth  = (pictureWidth * mValues[Matrix.MSCALE_X]);
		float currentHeight = (pictureHeight * mValues[Matrix.MSCALE_Y]); 
		
		// initialPicX and initialPicY are alternative to 0,0 coordinate.. i.e.
		// starting X and Y.
		float maxX = initialPicX;
		float maxY = initialPicY;
		float minX = initialPicX;
		float minY = initialPicY;

		if (currentWidth < viewerWidth) {
			// image is smaller then screen
			// if screen size is 100 and image is 50, then image starting X
			// corner should be 25 to keep image in center
			// i.e. from 100 screen, image will lie between 26px to 75px ..
			// leaving 25px at both ends
			// same concept apply to height

			maxX = initialPicX + ((viewerWidth - currentWidth) / 2);
			minX = initialPicX + ((viewerWidth - currentWidth) / 2); // center
		} else {
			// image is larger then screen
			// if screen is 100 and image is 200, then the initial X of image
			// can go upto -100 to show complete image by draging.
			// if initial will goto -100, then the visible area of image on
			// screen will become 101 to 200
			// same concept apply to height
			minX = (initialPicX + viewerWidth - currentWidth);
		}

		if(currentHeight < viewerHeight){
			maxY = initialPicY + ((viewerHeight - currentHeight) /2);
			minY = initialPicY + ((viewerHeight - currentHeight) /2);
		}else {
			minY = (initialPicY + viewerHeight - currentHeight);
		}
		
		
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
		
		// Optimization, postTranslate only if we have some valid value to translate.
		if(adjustX != 0 || adjustY != 0){
			matrix.postTranslate(adjustX, adjustY);
		}
	}
	
	private void calculateRestrictedTranslations(float[] desiredTranslations){
		float[] mValues = new float[9];
		matrix.getValues(mValues);
		float currentWidth  = (pictureWidth * mValues[Matrix.MSCALE_X]);
		float currentHeight = (pictureHeight * mValues[Matrix.MSCALE_Y]); 
		
		
		// initialPicX and initialPicY are alternative to 0,0 coordinate.. i.e. starting X and Y.
		float maxX = initialPicX; 
		float maxY = initialPicY; 
		float minX = initialPicX;
		float minY = initialPicY;
		
		
		
		if(currentWidth < viewerWidth){
			// image is smaller then screen
			// if screen size is 100 and image is 50, then image starting X corner should be 25 to keep image in center
			// i.e. from 100 screen, image will lie between 26px to 75px .. leaving 25px at both ends
			// same concept apply to height
			
			maxX = initialPicX + ((viewerWidth - currentWidth) / 2);
			minX = initialPicX + ((viewerWidth - currentWidth) / 2); // center
		}
		else {
			// image is larger then screen 
			// if screen is 100 and image is 200, then the initial X of image can go upto -100 to show complete image by draging.
			// if initial will goto -100, then the visible area of image on screen will become 101 to 200 
			// same concept apply to height
			minX = (initialPicX + viewerWidth - currentWidth);
		}
		
		if(currentHeight < viewerHeight){
			maxY = initialPicY + ((viewerHeight - currentHeight) /2);
			minY = initialPicY + ((viewerHeight - currentHeight) /2);
		}else {
			minY = (initialPicY + viewerHeight - currentHeight);
		}
		
		
		
		
		// getting projected values, will check them if they comply with our boundries.. otherwise will adjust user's desired translation.
		float projectedX = mValues[Matrix.MTRANS_X] + desiredTranslations[KEY_TRANS_X];
		float projectedY = mValues[Matrix.MTRANS_Y] + desiredTranslations[KEY_TRANS_Y];
		
		// checking projected values with our boundries.. and adjusting if needed.
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
		
		// checking if projectedZoon is within our given limit. Otherwise adjust it
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
	
	private boolean isWithinBounds(MotionEvent e){
		boolean ret = false;
		float[] mValues = new float[9];
		matrix.getValues(mValues);
		float totalScale = mValues[Matrix.MSCALE_X];
		float x = (e.getRawX()-mValues[Matrix.MTRANS_X])/totalScale;
		float y = (e.getRawY()-mValues[Matrix.MTRANS_Y])/totalScale;
		
		Log.d("","objects="+objects);
		Log.d("","id_color_map="+id_color_map);
		String shop = getClickedShop(x, y);
		Log.d("","shop clicked="+shop);
		if(shop!=null){
			Toast.makeText(getContext(), shop, Toast.LENGTH_SHORT).show();;
			highlightObjProperties = objects.get(shop);
        	highlightType = (String) highlightObjProperties.get(PROP_KEY_TYPE);
        	Log.d("","shop clicked="+shop);
        	Log.d("","highlightType="+highlightType);
        	Log.d("","highlightObjProperties="+highlightObjProperties);
        	return true;
		}
		/*
		for(Entry<String, Properties> object : objects.entrySet()){
			Properties prop = object.getValue();
            String type =(String)prop.get("type");


			if(type.equals("rect")){
				Log.d("","RECT!");
				objX = (Float)prop.get("x");
				objY = (Float)prop.get("y");
				objWidth = (Float)prop.get("width");
				objHeight = (Float)prop.get("height");
		        if ((x > objX) && (x < (objX + objWidth))) {
		            if ((y > objY) && (y < (objY + objHeight))) {
		            	Toast.makeText(getContext(), object.getKey().toString(), Toast.LENGTH_SHORT).show();;
		            	highlightObjProperties = prop;
		            	highlightType = type;
		            	Log.d("","INSIDE RECT");
		                return true;
		            }
		        }
			}else if(type.equals("path")){
				Log.d("","PATH!");
				Path path = (Path) prop.get("path");
				
		        if(isContained(path, (int)x, (int)y)){
	            	Toast.makeText(getContext(), object.getKey().toString(), Toast.LENGTH_SHORT).show();;
	            	highlightObjProperties = prop;
	            	highlightType = type;
	            	Log.d("","INSIDE PATH");
	            	return true;
		        }
				

			}else{
				highlightType = "";
			}

		}
		*/
		return ret;
	}
/*
	private boolean isContained(Path mPath, int x, int y){
		Paint cPaint = new Paint();

		cPaint.setAntiAlias(false);
		cPaint.setColor(0xFF000000);
		cPaint.setStyle(Paint.Style.FILL);
		Bitmap topBitmap = Bitmap.createBitmap(pictureWidth, pictureHeight, Bitmap.Config.ARGB_8888);
		Canvas topCanvas = new Canvas(topBitmap);
		topCanvas.drawColor(0xFFFFFFFF, Mode.CLEAR);
		topCanvas.drawPath(mPath, cPaint);
		if (topBitmap.getPixel(x, y) == 0xFF000000) {
			return true;
		}

		return false;

	}
	*/
	Canvas topCanvas;
	Bitmap topBitmap;
	private void createTopMapCanvas(){
		
		topBitmap = Bitmap.createBitmap(pictureWidth, pictureHeight, Bitmap.Config.ARGB_8888);
		id_color_map = new HashMap<String, Integer>();
		
	    int color = 0xFF000000;
		Paint cPaint = new Paint();
		cPaint.setAntiAlias(false);
		cPaint.setStyle(Paint.Style.FILL);
		topCanvas = new Canvas(topBitmap);
		topCanvas.drawColor(0xFFFFFFFF, Mode.CLEAR);

		for(Entry<String, Properties> object : objects.entrySet()){
			Properties prop = object.getValue();
            String type =(String)prop.get(PROP_KEY_TYPE);
            
            if(type.equals(VAL_TYPE_PATH)){
            	Path path = (Path) prop.get(PROP_KEY_PATH_OBJ);
        		cPaint.setColor(color);
        		topCanvas.drawPath(path, cPaint);
        		id_color_map.put(object.getKey(), color);
        		color++;
            }else if(type.equals(VAL_TYPE_RECT)){
				float x = (Float)prop.get(PROP_KEY_X);
				float y = (Float)prop.get(PROP_KEY_Y);
				float width = (Float)prop.get(PROP_KEY_WIDTH);
				float height = (Float)prop.get(PROP_KEY_HEIGHT);
				cPaint.setColor(color);
				topCanvas.drawRect(x, y, x + width, y + height, cPaint);
				id_color_map.put(object.getKey(), color);
				color++;
            }
		}
	}
	
	private String getClickedShop( float x, float y){
		for(Entry<String, Integer> id : id_color_map.entrySet()){
			Log.d("","pixel color="+topBitmap.getPixel((int)x, (int)y) );
			Log.d("","id="+id.getKey()+", id color value="+id.getValue());
			if (topBitmap.getPixel((int)x, (int)y) == (int)id.getValue()) {
				return id.getKey();
			}
		}
		return null;
	}
}
