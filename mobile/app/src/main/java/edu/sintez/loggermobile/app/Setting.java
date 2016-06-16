package edu.sintez.loggermobile.app;

/**
 * Settings which contain this application.
 */
public class Setting {

	/**
	 * File name contains this object settings data in application.
	 */
	public static final String SETTINGS_XML_FILE_NAME = "settings.xml";

	/**
	 * Default address value. Using when application first installed to android device and
	 * settings internal storage is empty.
	 * In method {@link android.bluetooth.BluetoothAdapter#getRemoteDevice} must be passed
	 * any formatted address otherwise throw {@link IllegalArgumentException}.
	 */
	public static final String SETTINGS_BT_MCU_DEVICE_DEFAULT_ADDRESS = "00:00:00:00:00:00";

	public static final String SETTINGS_BT_MCU_DEVICE_ADDRESS_KEY = "BT_MCU_DEVICE_ADDRESS";

	private String address;

	/**
	 * Constructor setting object. This data must be save and restore
	 * at {@link android.content.SharedPreferences} mechanism.
	 *
	 * @param address bluetooth (BT) address in MCU device.
	 *                As key this value must be used {@link #SETTINGS_BT_MCU_DEVICE_ADDRESS_KEY}.
	 */
	public Setting(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
