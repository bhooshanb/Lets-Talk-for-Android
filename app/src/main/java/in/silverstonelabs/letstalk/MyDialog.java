package in.silverstonelabs.letstalk;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.markushi.ui.CircleButton;

public class MyDialog extends AppCompatActivity implements View.OnClickListener {

	static final int check = 1112;
	public static boolean active = false;
	public static Activity myDialog;
	public LinearLayout dialogView;
	ListView lv;
	TextView tv;
	ArrayList<String> results;
	String name = "MyDialogActivity";
	Tracker mTracker;
	String newPackageName = null, launcherPackage, applicationName, activePackage;
	AdView adView;
	AdRequest adRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog);
		active = true;
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		myDialog = MyDialog.this;
		dialogView = (LinearLayout) findViewById(R.id.dialog_main_view);

		AnalyticsApplication application = (AnalyticsApplication) getApplication();
		mTracker = application.getDefaultTracker();

		launcherPackage = findLauncherPackageName();
		buildAnalytics();

		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.alpha = 1.0f;
		params.dimAmount = 0.1f;
		getWindow().setAttributes(params);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		getNewPackages();
		getSimpleAppName();

		if (height > width) {
			getWindow().setLayout((int) (width * .9), (int) (height * .8));
		} else {
			getWindow().setLayout((int) (width * .7), (int) (height * .8));
		}

		lv = (ListView) findViewById(R.id.lvVoiceReturn);
		at.markushi.ui.CircleButton b = (CircleButton) findViewById(R.id.bVoice);

		b.setOnClickListener(this);

		// Long Click //

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				shareText(results.get(position));
				return true;
			}
		});

		// Single Click //

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, results.get(position));
				sendIntent.setType("text/plain");
				activePackage = newPackageName;
				if (activePackage.contentEquals(launcherPackage)) {
					shareText(results.get(position));
				} else {
					sendIntent.setPackage(activePackage);
					try {
						callAnalytics();
						startActivity(sendIntent);
					} catch (Exception e) {
						Toast.makeText(MyDialog.this, "Current App doesn't accept Text, Try Long Press for more apps", Toast.LENGTH_LONG).show();
					}
				}
			}
		});

		adView = (AdView) findViewById(R.id.adView);
		adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);
		startListening();
	}

	public void shareText(String txt) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, txt);
		sendIntent.setType("text/plain");
		callAnalytics();
		startActivity(sendIntent);
	}

	public void getNewPackages() {
		String newp0 = "A";
		ActivityManager mActivityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		if (Build.VERSION.SDK_INT >= 21) {
			final Set<String> activePackages = new HashSet<>();
			final List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
			for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
				if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
					activePackages.addAll(Arrays.asList(processInfo.pkgList));
				}
			}
			//activePackages.remove("com.google.process.gapps");
			//activePackages.remove("com.google.android.gms");
			//activePackages.remove("com.google.android.googlequicksearchbox:search");
			newPackageName = processInfos.get(1).processName;
		} else {
			ActivityManager.RunningTaskInfo foregroundTaskInfo = mActivityManager.getRunningTasks(2).get(1);
			newPackageName = foregroundTaskInfo.topActivity.getPackageName();
		}
		Toast.makeText(this, newPackageName, Toast.LENGTH_SHORT).show();
	}


	public void getSimpleAppName() {
		final PackageManager pm = getApplicationContext().getPackageManager();
		ApplicationInfo ai;
		try {
			ai = pm.getApplicationInfo(newPackageName, 0);
		} catch (final PackageManager.NameNotFoundException e) {
			ai = null;
		}
		applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(Unknown)");

	}

	// Clear Button //
	public void onClearClick(View view) {
		if (lv.getChildCount() != 0) {
			lv.setAdapter(null);
			tv.setVisibility(View.INVISIBLE);
		}
		adView.loadAd(adRequest);
		getNewPackages();
	}

	public void startListening() {
		Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		i.putExtra(RecognizerIntent.EXTRA_PROMPT, "What's Happening?");
		try {
			callAnalytics();
			startActivityForResult(i, check);
		} catch (Exception e) {
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setMessage("Google Search App is not installed. Let me take you on PlayStore to download?");
			builder1.setCancelable(true);
			builder1.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							try {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.googlequicksearchbox")));
							} catch (Exception e) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox")));
							}
						}
					});
			builder1.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});

			AlertDialog alert11 = builder1.create();
			alert11.show();
		}
	}

	// Record Button
	public void onClick(View v) {
		startListening();
	}

	// Logo Click //
	public void ssOnClick(View view) {
		try {
			callAnalytics();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=6816054042586511054")));
		} catch (android.content.ActivityNotFoundException anfe) {
			callAnalytics();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=6816054042586511054")));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == check && resultCode == RESULT_OK) {
			results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results));
			tv = (TextView) findViewById(R.id.text_info_label);
			if (launcherPackage.contentEquals(newPackageName)) {
				tv.setText("One-Touch to Open Share option");
			} else {
				tv.setText("One-Touch to Send Message to " + applicationName + ", Long Press for More.");
			}
			tv.setVisibility(View.VISIBLE);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private String findLauncherPackageName() {
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo res = getPackageManager().resolveActivity(intent, 0);
		return res.activityInfo.packageName;
	}

	@Nullable
	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		active = true;
		return super.onCreateView(name, context, attrs);
	}

	@Override
	protected void onResume() {
		super.onResume();
		active = true;
		adView.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		active = false;
		adView.pause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		active = false;
		adView.destroy();
	}

	public void callAnalytics() {
		mTracker.send(new HitBuilders.EventBuilder()
				.setCategory("Action")
				.setAction("Share")
				.build());
	}

	public void buildAnalytics() {
		Log.i("LT-Dialog", "A: " + name);
		mTracker.setScreenName("A~" + name);
		mTracker.send(new HitBuilders.ScreenViewBuilder().build());
	}
}