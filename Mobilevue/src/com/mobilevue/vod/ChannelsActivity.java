package com.mobilevue.vod;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mobilevue.adapter.ChannelGridViewAdapter;
import com.mobilevue.data.ChannelsDatum;
import com.mobilevue.data.ServiceDatum;
import com.mobilevue.database.DBHelper;
import com.mobilevue.database.ServiceProvider;
import com.mobilevue.retrofit.OBSClient;
import com.mobilevue.service.DoBGTasksService;
import com.mobilevue.vod.MyApplication.SetAppState;
import com.mobilevue.vod.MyApplication.SortBy;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ChannelsActivity extends Activity implements
		LoaderCallbacks<Cursor> {

	// private final String TAG = ChannelsActivity.this.getClass().getName();
	private ProgressDialog mProgressDialog;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	boolean mIsLiveDataReq = false;
	String mSearchString;
	String mSelection;
	String[] mSelectionArgs;
	boolean mIsRefresh = false;

	public static int mSortBy = SortBy.CATEGORY.ordinal();

	private ArrayList<String> groupsList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channels);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();
		mSearchString = null;

		getServices();
	}

	@Override
	protected void onResume() {
		mIsRefresh = false;
		getServices();
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Log.d("ChannelsActivity", "onNewIntent");
		if (null != intent && null != intent.getAction()
				&& intent.getAction().equals(Intent.ACTION_SEARCH)) {

			// initiallizing req criteria
			mSearchString = intent.getStringExtra(SearchManager.QUERY);
			mSelection = DBHelper.CHANNEL_DESC + " LIKE ?";
			mSelectionArgs = new String[] { "%" + mSearchString + "%" };
			getServices();
		}
	}

	private void getServices() {
		Loader<Cursor> loader = getLoaderManager().getLoader(mSortBy);
		if (loader != null && !loader.isReset()) {
			getLoaderManager().restartLoader(mSortBy, null, this);
		} else {
			getLoaderManager().initLoader(mSortBy, null, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchItem.setVisible(true);
		MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.setVisible(true);
		MenuItem sortByItem = menu.findItem(R.id.action_sort_by);
		sortByItem.setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		mSelection = null;
		mSearchString = null;
		mSelectionArgs = null;
		mIsRefresh = false;

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		case R.id.action_refresh:
			mIsRefresh = true;
			getServices();
			break;
		case R.id.action_search:
			onSearchRequested();
			break;
		case R.id.action_sort_by_default:
			mSortBy = SortBy.DEFAULT.ordinal();
			getServices();
			break;
		case R.id.action_sort_by_categ:
			mSortBy = SortBy.CATEGORY.ordinal();
			getServices();
			break;
		case R.id.action_sort_by_lang:
			mSortBy = SortBy.LANGUAGE.ordinal();
			getServices();
			break;
		case R.id.action_logout:
			logout();
			break;
		default:
			break;
		}
		return true;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mProgressDialog = null;

			}
		});
		mProgressDialog.show();

		// using sortOrder arg for passing both mIsRefresh&SortOrder
		String sortOrder = mIsRefresh + "&";
		// This is called when a new Loader needs to be created.
		CursorLoader loader = null;
		if (id == SortBy.CATEGORY.ordinal()) {
			// group cursor
			loader = new CursorLoader(this,
					ServiceProvider.SERVICE_CATEGORIES_URI, null, null, null,
					sortOrder);
		} else if (id == SortBy.LANGUAGE.ordinal()) {
			// group cursor
			loader = new CursorLoader(this,
					ServiceProvider.SERVICE_SUB_CATEGORIES_URI, null, null,
					null, sortOrder);
		} else if (id == SortBy.DEFAULT.ordinal()) {
			// group cursor
			loader = new CursorLoader(this, ServiceProvider.SERVICES_URI, null,
					null, null, sortOrder);
		}
		return loader;
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// Log.d("ChannelsActivity","onLoadFinished");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		if (mSortBy == SortBy.CATEGORY.ordinal()
				|| mSortBy == SortBy.LANGUAGE.ordinal()) {
			groupsList.clear();
			if (cursor != null) {
				cursor.moveToFirst();
				do {
					if (mSortBy == SortBy.CATEGORY.ordinal())
						groupsList.add(cursor.getString(cursor
								.getColumnIndex(DBHelper.CATEGORY)));
					else
						groupsList.add(cursor.getString(cursor
								.getColumnIndex(DBHelper.SUB_CATEGORY)));
				} while (cursor.moveToNext());

				if (groupsList.size() > 0) {
					InitializeUI(cursor);
				}
			}
		} else if (loader.getId() == SortBy.DEFAULT.ordinal()
				&& mSortBy == SortBy.DEFAULT.ordinal()) {
			int svcIdIdx = cursor.getColumnIndexOrThrow(DBHelper.SERVICE_ID);
			// int cIdIdx = cursor.getColumnIndexOrThrow(DBHelper.CLIENT_ID);
			int chNameIdx = cursor.getColumnIndexOrThrow(DBHelper.CHANNEL_NAME);
			int chDescIdx = cursor.getColumnIndexOrThrow(DBHelper.CHANNEL_DESC);
			int imgIdx = cursor.getColumnIndexOrThrow(DBHelper.IMAGE);
			int urlIdx = cursor.getColumnIndexOrThrow(DBHelper.URL);
			List<ServiceDatum> serviceList = new ArrayList<ServiceDatum>();
			while (cursor.moveToNext()) {
				ServiceDatum service = new ServiceDatum();
				service.setServiceId(Integer.parseInt(cursor
						.getString(svcIdIdx)));
				service.setClientId(Integer.parseInt(cursor.getString(svcIdIdx)));
				service.setChannelName(cursor.getString(chNameIdx));
				service.setChannelDescription(cursor.getString(chDescIdx));
				service.setImage(cursor.getString(imgIdx));
				service.setUrl(cursor.getString(urlIdx));
				serviceList.add(service);
			}
			if (serviceList.size() > 0) {
				updateChannelsForZapping(serviceList);
				updateChannels(serviceList);
			}
		}
	}

	private void updateChannels(final List<ServiceDatum> list) {
		if (list != null) {
			LayoutInflater inflater = LayoutInflater.from(this);

			final GridView gridView = (GridView) inflater.inflate(
					R.layout.a_ch_gridview, null);
			gridView.setAdapter(new ChannelGridViewAdapter(list,
					ChannelsActivity.this));
			gridView.setDrawSelectorOnTop(true);
			gridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View imageVw,
						int position, long arg3) {
					ServiceDatum service = list.get(position);
					mApplication.getEditor().putString(
							IPTVActivity.CHANNEL_DESC,
							service.getChannelDescription());
					mApplication.getEditor().putString(
							IPTVActivity.CHANNEL_URL, service.getUrl());
					mApplication.getEditor().commit();
					startActivity(new Intent(ChannelsActivity.this,
							IPTVActivity.class));
				}
			});
			LinearLayout layout = (LinearLayout) findViewById(R.id.a_ch_parent_layout);
			layout.removeAllViews();
			layout.addView(gridView, new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}

	private void InitializeUI(Cursor cursor) {

		// retrive the services based on category
		// need to create a xml file containing header and HSView
		// fill the data header with group name & HSView with services
		// add that layout to container
		try {
			if (cursor.getCount() > 0) {
				List<ChannelsDatum> channelsList = new ArrayList<ChannelsDatum>();
				LayoutInflater inflater = LayoutInflater.from(this);
				ScrollView scrollView = (ScrollView) inflater.inflate(
						R.layout.a_ch_scrollview, null);

				cursor.moveToFirst();
				LinearLayout parent = (LinearLayout) scrollView
						.findViewById(R.id.a_ch_ll_container);
				String selectionArgs = null;
				if (mSelectionArgs != null && mSelectionArgs.length > 0) {
					selectionArgs = mSelectionArgs[0];
				}

				do {

					CursorLoader cursorLoader = null;
					String groupName = null;
					View groupView = inflater.inflate(R.layout.ch_group_item,
							null);
					TextView groupNameTv = (TextView) groupView
							.findViewById(R.id.a_ch_group_name);

					boolean isChildExist = false;

					if (mSortBy == SortBy.CATEGORY.ordinal()) {
						groupName = cursor.getString(cursor
								.getColumnIndex(DBHelper.CATEGORY));
						if (mSelection != null && selectionArgs != null) {
							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.CATEGORY + "=? AND " + mSelection,
									new String[] { groupName, selectionArgs },
									null);
						} else if (mSelection != null && selectionArgs == null) {

							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.CATEGORY + "=? AND " + mSelection,
									new String[] { groupName }, null);
						} else if (mSelection == null && selectionArgs == null) {
							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.CATEGORY + "=?",
									new String[] { groupName }, null);
						}
					} else {
						groupName = cursor.getString(cursor
								.getColumnIndex(DBHelper.SUB_CATEGORY));
						if (mSelection != null && selectionArgs != null) {
							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.SUB_CATEGORY + "=? AND "
											+ mSelection, new String[] {
											groupName, selectionArgs }, null);
						} else if (mSelection != null && selectionArgs == null) {

							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.SUB_CATEGORY + "=? AND "
											+ mSelection,
									new String[] { groupName }, null);
						} else if (mSelection == null && selectionArgs == null) {
							cursorLoader = new CursorLoader(this,
									ServiceProvider.SERVICES_URI, null,
									DBHelper.SUB_CATEGORY + "=?",
									new String[] { groupName }, null);
						}
					}
					Cursor childCursor = null;

					childCursor = cursorLoader.loadInBackground();

					if (childCursor.getCount() > 0) {
						groupNameTv.setText(groupName);
						isChildExist = true;
						childCursor.moveToFirst();
						ImageLoader imgLoader = com.nostra13.universalimageloader.core.ImageLoader
								.getInstance();
						int imgSize = getResources().getInteger(
								R.integer.ch_img_size);
						LayoutParams params = new LayoutParams(imgSize, imgSize);
						do {
							int svcIdIdx = childCursor
									.getColumnIndexOrThrow(DBHelper.SERVICE_ID);
							int chDescIdx = childCursor
									.getColumnIndexOrThrow(DBHelper.CHANNEL_DESC);
							int imgIdx = childCursor
									.getColumnIndexOrThrow(DBHelper.IMAGE);
							int urlIdx = childCursor
									.getColumnIndexOrThrow(DBHelper.URL);

							ChannelsDatum channel = new ChannelsDatum();
							channel.serviceId = Integer.parseInt(childCursor
									.getString(svcIdIdx));
							channel.channelDescription = childCursor
									.getString(chDescIdx);
							channel.image = childCursor.getString(imgIdx);
							channel.url = childCursor.getString(urlIdx);
							channelsList.add(channel);
							ImageView iv = (ImageView) inflater.inflate(
									R.layout.ch_gridview_item, null);
							iv.setLayoutParams(params);
							imgLoader.displayImage(channel.image, iv);
							ChannelTag tag = new ChannelTag();
							tag.desc = channel.channelDescription;
							tag.url = channel.url;
							iv.setTag(tag);
							iv.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									ChannelTag chTag = (ChannelTag) v.getTag();
									mApplication.getEditor().putString(
											IPTVActivity.CHANNEL_DESC,
											chTag.desc);
									mApplication.getEditor()
											.putString(
													IPTVActivity.CHANNEL_URL,
													chTag.url);
									mApplication.getEditor().commit();
									startActivity(new Intent(
											ChannelsActivity.this,
											IPTVActivity.class));
								}
							});

							LinearLayout channels = (LinearLayout) groupView
									.findViewById(R.id.a_ch_group_container);
							channels.addView(iv);
						} while (childCursor.moveToNext());
					}
					if (isChildExist) {
						parent.addView(groupView);
						childCursor.close();
					}
				} while (cursor.moveToNext());
				LinearLayout layout = (LinearLayout) findViewById(R.id.a_ch_parent_layout);
				layout.removeAllViews();
				layout.addView(scrollView, new LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				if (channelsList.size() > 0)
					updateChannelsForZapping(channelsList);
			}
		} catch (Exception e) {
			Log.e("ChannelsActivity", "Cursor Exception");
		}
		cursor.close();
	}

	private void updateChannelsForZapping(List list) {
		Object obj = list.get(0);
		List<ChannelsDatum> channelsList = null;
		if (obj instanceof ServiceDatum) {
			List<ServiceDatum> serviceList = list;
			channelsList = new ArrayList<ChannelsDatum>();
			ChannelsDatum cdata = null;
			for (ServiceDatum sdata : serviceList) {
				cdata = new ChannelsDatum();
				cdata.serviceId = sdata.getServiceId();
				cdata.channelDescription = sdata.getChannelDescription();
				cdata.url = sdata.getUrl();
				cdata.image = sdata.getImage();
				channelsList.add(cdata);
			}
		} else if (obj instanceof ChannelsDatum) {
			channelsList = list;
		}
		if (channelsList.size() > 0) {
			String sJsonChannels = new Gson().toJson(channelsList);
			// update channels data in prefs
			mApplication.getEditor().putString(
					getResources().getString(R.string.channels_list),
					sJsonChannels);
			mApplication.getEditor().commit();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onDestroy() {
		// Log.e("ChannelsActivity-onDestroy","clearing the channelsList");
		mApplication.getEditor().remove(
				getResources().getString(R.string.channels_list));
		mApplication.getEditor().commit();
		super.onDestroy();
	}

	public void logout() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this,
				AlertDialog.THEME_HOLO_LIGHT);
		builder.setIcon(R.drawable.ic_logo_confirm_dialog);
		builder.setTitle("Confirmation");
		builder.setMessage("Are you sure to Logout?");
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
					}
				});
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						if (MyApplication.isActive) {
							Intent intent = new Intent(ChannelsActivity.this,
									DoBGTasksService.class);
							intent.putExtra(DoBGTasksService.App_State_Req,
									SetAppState.SET_INACTIVE.ordinal());
							intent.putExtra("CLIENTID",
									((MyApplication) getApplicationContext())
											.getClientId());
							startService(intent);
						}
						// Clear shared preferences..
						((MyApplication) getApplicationContext()).clearAll();
						// close all activities..
						Intent Closeintent = new Intent(ChannelsActivity.this,
								MainActivity.class);
						// set the new task and clear flags
						Closeintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TOP);
						Closeintent.putExtra("LOGOUT", true);
						startActivity(Closeintent);
						finish();
					}
				});
		dialog.show();

	}

	public class ChannelTag {
		String desc;
		String url;
	}
}