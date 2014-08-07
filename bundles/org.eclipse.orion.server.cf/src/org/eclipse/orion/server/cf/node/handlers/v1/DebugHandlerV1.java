/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.node.handlers.v1;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.*;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.internal.server.servlets.ServletResourceHandler;
import org.eclipse.orion.server.cf.CFProtocolConstants;
import org.eclipse.orion.server.cf.commands.*;
import org.eclipse.orion.server.cf.jobs.CFJob;
import org.eclipse.orion.server.cf.manifest.v2.ManifestParseTree;
import org.eclipse.orion.server.cf.node.objects.Debug;
import org.eclipse.orion.server.cf.objects.App;
import org.eclipse.orion.server.cf.objects.Target;
import org.eclipse.orion.server.cf.servlets.AbstractRESTHandler;
import org.eclipse.orion.server.core.IOUtilities;
import org.eclipse.orion.server.core.ServerStatus;
import org.eclipse.osgi.util.NLS;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugHandlerV1 extends AbstractRESTHandler<Debug> {

	private final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$
	private App app;

	public DebugHandlerV1(ServletResourceHandler<IStatus> statusHandler) {
		super(statusHandler);
	}

	@Override
	protected Debug buildResource(HttpServletRequest request, String path) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected CFJob handleGet(Debug resource, HttpServletRequest request, HttpServletResponse response, final String path) {
		final String encodedContentLocation = IOUtilities.getQueryParameter(request, ProtocolConstants.KEY_CONTENT_LOCATION);
		String contentLocation = null;
		if (encodedContentLocation != null) {
			try {
				contentLocation = ServletResourceHandler.toOrionLocation(request, URLDecoder.decode(encodedContentLocation, "UTF8"));
			} catch (UnsupportedEncodingException e) {
				// do nothing
			}
		}
		final String finalContentLocation = contentLocation;
		final JSONObject targetJSON = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_TARGET));

		return new CFJob(request, false) {
			@Override
			protected IStatus performJob() {
				try {
					ComputeTargetCommand computeTarget = new ComputeTargetCommand(this.userId, targetJSON);
					IStatus result = computeTarget.doIt();
					if (!result.isOK())
						return result;
					Target target = computeTarget.getTarget();

					if (finalContentLocation == null)
						return new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Expected ContentLocation to be provided", null);

					// Create an App
					result = DebugHandlerV1.this.createTempApp(target, this.userId, finalContentLocation);
					if (!result.isOK())
						return result;
					App app = getTempApp();

					// parse the application manifest
					String manifestAppName = null;
					ParseManifestCommand parseManifestCommand = new ParseManifestCommand(target, this.userId, finalContentLocation);
					result = parseManifestCommand.doIt();
					if (!result.isOK())
						return result;

					// TODO the actual GetAppInstrumentationState command here
					if (true)
						throw new RuntimeException("Finish me");
					return null;
				} catch (Exception e) {
					String msg = NLS.bind("Failed to handle request for {0}", path); //$NON-NLS-1$
					ServerStatus status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
					logger.error(msg, e);
					return status;
				}
			}
		};
	}

	// Creates a temporary App to encapsulate Manifest and AppStore
	private IStatus createTempApp(Target target, String userId, String contentLocation) {
		ParseManifestCommand parseManifestCommand = null;
		parseManifestCommand = new ParseManifestCommand(target, userId, contentLocation);
		IStatus status = parseManifestCommand.doIt();
		if (!status.isOK())
			return status;

		// Create app
		app = new App();
		app.setName(contentLocation);
		app.setManifest(parseManifestCommand.getManifest());
		app.setAppStore(parseManifestCommand.getAppStore());
		return new ServerStatus(IStatus.OK, HttpServletResponse.SC_OK, null, app.getAppJSON(), null);
	}

	private App getTempApp() {
		return this.app;
	}

	@Override
	protected CFJob handlePut(Debug resource, HttpServletRequest request, HttpServletResponse response, final String pathString) {
		final JSONObject targetJSON2 = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_TARGET));

		IPath path = pathString != null ? new Path(pathString) : new Path("");
		boolean addRoute = "routes".equals(path.segment(1));

		final JSONObject jsonData = extractJSONData(request);
		final JSONObject targetJSON = jsonData.optJSONObject(CFProtocolConstants.KEY_TARGET);

		final String state = jsonData.optString(CFProtocolConstants.KEY_STATE, null);
		final String appName = jsonData.optString(CFProtocolConstants.KEY_NAME, null);
		final String contentLocation = ServletResourceHandler.toOrionLocation(request, jsonData.optString(CFProtocolConstants.KEY_CONTENT_LOCATION, null));

		/* default application startup is one minute */
		int userTimeout = jsonData.optInt(CFProtocolConstants.KEY_TIMEOUT, 60);
		final int timeout = (userTimeout > 0) ? userTimeout : 0;

		/* TODO: The force shouldn't be always with us */
		final boolean force = jsonData.optBoolean(CFProtocolConstants.KEY_FORCE, true);

		return new CFJob(request, false) {
			@Override
			protected IStatus performJob() {
				try {
					ComputeTargetCommand computeTarget = new ComputeTargetCommand(this.userId, targetJSON);
					IStatus status = computeTarget.doIt();
					if (!status.isOK())
						return status;
					Target target = computeTarget.getTarget();

					/* parse the application manifest */
					String manifestAppName = null;
					ParseManifestCommand parseManifestCommand = null;
					if (contentLocation != null) {
						parseManifestCommand = new ParseManifestCommand(target, this.userId, contentLocation);
						status = parseManifestCommand.doIt();
						if (!status.isOK())
							return status;

						/* get the manifest name */
						ManifestParseTree manifest = parseManifestCommand.getManifest();
						if (manifest != null) {
							ManifestParseTree applications = manifest.get(CFProtocolConstants.V2_KEY_APPLICATIONS);
							if (applications.getChildren().size() > 0)
								manifestAppName = applications.get(0).get(CFProtocolConstants.V2_KEY_NAME).getValue();
						}
					}

					GetAppCommand getAppCommand = new GetAppCommand(target, appName != null ? appName : manifestAppName);
					status = getAppCommand.doIt();
					App app = getAppCommand.getApp();

					if (CFProtocolConstants.KEY_STARTED.equals(state)) {
						if (!status.isOK())
							return status;
						return new StartAppCommand(target, app, timeout).doIt();
					} else if (CFProtocolConstants.KEY_STOPPED.equals(state)) {
						if (!status.isOK())
							return status;
						return new StopAppCommand(target, app).doIt();
					} else {
						if (parseManifestCommand == null) {
							String msg = NLS.bind("Failed to handle request for {0}", pathString); //$NON-NLS-1$
							status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, null);
							logger.error(msg);
							return status;
						}
					}

					// push new application
					if (app == null)
						app = new App();

					app.setName(appName != null ? appName : manifestAppName);
					app.setManifest(parseManifestCommand.getManifest());
					app.setAppStore(parseManifestCommand.getAppStore());

					status = new PushAppCommand(target, app, force).doIt();
					if (!status.isOK())
						return status;

					// get the app again
					getAppCommand = new GetAppCommand(target, app.getName());
					getAppCommand.doIt();
					app = getAppCommand.getApp();
					app.setManifest(parseManifestCommand.getManifest());

					new StartAppCommand(target, app).doIt();

					return status;
				} catch (Exception e) {
					String msg = NLS.bind("Failed to handle request for {0}", pathString); //$NON-NLS-1$
					ServerStatus status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
					logger.error(msg, e);
					return status;
				}
			}
		};
	}

	@Override
	protected CFJob handleDelete(Debug resource, HttpServletRequest request, HttpServletResponse response, final String pathString) {
		final JSONObject targetJSON = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_TARGET));

		IPath path = new Path(pathString);

		return new CFJob(request, false) {
			@Override
			protected IStatus performJob() {
				try {
					ComputeTargetCommand computeTarget = new ComputeTargetCommand(this.userId, targetJSON);
					IStatus status = computeTarget.doIt();
					if (!status.isOK())
						return status;
					Target target = computeTarget.getTarget();

					// TODO call Uninstrument here
					if (true)
						throw new RuntimeException("TODO finish " + this.getClass());
					//return uninstrument.doIt();
					return null;

				} catch (Exception e) {
					String msg = NLS.bind("Failed to handle request for {0}", pathString); //$NON-NLS-1$
					ServerStatus status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
					logger.error(msg, e);
					return status;
				}
			}
		};
	}
}
