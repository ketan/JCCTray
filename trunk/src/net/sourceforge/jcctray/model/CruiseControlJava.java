/*******************************************************************************
 *  Copyright 2007 Ketan Padegaonkar http://ketan.padegaonkar.name
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/
package net.sourceforge.jcctray.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An implementation of {@link ICruise} that connects to CruiseControl (<a
 * href="http://cruisecontrol.sourceforge.net">http://cruisecontrol.sourceforge.net</a>)
 * 
 * @author Ketan Padegaonkar
 */
public class CruiseControlJava extends HTTPCruise implements ICruise {

	private static final Locale	LOCALE_US	= Locale.US;
	private static final SimpleDateFormat	DATE_FORMATTER	= new SimpleDateFormat("h:mm:ss a, dd MMM", LOCALE_US);
	private static final SimpleDateFormat	DATE_PARSER		= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LOCALE_US);
	
	protected String forceBuildURL(DashBoardProject project) {
		String hostName = project.getHost().getHostName();
		URL url = null;
		try {
			url = new URL(hostName);
			return url.getProtocol() + "://" + url.getHost() + ":" + getForceBuildPort(project.getHost()) + url.getPath().replaceAll("/*$", "")
					+ "/invoke?operation=build&objectname=CruiseControl+Project%3Aname%3D" + project.getName();
		} catch (MalformedURLException e) {
			getLog().error("The url was malformed: " + url, e);
		}
		return null;
	}
	
	protected String getForceBuildPort(Host host) {
		return Integer.getInteger("forcebuild." + host.getHostString() + ".port", 8000).toString();
	}

	public String formatDate(String date, TimeZone timeZone) {
		try {
			return DATE_FORMATTER.format(DATE_PARSER.parse(date));
		} catch (ParseException e) {
			getLog().error("Could not parse date: " + date);
		}
		return date;
	}

	public String getName() {
		return "CruiseControl";
	}

	protected String getSuccessMessage(DashBoardProject project) {
		return "Invocation successful";
	}

	protected String getXmlReportURL(Host host) {
		return host.getHostName().replaceAll("/*$", "") + "/xml";
	}

}
