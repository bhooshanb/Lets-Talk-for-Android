package in.silverstonelabs.letstalk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Main extends AppCompatActivity {

	public static int OVERLAY_PERMISSION_REQ_CODE = 1111;
	static Main main;
	boolean isDrawOkay = false;
	String name = "MainActivity";
	Tracker mTracker;
	Switch toggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().setIcon(R.mipmap.ic_launcher);
		main = Main.this;
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();

		isDrawOkay = sharedPref.getBoolean("isdrawokay", false);

		if (Build.VERSION.SDK_INT == 23) {
			if (!Settings.canDrawOverlays(this)) {
				isDrawOkay = false;
				editor.putBoolean("isdrawokay", isDrawOkay);
				Toast.makeText(Main.this, "Check App Permission Settings", Toast.LENGTH_LONG).show();
			} else {
				isDrawOkay = true;
				editor.putBoolean("isdrawokay", isDrawOkay);
			}
			editor.commit();
		}

		if (!isDrawOkay) {

		} else {
			AnalyticsApplication application = (AnalyticsApplication) getApplication();
			mTracker = application.getDefaultTracker();
			buildAnalytics();
			if (isMyServiceRunning()) {
				stopService(new Intent(main, ChatHeadService.class));
				startService(new Intent(main, ChatHeadService.class));
				callAnalytics();
				Toast.makeText(Main.this, "Reinitialized! Look at the Top-Left Corner!", Toast.LENGTH_SHORT).show();
				finish();
			} else {
				startService(new Intent(main, ChatHeadService.class));
				callAnalytics();
				finish();
			}
		}

		toggle = (Switch) findViewById(R.id.toggleButton);
		toggle.setChecked(isMyServiceRunning());
		toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				try {
					if (isChecked) {
						startService(new Intent(main, ChatHeadService.class));
						Main.this.finish();
					} else {
						stopService(new Intent(main, ChatHeadService.class));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (ChatHeadService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void onLogoClick(View view) {
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=6816054042586511054")));
		} catch (android.content.ActivityNotFoundException anfe) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=6816054042586511054")));
		}
	}

	public void callAnalytics() {
		mTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Action")
				.setAction("Share")
				.build());
	}

	public void buildAnalytics() {
		Log.i("LT-Main", "A: " + name);
		mTracker.setScreenName("A~" + name);
		mTracker.send(new HitBuilders.ScreenViewBuilder().build());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= 23) {
			getMenuInflater().inflate(R.menu.menu_main, menu);
		} else {
			getMenuInflater().inflate(R.menu.menu_help, menu);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
			Main.this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void isDrawOverAppsEnabled() {
		if (Build.VERSION.SDK_INT == 23) {
			if (!Settings.canDrawOverlays(this)) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
						Uri.parse("package:" + getPackageName()));
				try {
					startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
				} catch (Exception e) {
					Toast.makeText(Main.this, "Something Went Wrong! Open Permission Manager & Grant permissions!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
			if (!Settings.canDrawOverlays(this)) {
				isDrawOkay = false;
				editor.putBoolean("isdrawokay", isDrawOkay);
				Toast.makeText(Main.this, "Check App Permission Settings", Toast.LENGTH_LONG).show();
			} else {
				isDrawOkay = true;
				editor.putBoolean("isdrawokay", isDrawOkay);
			}
			editor.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_permission_m_settings) {
			isDrawOverAppsEnabled();
			return true;
		}
		if(id== R.id.help_menu) {
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}