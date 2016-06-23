package edu.sintez.loggermobile.app;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thread connected to BT in MCU device.
 */
public class ConnectedThread extends Thread {
	private final String LOG = ConnectedThread.class.getName();
	private boolean isLogThread = false;

	private final BluetoothSocket btSocket;
	private Handler btHandler;
	private final InputStream is;
	private final OutputStream os;

	public ConnectedThread(BluetoothSocket btSocket, Handler btHandler) {
		this.btSocket = btSocket;
		this.btHandler = btHandler;
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
				btHandler.obtainMessage(MainActivity.RECEIVE_MSG, bytes, -1, buffer).sendToTarget();
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
