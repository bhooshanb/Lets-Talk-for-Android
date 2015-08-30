package in.silverstonelabs.letstalk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class HelpActivity extends AppCompatActivity {

	static WebView mWebView;
	static WebSettings webSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		// getSupportActionBar().hide();
		mWebView = (WebView) findViewById(R.id.helpWebView);
		webSettings = mWebView.getSettings();
		webSettings.setCacheMode(1);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setAppCacheEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webSettings.setSupportMultipleWindows(false);
		mWebView.setWebChromeClient(new WebChromeClient() {
		});
		mWebView.loadUrl("http://silverstonelabs.in/letstalk/android_asset/help/index.html");
	}

}
