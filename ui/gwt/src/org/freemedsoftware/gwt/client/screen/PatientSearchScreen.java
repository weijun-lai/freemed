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

package org.freemedsoftware.gwt.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.freemedsoftware.gwt.client.JsonUtil;
import org.freemedsoftware.gwt.client.ScreenInterface;
import org.freemedsoftware.gwt.client.Util;
import org.freemedsoftware.gwt.client.Api.PatientInterfaceAsync;
import org.freemedsoftware.gwt.client.Util.ProgramMode;
import org.freemedsoftware.gwt.client.widget.CustomSortableTable;
import org.freemedsoftware.gwt.client.widget.PatientWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PatientSearchScreen extends ScreenInterface {

	protected CustomSortableTable sortableTable = null;

	protected Label sortableTableEmptyLabel = new Label();

	protected PatientWidget wSmartSearch = null;

	protected ListBox wFieldName = null;

	protected TextBox wFieldValue = null;

	protected HashMap<String, String> patientMap = null;

	public PatientSearchScreen() {
		final VerticalPanel verticalPanel = new VerticalPanel();
		initWidget(verticalPanel);

		final FlexTable flexTable = new FlexTable();
		verticalPanel.add(flexTable);
		flexTable.setWidth("100%");

		final Label smartSearchLabel = new Label("Smart Search : ");
		flexTable.setWidget(0, 0, smartSearchLabel);

		wSmartSearch = new PatientWidget();
		wSmartSearch.addChangeListener(new ChangeListener() {
			public void onChange(Widget w) {
				Integer val = ((PatientWidget) w).getValue();
				// Log.debug("Patient value = " + val.toString());
				try {
					if (val.compareTo(new Integer(0)) != 0) {
						spawnPatientScreen(val, wSmartSearch.getText());
						clearForm();
					}
				} catch (Exception e) {
					// Don't do anything if no patient is declared
					GWT.log("Caught exception", e);
				}
			}
		});
		addChildWidget(wSmartSearch);
		flexTable.setWidget(0, 1, wSmartSearch);

		final Label fieldSearchLabel = new Label("Field Search : ");
		flexTable.setWidget(1, 0, fieldSearchLabel);

		wFieldName = new ListBox();
		flexTable.setWidget(1, 1, wFieldName);
		wFieldName.setVisibleItemCount(1);
		wFieldName.addItem("Internal ID", "ptid");
		wFieldName.addItem("Social Security Number", "ssn");
		wFieldName.addItem("Drivers License", "dmv");
		wFieldName.addItem("Email Address", "email");
		wFieldName.addItem("City", "city");
		wFieldName.addItem("Zip/Postal Code", "zip");
		wFieldName.addItem("Home Phone", "hphone");
		wFieldName.addItem("Work Phone", "wphone");
		wFieldName.addItem("Age", "age");

		wFieldValue = new TextBox();
		flexTable.setWidget(2, 1, wFieldValue);
		wFieldValue.setWidth("100%");
		wFieldValue.addChangeListener(new ChangeListener() {
			public void onChange(Widget w) {
				refreshSearch();
			}
		});

		// Initialize patient mapping
		patientMap = new HashMap<String, String>();

		final HorizontalPanel horizontalPanel = new HorizontalPanel();
		verticalPanel.add(horizontalPanel);

		sortableTable = new CustomSortableTable();
		sortableTable.setWidth("100%");
		sortableTable.addColumn("Last Name", "last_name");
		sortableTable.addColumn("First Name", "first_name");
		sortableTable.addColumn("Middle", "middle_name");
		sortableTable.addColumn("Internal ID", "patient_id");
		sortableTable.addColumn("Date of Birth", "date_of_birth");
		sortableTable.addColumn("Age", "age");
		sortableTable.addTableListener(new TableListener() {
			public void onCellClicked(SourcesTableEvents ste, int row, int col) {
				// Get information on row...
				try {
					final Integer patientId = new Integer(sortableTable
							.getValueByRow(row));
					final String patientName = (String) sortableTable.getText(
							row, 0)
							+ ", " + (String) sortableTable.getText(row, 1);
					spawnPatientScreen(patientId, patientName);
				} catch (Exception e) {
					GWT.log("Caught exception: ", e);
				}
			}
		});

		final VerticalPanel stPanel = new VerticalPanel();
		stPanel.setWidth("100%");
		stPanel.add(sortableTable);
		stPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		sortableTableEmptyLabel.setStylePrimaryName("freemed-MessageText");
		sortableTableEmptyLabel
				.setText("No patients found with the specified criteria.");
		sortableTableEmptyLabel.setVisible(true);
		stPanel.add(sortableTableEmptyLabel);

		verticalPanel.add(stPanel);

		// Set visible focus *after* this is shown, otherwise it won't focus.
		try {
			wSmartSearch.setFocus(true);
		} catch (Exception e) {
			GWT.log("Caught exception: ", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void refreshSearch() {
		sortableTable.clear();
		clearSearchResults();
		patientMap.clear();
		if (Util.getProgramMode() == ProgramMode.STUBBED) {
			HashMap<String, String> a = new HashMap<String, String>();
			a.put("last_name", "Hackenbush");
			a.put("first_name", "Hugo");
			a.put("middle_name", "Z");
			a.put("patient_id", "HACK01");
			a.put("date_of_birth", "1979-08-10");
			a.put("age", "28");
			a.put("id", "1");
			List<HashMap<String, String>> l = new ArrayList<HashMap<String, String>>();
			l.add(a);
			sortableTable.loadData((HashMap<String, String>[]) l
					.toArray(new HashMap<?, ?>[0]));
		} else if (Util.getProgramMode() == ProgramMode.JSONRPC) {
			HashMap<String, String> criteria = new HashMap<String, String>();
			criteria.put(wFieldName.getValue(wFieldName.getSelectedIndex()),
					wFieldValue.getText());

			String[] params = { JsonUtil.jsonify(criteria) };
			RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
					URL.encode(Util.getJsonRequest(
							"org.freemedsoftware.api.PatientInterface.Search",
							params)));
			try {
				builder.sendRequest(null, new RequestCallback() {
					public void onError(Request request, Throwable ex) {
						Window.alert(ex.toString());
					}

					public void onResponseReceived(Request request,
							Response response) {
						if (Util.checkValidSessionResponse(response.getText(),
								state)) {
							if (200 == response.getStatusCode()) {
								HashMap<String, String>[] result = (HashMap<String, String>[]) JsonUtil
										.shoehornJson(JSONParser.parse(response
												.getText()),
												"HashMap<String,String>[]");
								if (result.length > 0) {
									sortableTableEmptyLabel.setVisible(false);
								} else {
									sortableTableEmptyLabel.setVisible(true);
								}
								sortableTable.loadData(result);
							} else {
								Window.alert(response.toString());
								sortableTableEmptyLabel.setVisible(true);
							}
						}
					}
				});
			} catch (RequestException e) {
				Window.alert(e.toString());
				sortableTableEmptyLabel.setVisible(true);
			}
		} else {
			PatientInterfaceAsync service = null;
			try {
				service = (PatientInterfaceAsync) Util
						.getProxy("org.freemedsoftware.gwt.client.Api.PatientInterface");
			} catch (Exception e) {
				GWT.log("Caught exception: ", e);
			}

			HashMap<String, String> criteria = new HashMap<String, String>();
			criteria.put(wFieldName.getValue(wFieldName.getSelectedIndex()),
					wFieldValue.getText());

			service.Search(criteria,
					new AsyncCallback<HashMap<String, String>[]>() {
						public void onSuccess(HashMap<String, String>[] result) {
							// Log.info("found " + new
							// Integer(r.length).toString() + "
							// results for Search");
							if (result.length > 0) {
								sortableTableEmptyLabel.setVisible(false);
							} else {
								sortableTableEmptyLabel.setVisible(true);
							}
							sortableTable.loadData(result);
						}

						public void onFailure(Throwable t) {
							sortableTableEmptyLabel.setVisible(true);
							// Log.error("Caught exception: ", t);
						}
					});
		}
	}

	/**
	 * Clear all contents of the SortableTable, since the generic clear() method
	 * does not clear set cell contents.
	 */
	public void clearSearchResults() {
		for (int rowIter = 0; rowIter < patientMap.size(); rowIter++) {
			for (int colIter = 0; colIter < 6; colIter++) {
				sortableTable.clearCell(rowIter, colIter);
			}
		}
	}

	/**
	 * Set contents of numbered output row.
	 * 
	 * @param res
	 *            HashMap containing return values from
	 *            PatientInterfaceAsync.Search() method
	 * @param rowIndex
	 *            Index row, starting at 0 for any results.
	 */
	public void setResultRow(HashMap<String, String> res, int rowIndex) {
		try {
			sortableTable.setValue(rowIndex + 1, 0, (String) res
					.get("last_name"));
			sortableTable.setValue(rowIndex + 1, 1, (String) res
					.get("first_name"));
			sortableTable.setValue(rowIndex + 1, 2, (String) res
					.get("middle_name"));
			sortableTable.setValue(rowIndex + 1, 3, (String) res
					.get("patient_id"));
			sortableTable.setValue(rowIndex + 1, 4, (String) res
					.get("date_of_birth"));
			sortableTable.setValue(rowIndex + 1, 5, (String) res.get("age"));
		} catch (NullPointerException npe) {
			GWT.log("NPE caught: ", npe);
		}

		// Add value to lookup table, by patient id (supposedly unique field)
		patientMap.put((String) res.get("patient_id"), (String) res.get("id"));
	}

	/**
	 * Create new tab for patient.
	 * 
	 * @param patient
	 */
	public void spawnPatientScreen(Integer patient, String patientName) {
		PatientScreen s = new PatientScreen();
		s.setPatient(patient);
		GWT.log("Spawn patient screen with patient = " + patient.toString(),
				null);
		Util.spawnTab(patientName, s, state);
	}

	public void clearForm() {
		wSmartSearch.clear();
		wFieldValue.setText("");
	}

}
