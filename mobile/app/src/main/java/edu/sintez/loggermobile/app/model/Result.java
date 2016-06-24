package edu.sintez.loggermobile.app.model;

import java.util.ArrayList;

/**
 * Contained results in measure process for saving in file.
 */
public class Result {

	/**
	 * Results container for channel 0
	 */
	private ArrayList<Integer> values0 = new ArrayList<Integer>();

	/**
	 * Results container for channel 1
	 */
	private ArrayList<Integer> values1 = new ArrayList<Integer>();

	/**
	 * Results container for channel 2
	 */
	private ArrayList<Integer> values2 = new ArrayList<Integer>();

	/**
	 * Results container for channel 3
	 */
	private ArrayList<Integer> values3 = new ArrayList<Integer>();


	public ArrayList<Integer> getValues0() {
		return values0;
	}

	public void setValues0(ArrayList<Integer> values0) {
		this.values0 = values0;
	}

	public ArrayList<Integer> getValues1() {
		return values1;
	}

	public void setValues1(ArrayList<Integer> values1) {
		this.values1 = values1;
	}

	public ArrayList<Integer> getValues2() {
		return values2;
	}

	public void setValues2(ArrayList<Integer> values2) {
		this.values2 = values2;
	}

	public ArrayList<Integer> getValues3() {
		return values3;
	}

	public void setValues3(ArrayList<Integer> values3) {
		this.values3 = values3;
	}
}
