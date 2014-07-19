package com.mobilevue.vod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.mobilevue.adapter.MyAccountMenuAdapter;
import com.mobilevue.service.DoBGTasksService;
import com.mobilevue.vod.MyApplication.SetAppState;

public class MyAccountActivity extends Activity {

	// private static final String TAG = MyAccountActivity.class.getName();
	ListView listView;
	private static final String FRAG_TAG = "My Fragment";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_account);
		listView = (ListView) findViewById(R.id.a_my_acc_lv_menu);
		MyAccountMenuAdapter menuAdapter = new MyAccountMenuAdapter(this);
		listView.setAdapter(menuAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemChecked(0, true);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:
					Fragment myPackageFrag = new MyPakagesFragment();
					FragmentTransaction transaction1 = getFragmentManager()
							.beginTransaction();
					transaction1.replace(R.id.a_my_acc_frag_container,
							myPackageFrag, FRAG_TAG);
					transaction1.commit();
					break;

				case 1:
					Fragment myProfileFrag = new MyProfileFragment();
					FragmentTransaction transaction2 = getFragmentManager()
							.beginTransaction();
					transaction2.replace(R.id.a_my_acc_frag_container,
							myProfileFrag, FRAG_TAG);
					transaction2.commit();
					break;
				}
			}
		});
		Fragment myPackageFrag = new MyPakagesFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.add(R.id.a_my_acc_frag_container, myPackageFrag, FRAG_TAG);
		transaction.commit();
	}

	public void btnSubmit_onClick(View v) {
		Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
		if (frag instanceof MyPakagesFragment) {
			((MyPakagesFragment) frag).btnSubmit_onClick(v);
		}

	}

	@Override
	public void onBackPressed() {
		Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
		if (frag instanceof MyPakagesFragment) {
			((MyPakagesFragment) frag).onBackPressed();
		} else
			super.onBackPressed();
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
							Intent intent = new Intent(MyAccountActivity.this,
									DoBGTasksService.class);
							intent.putExtra("CLIENTID",
									((MyApplication) getApplicationContext())
											.getClientId());
							intent.putExtra(DoBGTasksService.App_State_Req,
									SetAppState.SET_INACTIVE.ordinal());
							startService(intent);
						}
						// Clear shared preferences..
						((MyApplication) getApplicationContext()).clearAll();
						// close all activities..
						Intent Closeintent = new Intent(MyAccountActivity.this,
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

}