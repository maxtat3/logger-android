package edu.sintez.loggermobile.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements OnChartValueSelectedListener {

	private static final String LOG = MainActivity.class.getName();
	private LineChart lineChart;
	private int[] colors = ColorTemplate.VORDIPLOM_COLORS;

	// SPP UUID сервиса
	private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// MAC-адрес Bluetooth модуля
	private static final String BT_DEVICE_ADDRESS = "20:11:02:47:01:60"; //for H-C-2010-06-01
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int RECEIVE_BT_DATA = 1;

	private Handler btHandler;
	private static final int RECEIVE_MESSAGE = 1;        // Статус для Handler
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private ConnectedThread connectedThread;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);

		lineChart = (LineChart) findViewById(R.id.line_chart);
		lineChart.setOnChartValueSelectedListener(this);
		lineChart.setDrawGridBackground(false);
		lineChart.setDescription("");
		lineChart.setData(new LineData());
		lineChart.invalidate();
		lineChart.setBackgroundColor(Color.GRAY);

		final Handler chartHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
//				super.handleMessage(msg);
				if (msg.what == RECEIVE_BT_DATA) {
					// call this method for add point to chart !
					addEntry(msg.arg1);
				}
			}
		};

		btHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
					case RECEIVE_MESSAGE:
						byte[] readBuf = (byte[]) msg.obj;
						String strIncom = new String(readBuf, 0, msg.arg1);
						char[] chars = strIncom.toCharArray();
//						for (char aChar : chars) {
//							Log.d(LOG, "> char = "  +(byte)aChar);
//						}
						Message msg1 = chartHandler.obtainMessage(RECEIVE_BT_DATA, chars[0], 0);
						chartHandler.sendMessage(msg1);
						break;
				}
			};
		};

		getDeviceList();

		addDataSet();
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
			connectedThread.write("b");
			Toast.makeText(this, "Entry added!", Toast.LENGTH_SHORT).show();
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
			data.addXValue(set.getEntryCount() + "");
			data.addEntry(new Entry(val1, set.getEntryCount()), 0);

			lineChart.notifyDataSetChanged();

			lineChart.setVisibleXRangeMaximum(300);
			lineChart.setVisibleYRangeMaximum(200, YAxis.AxisDependency.LEFT);
//          // this automatically refreshes the chart (calls invalidate())
			lineChart.moveViewTo(data.getXValCount()-7, 50f, YAxis.AxisDependency.LEFT);
		}
	}

	private void addDataSet() {
		LineData data = lineChart.getData();

		if(data != null) {
			Log.d(LOG, "data.getDataSetCount() = " + data.getDataSetCount());
			int count = (data.getDataSetCount() + 1);
			Log.d(LOG, "data.getDataSetCount() = " + data.getDataSetCount());

			// create 10 y-vals
			ArrayList<Entry> yVals = new ArrayList<Entry>();

			Log.d(LOG, "data.getXValCount() = " + (data.getXValCount()));

			LineDataSet set = new LineDataSet(yVals, "DataSet " + count);
			set.setLineWidth(2.5f);
			set.setCircleRadius(0);

			int color = colors[count % colors.length];

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
			Log.d(LOG, "device = " + device);
			Log.d(LOG, "device.getName() = " + device.getName());
			Log.d(LOG, "device.getAddress() = " + device.getAddress());
			Log.d(LOG, "device.getBondState() = " + device.getBondState());
			Log.d(LOG, "---");
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket btSocket;
		private final InputStream is;
		private final OutputStream os;

		public ConnectedThread(BluetoothSocket btSocket) {
			this.btSocket = btSocket;
			InputStream isTmp = null;
			OutputStream osTmp = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
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
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = is.read(buffer);        // Получаем кол-во байт и само собщение в байтовый массив "buffer"
					btHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Отправляем в очередь сообщений Handler
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {
			Log.d(LOG, "Данные для отправки: " + message + "...");
			byte[] msgBuffer = message.getBytes();
			try {
				os.write(msgBuffer);
			} catch (IOException e) {
				Log.d(LOG, "Ошибка отправки данных: " + e.getMessage() + "...");
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				btSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
