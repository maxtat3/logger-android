package edu.sintez.loggermobile.app;

import android.os.Environment;
import android.util.Log;

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

			ArrayList<Integer> values1 = result.getValues1();
			int line = 0; // line number count on which data was stored
			for (Integer res : values1) {
				buffer.append(line).append(TAB)
					.append(res).append(TAB)
					.append(LFCR);
				line ++;
			}

			values1.clear();

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
