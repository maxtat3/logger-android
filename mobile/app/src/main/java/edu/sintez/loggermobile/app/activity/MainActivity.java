package edu.sintez.loggermobile.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
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
import edu.sintez.loggermobile.app.*;
import edu.sintez.loggermobile.app.async.ConnectedThread;
import edu.sintez.loggermobile.app.model.Result;
import edu.sintez.loggermobile.app.model.Setting;
import edu.sintez.loggermobile.app.utils.Recorder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
	 * Maximum viewed points (samples) placed for X axis in one display.
	 * After this maximum points, chart dynamic shifted to left.
	 */
	private static final int CHART_X_MAX_POINTS = 100;

	/**
	 * Minimum value samples (points) in Y axis.
	 */
	private static final float CHART_Y_MIN_VAL = 0;

	/**
	 * Maximum value samples (points) in Y axis.
	 * This set for 10 bit ADC.
	 */
	private static final float CHART_Y_MAX_VAL = 1050;

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
	 * Application in connection process.
	 */
	public static final int STATUS_CONNECTION = 1;

	/**
	 * Application connected to BT MCU device.
	 */
	public static final int STATUS_CONNECTED = 2;

	/**
	 * Application unable to connect.
	 */
	public static final int STATUS_CONNECTION_ERROR = 3;

	/**
	 * Command to BT MCU device to start and stop process.
	 */
	private static final String START_STOP_CMD = "t";

	/**
	 * Max amount of channels in measure process involved.
	 */
	public static final int MAX_CHANNELS = 4;

	private enum ConnectionState {
		CONNECTED,
		NOT_CONNECTION
	}

	private ConnectionState connState = ConnectionState.NOT_CONNECTION;

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

	private Handler connHandler;
	private Handler chartHandler;
	private Handler btHandler;

	/**
	 * Show easy progress when trying to connect BT MCU device.
	 */
	private ProgressDialog connDialog = null;

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

		connHandler = new ConnectionHandler(this);
		chartHandler = new ChartHandler(this);
		btHandler = new BTHandler(this);

		connectingDialogInit();
		chartInit();
		addDataSet();

		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	/**
	 * Initialization connecting dialog when trying to connect BT MCU device.
	 */
	private void connectingDialogInit(){
		connDialog = new ProgressDialog(this);
		connDialog.setCancelable(true);
		connDialog.setMessage("Try connect to " + getAddressFromInternal() + " device");
		connDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}

	@Override
	public void onResume() {
		log("@onResume");
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
				if (resultCode != Activity.RESULT_OK) {
					// User did not enable Bluetooth or an error occurred
					log("BT not enabled or not supported in this device.");
					showMsgErrorAndExit("", "Bluetooth is not enabled. Exit Logger.");
				}
		}
	}

	/**
	 * Attempt ot connection to BT MCU device.
	 */
	private void setupBTConnection() {
		log("Try connection ...");
		BluetoothDevice device = btAdapter.getRemoteDevice(getAddressFromInternal());

		// Two things are needed to make a connection:
		// - MAC address, which we got above.
		// - Service ID or UUID. In this case we are using the UUID for SPP.
		try {
			btSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
		} catch (IOException e) {
			showMsgErrorAndExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
		}
		btAdapter.cancelDiscovery();

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Establish the connection. This will block until it connects.
				log("Connecting ...");
				try {
					connHandler.sendEmptyMessage(STATUS_CONNECTION);
					btSocket.connect();

					log("Connecting and ready do sending data !");
					log("Create data stream ...");
					connectedThread = new ConnectedThread(btSocket, btHandler);
					connectedThread.start();
					connHandler.sendEmptyMessage(STATUS_CONNECTED);
					log("Data stream created !");
				} catch (IOException e) {
					try {
						log("bts close");
						connHandler.sendEmptyMessage(STATUS_CONNECTION_ERROR);
						btSocket.close();
					} catch (IOException e2) {
						showMsgErrorAndExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
					}
				}
			}
		}).start();


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
		lineChart.getAxisLeft().setAxisMinValue(CHART_Y_MIN_VAL);
		lineChart.getAxisLeft().setAxisMaxValue(CHART_Y_MAX_VAL);
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
			if (connState == ConnectionState.NOT_CONNECTION) setupBTConnection();
			else Toast.makeText(this, "Already connected", Toast.LENGTH_SHORT).show();

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
		if (connState == ConnectionState.NOT_CONNECTION) {
			Toast.makeText(this, "No connection", Toast.LENGTH_SHORT).show();
			return;
		}

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

		if (connectedThread != null) {
			connectedThread.write(START_STOP_CMD);
		}
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
		if (connState == ConnectionState.NOT_CONNECTION) {
			Toast.makeText(this, "No connection", Toast.LENGTH_SHORT).show();
			return false;
		}

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
		LineData data = lineChart.getData();

		if(data != null) {
			ILineDataSet set = data.getDataSetByIndex(0);

			if (set == null) {
				set = createSet();
				data.addDataSet(set);
			}

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

			lineChart.setVisibleXRangeMaximum(CHART_X_MAX_POINTS);
			lineChart.setVisibleYRangeMaximum(CHART_Y_MAX_VAL, YAxis.AxisDependency.LEFT);
			lineChart.notifyDataSetChanged();
			// this automatically refreshes the chart (calls invalidate())
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
			for (int ch = 0 ; ch < MAX_CHANNELS; ch++) {
				LineDataSet set = new LineDataSet(new ArrayList<Entry>(), "DataSet " + ch);
				set.setLineWidth(2.5f);
				set.setCircleRadius(0);

				int color = dataSetColors[ch % dataSetColors.length];

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

	/*
	 * ----------------------------------
	 *      Handlers for update UI
	 * ----------------------------------
	 */

	/**
	 * Show progress (connection) dialog in this process and
	 * show status Toast massages.
	 *
	 * @see #connDialog
	 */
	private static class ConnectionHandler extends Handler{
		WeakReference<MainActivity> wActivity;

		public ConnectionHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case STATUS_CONNECTION:
					wActivity.get().connDialog.show();
					wActivity.get().connDialog.setCanceledOnTouchOutside(false);
					break;

				case STATUS_CONNECTED:
					wActivity.get().connState = ConnectionState.CONNECTED;
					wActivity.get().connDialog.dismiss();
					Toast.makeText(
						wActivity.get().getBaseContext(),
						"Connected",
						Toast.LENGTH_LONG
					).show();
					break;

				case STATUS_CONNECTION_ERROR:
					wActivity.get().connState = ConnectionState.NOT_CONNECTION;
					wActivity.get().connDialog.dismiss();
					Toast.makeText(
						wActivity.get().getBaseContext(),
						"Unable connected to " + wActivity.get().getAddressFromInternal() + " device",
						Toast.LENGTH_LONG
					).show();
					break;
			}
		}
	}

	/**
	 * Handler for update chart in UI thread.
	 */
	private static class ChartHandler extends Handler {
		private int byteCounter = 0; //0 - low Byte, 1 - high Byte; By default pointed to low Byte
		private int rxLow; // low Byte, using 7 bit only
		private int rxHigh; // high Byte, using 7 bit only
		private int adcVal; // ADC value
		private int channel; // channel number (0 ... 3)
		private WeakReference<MainActivity> wActivity;

		public ChartHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == RECEIVE_BT_DATA) {
				if (byteCounter == 0) {
					rxLow = msg.arg1;
					byteCounter++;

				}else if (byteCounter == 1) {
					rxHigh = msg.arg1;
					// all Bytes (7 bits in every Byte) received and do decoding it
					// High Byte [14 - 11] bits - number of ADC channel, [10 - 8] bits - 10 bits ADC value part
					// low Byte [7 - 0] bits - 10 bits ADC value part
					adcVal = (rxLow & 0x7F) | ((rxHigh & 0x7) << 7);
					channel = (rxHigh & 0x78) >> 3;

					byteCounter = 0;

//					Log.d(LOG, "val = " + adcVal);
//					Log.d(LOG, "cmd = " + channel);
//					if (channel == 3) Log.d(LOG, "----");
					wActivity.get().addEntry(channel, adcVal);

					// record values to file
					if (wActivity.get().isRecord) {
						switch (channel) {
							case 0:
								wActivity.get().results.getValues0().add(adcVal);
								break;
							case 1:
								wActivity.get().results.getValues1().add(adcVal);
								break;
							case 2:
								wActivity.get().results.getValues2().add(adcVal);
								break;
							case 3:
								wActivity.get().results.getValues3().add(adcVal);
								break;
						}
					}
				}
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

		@Override
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
