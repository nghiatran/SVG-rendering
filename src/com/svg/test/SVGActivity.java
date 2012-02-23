package com.svg.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class SVGActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView imageView =new MyImageView(this);
         setContentView(imageView);
        
      //  setContentView(R.layout.main);
       // ViewGroup container = (ViewGroup) findViewById(R.id.container);
       // container.addView(imageView);

       

    }
}