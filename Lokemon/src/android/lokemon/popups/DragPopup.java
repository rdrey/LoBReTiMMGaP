package android.lokemon.popups;

import android.app.ListActivity;
import android.graphics.drawable.TransitionDrawable;
import android.lokemon.R;
import android.os.Bundle;

public class DragPopup extends ListActivity {
	
	protected TransitionDrawable background;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_popup_rearrange);
		background = (TransitionDrawable)(findViewById(R.id.list_parent).getBackground());
	}
	
	public void onResume()
	{
		super.onResume();
		background.startTransition(500);
	}
	
	public void onBackPressed()
	{
		this.setResult(RESULT_CANCELED);
		super.onBackPressed();
	}
}
