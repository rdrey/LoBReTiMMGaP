package android.lokemon.test;

import com.example.android.apis.graphics.AnimateDrawable;

import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.lokemon.R;
import android.os.*;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;

public class AnimationTest extends Activity {

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(new SampleView(this));
	}
	
	private static class SampleView extends View {
        private AnimateDrawable mDrawable;

        public SampleView(Context context) {
            super(context);
            setFocusable(true);
            setFocusableInTouchMode(true);

            BitmapDrawable dr = (BitmapDrawable)context.getResources().getDrawable(R.drawable.aura);
            dr.setAntiAlias(true);
            dr.setDither(true);
            //dr.setBounds(0,0, dr.getIntrinsicWidth()/, dr.getIntrinsicHeight()/);
            dr.setBounds(dr.getIntrinsicWidth()/-2, dr.getIntrinsicHeight()/-2, dr.getIntrinsicWidth()/2, dr.getIntrinsicHeight()/2);
            
            //RotateAnimation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF, 0.5f);
            RotateAnimation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,0.0f,Animation.RELATIVE_TO_SELF, 0.0f);
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(3000);
            anim.setRepeatCount(Animation.INFINITE);
            anim.setRepeatMode(Animation.RESTART);
            anim.initialize(dr.getIntrinsicWidth(), dr.getIntrinsicHeight(), 0, 0);
            
            mDrawable = new AnimateDrawable(dr, anim);
            anim.startNow();
        }
        
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);

            mDrawable.draw(canvas);
            invalidate();
        }
    }
}
