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
import java.io.InputStream;
import java.io.OutputStream;
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
	 * MAC address bluetooth (BT) module. This BT module placed in mcu device.
	 */
	public static final String BT_DEVICE_ADDRESS = "20:11:02:47:01:60"; //for H-C-2010-06-01

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
	private static final int RECEIVE_MSG = 1;

	/**
	 * Data set line colours.
	 */
	private int[] dataSetColors = ColorTemplate.VORDIPLOM_COLORS;

	/**
	 * Dynamic line chart object.
	 */
	private LineChart lineChart;

	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private ConnectedThread connectedThread;

	/**
	 * When started measure process in this variable set beginning time this process.
	 */
	private long startTime = 0;

	/**
	 * Flag indicates start/stop measure process.
	 * <tt>true</tt> process started, otherwise <tt>false</tt> stopped.
	 */
	private boolean isStartMeasure = false;

	private Handler chartHandler;
	private Handler btHandler;


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
			getDeviceList();
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
			connectedThread = new ConnectedThread(btSocket);
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
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.mi_action_call_mcu) {
			// if isStartMeasure flag == false -> reset previous data and start msr
			// if true -> stop msr
			if (!isStartMeasure) {
				lineChart.clear();
				lineChart.setData(new LineData());
				addDataSet();
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				item.setIcon(getResources().getDrawable(R.mipmap.ic_start_process_turn_on));
			} else {
				item.setIcon(getResources().getDrawable(R.mipmap.ic_start_process_turn_off));
			}
			isStartMeasure = !isStartMeasure;

			if (connectedThread != null) connectedThread.write("b");
			startTime = System.currentTimeMillis();

		} else if (item.getItemId() == R.id.mi_action_refresh) {
			setupBTConnection();

		} else if (item.getItemId() == R.id.mi_action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
		}
		return true;
	}

	@Override
	public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

	}

	@Override
	public void onNothingSelected() {

	}

	private void addEntry(float val1) {
		LineData data = lineChart.getData();

		if(data != null) {
			ILineDataSet set = data.getDataSetByIndex(0);

			if (set == null) {
				set = createSet();
				data.addDataSet(set);
			}

			// add a new x-value first
			getRealTime();
			data.addXValue(getRealTime());
			data.addEntry(new Entry(val1, set.getEntryCount()), 0);

			lineChart.notifyDataSetChanged();

			lineChart.setVisibleXRangeMaximum(300);
			lineChart.setVisibleYRangeMaximum(200, YAxis.AxisDependency.LEFT);
//          // this automatically refreshes the chart (calls invalidate())
			lineChart.moveViewTo(data.getXValCount()-7, 50f, YAxis.AxisDependency.LEFT);
		}
	}

	private String getRealTime(){
		long processTime = System.currentTimeMillis() - startTime;
		int seconds = (int) (processTime / 1000) % 60 ;
		int minutes = (int) ((processTime / (1000*60)) % 60);
		return minutes + ":" + seconds;
	}

	private void addDataSet() {
		LineData data = lineChart.getData();

		if(data != null) {
//			log("data.getDataSetCount() = " + data.getDataSetCount());
			int count = (data.getDataSetCount() + 1);
//			log("data.getDataSetCount() = " + data.getDataSetCount());

			// create 10 y-vals
			ArrayList<Entry> yVals = new ArrayList<Entry>();

//			log("data.getXValCount() = " + (data.getXValCount()));

			LineDataSet set = new LineDataSet(yVals, "DataSet " + count);
			set.setLineWidth(2.5f);
			set.setCircleRadius(0);

			int color = dataSetColors[count % dataSetColors.length];

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

	private void getDeviceList() {
		Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
		for (BluetoothDevice device : bondedDevices) {
			log("device = " + device);
			log("device.getName() = " + device.getName());
			log("device.getAddress() = " + device.getAddress());
			log("device.getBondState() = " + device.getBondState());
			log("---");
		}
	}

	private static class ChartHandler extends Handler {
		WeakReference<MainActivity> wActivity;

		public ChartHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
//				super.handleMessage(msg);
			if (msg.what == RECEIVE_BT_DATA) {
				// call this method for add point to chart !
				wActivity.get().addEntry(msg.arg1);
			}
		}
	}

	private static class BTHandler extends Handler{
		WeakReference<MainActivity> wActivity;

		public BTHandler(MainActivity activity) {
			wActivity = new WeakReference<MainActivity>(activity);
		}

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case RECEIVE_MSG:
					byte[] readBuf = (byte[]) msg.obj;
					String strIncom = new String(readBuf, 0, msg.arg1);
					char[] chars = strIncom.toCharArray();
//						for (char aChar : chars) {
//							Log.d(LOG, "> char = "  +(byte)aChar);
//							log( "> char = "  +(byte)aChar);
//						}
					Message msg1 = wActivity.get().chartHandler.obtainMessage(RECEIVE_BT_DATA, chars[0], 0);
					wActivity.get().chartHandler.sendMessage(msg1);
					break;
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final String LOG = ConnectedThread.class.getName();
		private boolean isLogThread = false;
		private final BluetoothSocket btSocket;
		private final InputStream is;
		private final OutputStream os;

		public ConnectedThread(BluetoothSocket btSocket) {
			this.btSocket = btSocket;
			InputStream isTmp = null;
			OutputStream osTmp = null;

			// Get the input and output streams, using temp objects because member streams are final.
			try {
				isTmp = btSocket.getInputStream();
				osTmp = btSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			is = isTmp;
			os = osTmp;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[256];  // buffer store for the stream
			int bytes;

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					bytes = is.read(buffer);
					btHandler.obtainMessage(RECEIVE_MSG, bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}

		/**
		 * Write string data to output stream and translate from BT to mcu device.
		 *
		 * @param msg massage sending to mcu device
		 */
		public void write(String msg) {
			if (isLogThread) Log.d(LOG, "Data for sending to mcu device : " + msg + "...");
			byte[] msgBuffer = msg.getBytes();
			try {
				os.write(msgBuffer);
			} catch (IOException e) {
				if (isLogThread) Log.d(LOG, "Error data sending : " + e.getMessage() + " !");
			}
		}

		/**
		 * Call this from the main activity to shutdown the connection
		 */
		public void cancel() {
			try {
				btSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
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
