/*
 * $Id$
 *
 * Authors:
 *      Jeff Buchbinder <jeff@freemedsoftware.org>
 *
 * FreeMED Electronic Medical Record and Practice Management System
 * Copyright (C) 1999-2009 FreeMED Software Foundation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.freemedsoftware.gwt.client;

import java.io.Serializable;
import java.util.HashMap;

import org.freemedsoftware.gwt.client.Util.ProgramMode;
import org.freemedsoftware.gwt.client.screen.MainScreen;
import org.freemedsoftware.gwt.client.screen.PatientScreen;
import org.freemedsoftware.gwt.client.widget.Toaster;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;

public class CurrentState {

	protected static HashMap<String, String> statusItems = new HashMap<String, String>();

	protected static Label statusBar = null;

	protected static Toaster toaster = null;

	protected static TabPanel tabPanel = null;

	protected static String locale = "en_US";

	protected static Integer defaultProvider = new Integer(0);

	protected static HashMap<Integer, PatientScreen> patientScreenMap = new HashMap<Integer, PatientScreen>();

	protected static HashMap<String, String> userConfiguration = new HashMap<String, String>();

	protected static FreemedInterface freemedInterface = null;

	protected static MainScreen mainScreen = null;

	public CurrentState() {
		retrieveUserConfiguration(true);
	}

	/**
	 * Bulk assign mainscreen object
	 * 
	 * @param m
	 */
	public static void assignMainScreen(MainScreen m) {
		mainScreen = m;
		assignStatusBar(m.getStatusBar());
		assignTabPanel(m.getTabPanel());
	}

	public static void assignFreemedInterface(FreemedInterface i) {
		freemedInterface = i;
	}

	/**
	 * Assign status bar object.
	 * 
	 * @param w
	 */
	public static void assignStatusBar(Label l) {
		statusBar = l;
	}

	/**
	 * Assign default provider.
	 * 
	 * @param p
	 */
	public static void assignDefaultProvider(Integer p) {
		defaultProvider = p;
	}

	/**
	 * Assign tab panel object.
	 * 
	 * @param t
	 */
	public static void assignTabPanel(TabPanel t) {
		tabPanel = t;
	}

	/**
	 * Assign toaster object.
	 */
	public static void assignToaster(Toaster t) {
		toaster = t;
	}

	/**
	 * Assign locale value.
	 * 
	 * @param l
	 *            Locale string, default is "en_US"
	 */
	public static void assignLocale(String l) {
		locale = l;
	}

	/**
	 * Add an item to the status bar stack.
	 * 
	 * @param module
	 * @param text
	 */
	public static void statusBarAdd(String module, String text) {
		statusItems.put(module, text);
		((Label) statusBar).setText("Processing (" + text + ")");
	}

	/**
	 * Remove an item from the status bar stack.
	 * 
	 * @param module
	 */
	public static void statusBarRemove(String module) {
		statusItems.remove(module);
		if (statusItems.size() > 0) {
			((Label) statusBar).setText("Processing");
		} else {
			((Label) statusBar).setText("Ready");
		}
	}

	public static String getLocale() {
		return locale;
	}

	public static Integer getDefaultProvider() {
		return defaultProvider;
	}

	public static FreemedInterface getFreemedInterface() {
		return freemedInterface;
	}

	public static MainScreen getMainScreen() {
		return mainScreen;
	}

	public static TabPanel getTabPanel() {
		return tabPanel;
	}

	public static Toaster getToaster() {
		return toaster;
	}

	public static HashMap<Integer, PatientScreen> getPatientScreenMap() {
		return patientScreenMap;
	}

	/**
	 * Get user specific configuration value, or "" if there is no value.
	 * 
	 * @param key
	 * @return
	 */
	public static String getUserConfig(String key) {
		JsonUtil.debug("getUserConfig() called");
		if (userConfiguration.size() != 0) {
			return userConfiguration.get(key);
		}
		JsonUtil.debug("getUserConfig(): was unable to find userConfiguration "
				+ "| key = " + key);
		return "";
	}

	/**
	 * Set user specific configuration value.
	 * 
	 * @param key
	 * @param value
	 */
	public static void setUserConfig(String key, Object value) {
		// Set key locally
		if (value == null) {
			value = new String("");
			JsonUtil.debug("For key = " + key + ", value was null");
		}
		if (value instanceof String) {
			userConfiguration.put(key, (String) value);
		} else if (value instanceof Serializable) {
			userConfiguration.put(key, ((Serializable) value).toString());
		} else {
			JsonUtil.debug("Unable to serialize value");
		}

		if (Util.getProgramMode() == ProgramMode.STUBBED) {
			// STUBBED mode
		} else if (Util.getProgramMode() == ProgramMode.JSONRPC) {
			RequestBuilder builder = new RequestBuilder(
					RequestBuilder.POST,
					URL
							.encode(Util
									.getJsonRequest(
											"org.freemedsoftware.api.UserInterface.SetConfigValue",
											new String[] { key,
													JsonUtil.jsonify(value) })));
			try {
				builder.sendRequest(null, new RequestCallback() {
					public void onError(Request request, Throwable ex) {
						getToaster().addItem("CurrentState",
								"Failed to update configuration value.",
								Toaster.TOASTER_ERROR);
					}

					public void onResponseReceived(Request request,
							Response response) {
						if (200 == response.getStatusCode()) {
							getToaster().addItem("CurrentState",
									"Updated configuration value.",
									Toaster.TOASTER_INFO);
						} else {
							getToaster().addItem("CurrentState",
									"Failed to update configuration value.",
									Toaster.TOASTER_ERROR);
						}
					}
				});
			} catch (RequestException e) {
				getToaster().addItem("CurrentState",
						"Failed to update configuration value.",
						Toaster.TOASTER_ERROR);
			}

		} else {
			// GWT-RPC
		}
	}

	public static void retrieveUserConfiguration(boolean forceReload) {
		retrieveUserConfiguration(forceReload, null);
	}

	/**
	 * Pull user configuration settings into CurrentState object.
	 * 
	 * @param forceReload
	 */
	public static void retrieveUserConfiguration(boolean forceReload,
			final Command onLoad) {

		JsonUtil.debug("retrieveUserConfiguration called");

		if (userConfiguration == null || forceReload) {
			if (Util.getProgramMode() == ProgramMode.STUBBED) {
				// STUBBED mode
			} else if (Util.getProgramMode() == ProgramMode.JSONRPC) {
				RequestBuilder builder = new RequestBuilder(
						RequestBuilder.POST,
						URL
								.encode(Util
										.getJsonRequest(
												"org.freemedsoftware.api.UserInterface.GetEMRConfiguration",
												new String[] {})));
				try {
					builder.sendRequest(null, new RequestCallback() {
						public void onError(Request request, Throwable ex) {
						}

						@SuppressWarnings("unchecked")
						public void onResponseReceived(Request request,
								Response response) {
							if (200 == response.getStatusCode()
									&& !response.getText().contentEquals("[]")) {

								JsonUtil
										.debug("Retrieved good looking content");

								HashMap<String, String> r = (HashMap<String, String>) JsonUtil
										.shoehornJson(JSONParser.parse(response
												.getText()),
												"HashMap<String,String>");
								if (r != null) {
									JsonUtil
											.debug("successfully retrieved User Configuration");
									userConfiguration = r;

									if (onLoad != null) {
										onLoad.execute();
									}
								}
							} else {
								userConfiguration = new HashMap<String, String>();
								if (onLoad != null) {
									onLoad.execute();
								}
							}
						}
					});
				} catch (RequestException e) {
				}

			} else {
				// GWT-RPC
			}
		}
	}

}
