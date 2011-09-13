package android.lokemon;

import android.app.ListActivity;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;

public class FadePopup extends ListActivity {
	
	protected TransitionDrawable background;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup);
		background = (TransitionDrawable)(findViewById(R.id.list_parent).getBackground());
	}
	
	public void onResume()
	{
		super.onResume();
		background.startTransition(500);
	}
	
	/*public void onPause()
	{
		super.onPause();
		background.reverseTransition(500);
	}*/
}
