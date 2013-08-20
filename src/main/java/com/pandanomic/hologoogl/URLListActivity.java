package com.pandanomic.hologoogl;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


/**
 * An activity representing a list of URLs. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link URLDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link URLListFragment} and the item details
 * (if present) is a {@link URLDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link URLListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class URLListActivity extends FragmentActivity
        implements URLListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_list);

        if (findViewById(R.id.url_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((URLListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.url_list))
                    .setActivateOnItemClick(true);
        }

//        findViewById(R.id.shorten_new_URL).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                newURLDialog();
//            }
//        });

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.shorten_new_URL:
                newURLDialog();
                return true;
            case R.id.refresh_url_list:
                return true;
            case R.id.login:
                return true;
            case R.id.action_settings:
                return true;
            case R.id.send_feedback:
                sendFeedback();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.urllist_menu, menu);
		return true;
	}

    /**
     * Callback method from {@link URLListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(URLDetailFragment.ARG_ITEM_ID, id);
            URLDetailFragment fragment = new URLDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.url_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, URLDetailActivity.class);
            detailIntent.putExtra(URLDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

	public void accountSetup() {
		AccountManager am = AccountManager.get(this);

		Account[] accounts = am.getAccountsByType("com.google");
	}

    private void newURLDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Shorten New URL");
        alert.setCancelable(true);

        final EditText input = new EditText(this);
        input.setHint("Type or paste a URL here");
        alert.setView(input);
        alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String urlToShare = input.getText().toString();

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                generateShortenedURL(urlToShare);

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alert.show();
    }

    private void generateShortenedURL(String input) {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            Toast.makeText(this, "Check your internet connection", Toast.LENGTH_LONG).show();
            return;
        }
        URLShortener shortener = new URLShortener();
        Log.d("hologoogl", "generating");
        final String resultURL = shortener.generate(input);
        Log.d("hologoogl", "done generating");

        Log.d("hologoogl", "Generated " + resultURL);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(resultURL)
                .setCancelable(true)
                .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        shareURL(resultURL);
                    }
        });

        alert.setNegativeButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                copyURL(resultURL);
            }
        });

        alert.show();
    }

    private void copyURL(String input) {
        ClipboardManager clipboard = (ClipboardManager)
                getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Shortened URL", input);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getBaseContext(), "Copied!", Toast.LENGTH_SHORT).show();
    }

    private void shareURL(String input) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, input);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Shared from Holo Goo.gl");
        startActivity(Intent.createChooser(intent, "Share"));
    }

    private void sendFeedback() {
        Intent gmail = new Intent(Intent.ACTION_VIEW);
        gmail.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        gmail.putExtra(Intent.EXTRA_EMAIL, new String[] { "pandanomic@gmail.com" });
        gmail.setData(Uri.parse("pandanomic@gmail.com"));
        gmail.putExtra(Intent.EXTRA_SUBJECT, "Holo Goo.gl Feedback");
        gmail.setType("plain/text");
        startActivity(gmail);
    }
}
