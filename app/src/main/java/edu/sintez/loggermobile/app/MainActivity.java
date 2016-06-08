package edu.sintez.loggermobile.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class MainActivity extends Activity implements OnChartValueSelectedListener {

	private static final String LOG = MainActivity.class.getName();
	private LineChart lineChart;
	private int[] colors = ColorTemplate.VORDIPLOM_COLORS;


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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
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

}
