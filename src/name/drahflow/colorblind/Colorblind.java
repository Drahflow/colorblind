package name.drahflow.colorblind;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

public class Colorblind extends Activity
{
	private MainSurface main;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		main = new MainSurface(this);
		setContentView(main.preview);
		addContentView(main,
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
	}

	@Override
	public void onPause() {
		super.onPause();

		main.stopActive();

		finish();
	}

	@Override
	public void onResume() {
		super.onResume();

		main.startActive();
	}
}
