package edu.sintez.loggermobile.app.utils;

import android.os.Environment;
import edu.sintez.loggermobile.app.activity.MainActivity;
import edu.sintez.loggermobile.app.model.Result;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Provide save measure process results to external storage to logger directory .
 */
public class Recorder {

	private static final String LOG = Recorder.class.getName();
	private static final String DIR_NAME_SAVE_RESULT_FILES = "Logger saved results";
	private static final String CSV_EXT_FILE = ".csv";
	private static final String PREFIX_FILE = "results_";
	private static final String LFCR = "\n";
	private static final String TAB = "\t";
	private static final String HEADER_FILE = "Num" + TAB + "ch0"+ TAB + "ch1"+ TAB + "ch2"+ TAB + "ch3";
	private static final File rootPath = Environment.getExternalStorageDirectory();

	/**
	 * Write measure results as text in file in csv format.
	 *
	 * @param result object contained all recorded data of channels
	 */
	public static void writeToFile(Result result) {
		if (isExternalStorageWritable()) {
			File loggerDir = new File(rootPath + File.separator + DIR_NAME_SAVE_RESULT_FILES);
			if (!loggerDir.exists()) {
				boolean mkdirs = loggerDir.mkdirs();
			}

			// This buffer contained channels values and other information to be recorded to result file.
			StringBuilder buffer = new StringBuilder();

			ArrayList<Integer> values0 = result.getValues0();
			ArrayList<Integer> values1 = result.getValues1();
			ArrayList<Integer> values2 = result.getValues2();
			ArrayList<Integer> values3 = result.getValues3();

			buffer.append(HEADER_FILE).append(LFCR);
			// Discard last values channels - focused on minimum list size.
			int minSizeList = findMin(values0.size(), values1.size(), values2.size(), values3.size());
			int line = 0; // line number on which data was stored
			while ( (minSizeList - 1) != line ){
				switch (MainActivity.MAX_CHANNELS) {
					case 1:
						buffer.append(line).append(TAB)
							.append(values0.get(line)).append(TAB)
							.append(LFCR);
						break;
					case 2:
						buffer.append(line).append(TAB)
							.append(values0.get(line)).append(TAB)
							.append(values1.get(line)).append(TAB)
							.append(LFCR);
						break;
					case 3:
						buffer.append(line).append(TAB)
							.append(values0.get(line)).append(TAB)
							.append(values1.get(line)).append(TAB)
							.append(values2.get(line)).append(TAB)
							.append(LFCR);
						break;
					case 4:
						buffer.append(line).append(TAB)
							.append(values0.get(line)).append(TAB)
							.append(values1.get(line)).append(TAB)
							.append(values2.get(line)).append(TAB)
							.append(values3.get(line)).append(TAB)
							.append(LFCR);
						break;
				}
				line++;
			}

			values0.clear();
			values1.clear();
			values2.clear();
			values3.clear();

			File file = new File (loggerDir, PREFIX_FILE + getCurrentDate() + CSV_EXT_FILE);
			try {
				FileWriter fw = new FileWriter(file);
				fw.write(buffer.toString());
				fw.flush();
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks external storage is available for read and write
	 */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	/**
	 * Find minimum for 4 integer numbers.
	 * Note - this check is correct if num1 not 0 !
	 * Value 0 can be: num2, num3, num4.
	 *
	 * @param num1 number 1 (!= 0)
	 * @param num2 number 2
	 * @param num3 number 3
	 * @param num4 number 4
	 *
	 * @return minimum number
	 */
	private static int findMin(int num1, int num2, int num3, int num4) {
		int min = num1;
		if (num2 != 0 && min > num2) {
			min = num2;
		}
		if (num3 != 0 && min > num3) {
			min = num3;
		}
		if (num4 != 0 && min > num4) {
			min = num4;
		}
		return min;
	}

	/**
	 * Get date when file will be created
	 *
	 * @return string date presentation which pattern dd_MM_yyyy__HH_mm_ss
	 * @see <a href="https://developer.android.com/reference/java/text/SimpleDateFormat.html">See documentation
	 * for SimpleDateFormat class.</a>
	 */
	private static String getCurrentDate(){
		Date date = new Date();
		SimpleDateFormat formatDate = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss", Locale.getDefault());
		return formatDate.format(date);
	}

}
