package edu.sintez.loggermobile.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements OnChartValueSelectedListener {

	private static final String LOG = MainActivity.class.getName();
	private boolean isLog = true;

	/**
	 * Service SPP UUID
	 */
	private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * Request to enable BT module in android device if he is turn off.
	 */
	private static final int REQUEST_ENABLE_BT = 1;

	/**
	 * Receive BT data from mcu device.
	 */
	private static final int RECEIVE_BT_DATA = 1;

	/**
	 * Receive massage from BT in {@link ConnectedThread}.
	 * This massage handled in BT handler {@link #btHandler}.
	 */
	public static final int RECEIVE_MSG = 1;

	/**
	 * Max amount of channels in measure process involved.
	 */
	private static final int MAX_CHANNELS = 4;

	/**
	 * Dynamic line chart object.
	 */
	private LineChart lineChart;

	/**
	 * Record menu item.
	 */
	private MenuItem miRecord;

	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private ConnectedThread connectedThread;

	private Handler chartHandler;
	private Handler btHandler;

	/**
	 * Data set line colours.
	 */
	private int[] dataSetColors = ColorTemplate.VORDIPLOM_COLORS;

	/**
	 * Result object created when user selected record to file.
	 */
	private Result results = new Result();

	/**
	 * When started measure process in this variable set beginning time this process.
	 */
	private long startTime = 0;

	/**
	 * Flag indicates start/stop measure process.
	 * <tt>true</tt> process started, otherwise <tt>false</tt> stopped.
	 */
	private boolean isStartMeasure = false;

	/**
	 * Flag indicates turn on or turn of recording measure process.
	 * States are if <tt>true</tt> recording turn on in process or <tt>false</tt> recording turn off.
	 */
	private boolean isRecord = false;

	/**
	 * Channel counter. Pointed to current handled channel.
	 * For 4 total channels this counter accept values are [0, 1, 2, 3].
	 */
	private Integer channel = 0;

	/**
	 * Buffer for values of all channels {@link #MAX_CHANNELS} in full iteration.
	 */
	private float[] valuesOfChannelsBuf = new float[4];


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);

		lineChart = (LineChart) findViewById(R.id.line_chart);

		chartHandler = new ChartHandler(this);
		btHandler = new BTHandler(this);

		chartInit();
		addDataSet();

		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public void onResume() {
		log("@onresume");
		super.onResume();
		if (checkBTState()) {
			setupBTConnection();
		}
	}

	/**
	 * Check Bluetooth support and then check to make sure it is turned on.
	 * Emulator doesn't support Bluetooth and will return null !
	 */
	private boolean checkBTState() {
		if(btAdapter == null) {
			showMsgErrorAndExit("Fatal Error", "Bluetooth not supported !");
			return false;
		} else {
			if (btAdapter.isEnabled()) {
				log("Bluetooth turn on .");
				return true;
			} else {
				// Prompt user to turn on Bluetooth
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				return false;
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		log(">>> @onPause");
		log("Socket close ...");
		try {
			if (btSocket != null) {
				btSocket.close();
				log(">>> bt socket close.");
			}
		} catch (IOException e2) {
			showMsgErrorAndExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
//					getDeviceList();
//					setupBTConnection();
				} else {
					// User did not enable Bluetooth or an error occurred
					log("BT not enabled");
					showMsgErrorAndExit("", "Bluetooth is not enabled. Exit Logger.");
				}
		}
	}

	/**
	 * Attempt ot connection to BT MCU device.
	 */
	private void setupBTConnection() {
		log("Try connection ...");
		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(getAddressFromInternal());

		// Two things are needed to make a connection:
		//      A MAC address, which we got above.
		//      A Service ID or UUID. In this case we are using the UUID for SPP.
		try {
			btSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
		} catch (IOException e) {
			showMsgErrorAndExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
		}
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.
		log("Connecting ...");
//		if (btSocket != null) log("bts NOT null"); else log("bts is null");
		try {
			btSocket.connect();
			log("Connecting and ready do sending data !");

			log("Create data stream ...");
			connectedThread = new ConnectedThread(btSocket, btHandler);
			connectedThread.start();
			log("Data stream created !");
		} catch (IOException e) {
			try {
				log("bts close");
				btSocket.close();
			} catch (IOException e2) {
				showMsgErrorAndExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}
	}

	/**
	 * Obtained MCU BT device address value from internal application storage (ShredPreferences).
	 *
	 * @return bluetooth (BT) address in MCU device.
	 */
	private String getAddressFromInternal() {
		SharedPreferences prefs = getSharedPreferences(Setting.SETTINGS_XML_FILE_NAME, MODE_PRIVATE);
		return prefs.getString(Setting.BT_MCU_DEVICE_ADDRESS_KEY, Setting.BT_MCU_DEVICE_DEFAULT_ADDRESS);
	}

	/**
	 * Initial chart configuration
	 */
	private void chartInit() {
		lineChart.setOnChartValueSelectedListener(this);
		lineChart.setDrawGridBackground(false);
		lineChart.setDescription("");
		lineChart.setData(new LineData());
		lineChart.invalidate();
		lineChart.setBackgroundColor(Color.GRAY);
		lineChart.getAxisLeft().setAxisMaxValue(255f);
		lineChart.getAxisLeft().setAxisMinValue(0);
	}

	/**
	 * If there is problem in application show this Toast (massage) and exit from application.
	 *
	 * @param title of this massage
	 * @param message showing in Toast
	 */
	private void showMsgErrorAndExit(String title, String message){
		Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		miRecord = menu.findItem(R.id.mi_action_record);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.mi_action_start_stop_process) {
			actionStartStopProcess(item);

		} else if (item.getItemId() == R.id.mi_action_record) {
			actionRecordData(item);

		} else if (item.getItemId() == R.id.mi_action_refresh) {
			setupBTConnection();

		} else if (item.getItemId() == R.id.mi_action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
		}
		return true;
	}

	/**
	 * This method must be called on {@link #onOptionsItemSelected} for start or stop measure process action.
	 * If {@link #isStartMeasure} flag == <tt>false</tt> -> reset previous data and start new msr process.
	 * If this flag <tt>true</tt></t> -> stop msr process.
	 *
	 * @param item start or stop action menu item
	 */
	private void actionStartStopProcess(MenuItem item) {
		if (!isStartMeasure) {
			lineChart.clear();
			lineChart.setData(new LineData());
			addDataSet();
			lineChart.notifyDataSetChanged();
			lineChart.invalidate();
			item.setIcon(getResources().getDrawable(R.mipmap.ic_start_process_turn_on));
		} else {
			item.setIcon(getResources().getDrawable(R.mipmap.ic_start_process_turn_off));
			if (isRecord) {
				Recorder.writeToFile(results);
				isRecord = false;
				miRecord.setIcon(R.mipmap.ic_recording_turn_off);
				Toast.makeText(this, "Recorded data saved", Toast.LENGTH_LONG).show();
			}
		}
		isStartMeasure = !isStartMeasure;

		if (connectedThread != null) connectedThread.write(channel.toString());
		startTime = System.currentTimeMillis();
	}

	/**
	 * This method must be called on {@link #onOptionsItemSelected} for recording data to file.
	 * Set or reset flag {@link #isRecord} for record process opportunity.
	 *
	 * @param item record meu item
	 * @return <tt>true</tt> -  flag {@link #isRecord} set. Otherwise <tt>false</tt> - flag not set because
	 *          measure process started
	 */
	private boolean actionRecordData(MenuItem item) {
		if (isStartMeasure) {
			Toast.makeText(this, "In measure process is forbid to change record state !", Toast.LENGTH_LONG).show();
			return false;
		}
		isRecord = !isRecord;
		if (isRecord) {
			item.setIcon(getResources().getDrawable(R.mipmap.ic_recording_turn_on));
		} else {
			item.setIcon(getResources().getDrawable(R.mipmap.ic_recording_turn_off));
		}
		return true;
	}

	@Override
	public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
	}

	@Override
	public void onNothingSelected() {
	}

	/**
	 * Add point to chart for any channel.
	 *
	 * @param channel channel where necessary set value in chart.
	 *                For 4 channels values this var are [0 ... 3].
	 * @param val value from selected channel.
	 */
	private void addEntry(int channel, float val) {
		Log.d(LOG, "channel = " + channel + " | " + "val = " + val);
		LineData data = lineChart.getData();

		if(data != null) {
			ILineDataSet set = data.getDataSetByIndex(0);

			if (set == null) {
				set = createSet();
				data.addDataSet(set);
			}

//			data.addEntry(new Entry(val, set.getEntryCount()), channel - 1);
			switch (channel) {
				case 0:
					valuesOfChannelsBuf[0] = val;
					break;
				case 1:
					valuesOfChannelsBuf[1] = val;
					break;
				case 2:
					valuesOfChannelsBuf[2] = val;
					break;
				case 3:
					valuesOfChannelsBuf[3] = val;
					break;
			}
			if (channel == MAX_CHANNELS - 1) {
				for (int ch = 0; ch < MAX_CHANNELS; ch++) {
					data.addEntry(new Entry(valuesOfChannelsBuf[ch], set.getEntryCount()), ch);
				}
				// add a new x-value first
				getRealTime();
				data.addXValue(getRealTime());
			}

			lineChart.notifyDataSetChanged();

			lineChart.setVisibleXRangeMaximum(300);
			lineChart.setVisibleYRangeMaximum(200, YAxis.AxisDependency.LEFT);
//          // this automatically refreshes the chart (calls invalidate())
			lineChart.moveViewTo(data.getXValCount()-7, 50f, YAxis.AxisDependency.LEFT);
		}
	}

	/**
	 * Getting system real time.
	 *
	 * @return time in MM:SS match pattern format.
	 */
	private String getRealTime(){
		long processTime = System.currentTimeMillis() - startTime;
		int seconds = (int) (processTime / 1000) % 60 ;
		int minutes = (int) ((processTime / (1000*60)) % 60);
		return minutes + ":" + seconds;
	}

	/**
	 * Add data set to one channel in chart.
	 */
	private void addDataSet() {
		LineData data = lineChart.getData();

		if(data != null) {
//			log("data.getDataSetCount() = " + data.getDataSetCount());
//			int count = (data.getDataSetCount() + 1);
//			log("data.getDataSetCount() = " + data.getDataSetCount());

			// create 10 y-vals
//			ArrayList<Entry> yVals = new ArrayList<Entry>();

//			log("data.getXValCount() = " + (data.getXValCount()));

			// i - is channel counter
			for (int i = 0 ; i < MAX_CHANNELS; i++) {
				LineDataSet set = new LineDataSet(new ArrayList<Entry>(), "DataSet " + i);
				set.setLineWidth(2.5f);
				set.setCircleRadius(0);

				int color = dataSetColors[i % dataSetColors.length];

				set.setColor(color);
				set.setCircleColor(color);
				set.setHighLightColor(color);
				set.setValueTextSize(10f);
				set.setValueTextColor(color);

				data.addDataSet(set);
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
			}
		}
	}

	/**
	 * Creating general data set design.
	 *
	 * @return linear data set object.
	 */
	private LineDataSet createSet() {
		LineDataSet set = new LineDataSet(null, "DataSet 1");
		set.setLineWidth(2.5f);
		set.setCircleRadius(0);
		set.setColor(Color.rgb(240, 99, 99));
		set.setCircleColor(Color.rgb(240, 99, 99));
		set.setHighLightColor(Color.rgb(190, 190, 190));
		set.setAxisDependency(YAxis.AxisDependency.LEFT);
		set.setValueTextSize(10f);
		return set;
	}

	/**
	 * Handler for update chart in UI thread.
	 */
	private static class ChartHandler extends Handler {
		WeakReference<MainActivity> wActivity;

		public ChartHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
//				super.handleMessage(msg);
			if (msg.what == RECEIVE_BT_DATA) {
//				Log.d(LOG, "RX chart = " + msg.arg1);

				// call this method for add point to chart !
				wActivity.get().addEntry(wActivity.get().channel, msg.arg1);
//				wActivity.get().results.getValues1().add(msg.arg1);

				wActivity.get().channel++;
//				Log.d(LOG, "wActivity.get().channel = " + wActivity.get().channel);
				if (wActivity.get().channel == MAX_CHANNELS) wActivity.get().channel = 0;
				wActivity.get().connectedThread.write(wActivity.get().channel.toString());
			}
		}
	}

	/**
	 * Handler serving receiving (RX) data from BT MCU device.
	 * When in {@link ConnectedThread} received data called this handler
	 * for asynchronous update chart in UI thread.
	 *
	 * @see ChartHandler
	 */
	private static class BTHandler extends Handler{
		WeakReference<MainActivity> wActivity;

		public BTHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case RECEIVE_MSG:
					byte[] byteBuf = (byte[]) msg.obj;
					char[] charBuf = new String(byteBuf, 0, msg.arg1).toCharArray();
					Message msg1 = wActivity.get().chartHandler.obtainMessage(RECEIVE_BT_DATA, charBuf[0], 0);
					wActivity.get().chartHandler.sendMessage(msg1);
					break;
			}
		}
	}

	/**
	 * Output logging information in console if {@link #isLog} flag enabled.
	 *
	 * @param msg logging massage
	 */
	private void log(String msg) {
		if (isLog) Log.d(LOG, msg);
	}

}
