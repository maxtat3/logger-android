package edu.sintez.loggermobile.app.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.sintez.loggermobile.app.R;
import edu.sintez.loggermobile.app.model.Setting;

public class SettingsActivity extends Activity {

	/**
	 * Address BT module in MCU device.
	 */
	private EditText etAddress;
	private Button btnSaveSettings;
	private Button btnCancelSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		etAddress = (EditText) findViewById(R.id.et_address);
		btnSaveSettings = (Button) findViewById(R.id.btn_save_settings);
		btnCancelSettings = (Button) findViewById(R.id.btn_cancel_settings);

		btnSaveSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (BluetoothAdapter.checkBluetoothAddress(etAddress.getText().toString())) {
					Setting settings = getSettings();
					settings.setAddress(etAddress.getText().toString());
					saveSettings(settings);
					Toast.makeText(getApplicationContext(), "Settings saved", Toast.LENGTH_SHORT).show();
					finish();
				} else {
					Toast.makeText(getApplicationContext(), "Incorrect address format !", Toast.LENGTH_LONG).show();
				}
			}
		});

		btnCancelSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		Setting settings = getSettings();
		etAddress.setText(settings.getAddress());
	}

	private Setting getSettings() {
		SharedPreferences prefs = getSharedPreferences(Setting.SETTINGS_XML_FILE_NAME, MODE_PRIVATE);
		String address = prefs.getString(Setting.BT_MCU_DEVICE_ADDRESS_KEY, Setting.BT_MCU_DEVICE_DEFAULT_ADDRESS);
		return new Setting(address);
	}

	private void saveSettings(Setting setting) {
		SharedPreferences prefs = getSharedPreferences(Setting.SETTINGS_XML_FILE_NAME, MODE_PRIVATE);
		prefs.edit().putString(Setting.BT_MCU_DEVICE_ADDRESS_KEY, setting.getAddress()).apply();
	}
}
