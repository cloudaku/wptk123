package org.eclipse.orion.server.cf.node.commands;

import java.text.MessageFormat;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.orion.server.cf.manifest.v2.InvalidAccessException;
import org.eclipse.orion.server.cf.node.CFNodeJSConstants;
import org.eclipse.orion.server.cf.node.objects.PackageJSON;
import org.eclipse.orion.server.cf.objects.App;
import org.eclipse.orion.server.cf.objects.Target;
import org.eclipse.orion.server.cf.utils.MultiServerStatus;
import org.eclipse.orion.server.core.ServerStatus;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instruments a Node.js application for debugging.
 */
public class InstrumentNodeAppCommand extends AbstractNodeCFCommand {
	private final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$

	private String commandName;
	private String password;
	private String urlPrefix;

	public InstrumentNodeAppCommand(Target target, App app, String password, String urlPrefix) {
		super(target, app);
		this.commandName = NLS.bind("Instrument app {0} for debug", new String[] {app.getName()});
		this.password = password;
		this.urlPrefix = urlPrefix == null ? "" : urlPrefix;
	}

	@Override
	protected ServerStatus _doIt() {
		MultiServerStatus status = new MultiServerStatus();

		// Get the package.json file
		GetAppPackageJSONCommand getPackageJsonCommand = new GetAppPackageJSONCommand(target, app);
		ServerStatus jobStatus = (ServerStatus) getPackageJsonCommand.doIt(); /* FIXME: unsafe type cast */
		status.add(jobStatus);
		if (!jobStatus.isOK())
			return status;
		PackageJSON packageJSON = getPackageJsonCommand.getPackageJSON();

		// Check permissions to app ContentLocation

		// Find the app start command
		GetAppStartCommand getStartCommand = new GetAppStartCommand(target, app, packageJSON);
		jobStatus = (ServerStatus) getStartCommand.doIt(); /* FIXME: unsafe type cast */
		status.add(jobStatus);
		if (!jobStatus.isOK())
			return status;

		try {
			// Create the modified manifest and package.json
			String modifiedStartCommand = this.getDebugStartCommand(getStartCommand.getCommand());
			PackageJSON modifiedPackageJSON = getModifiedPackageJSON(packageJSON, modifiedStartCommand);
			String modifiedManifest = getModifiedManifest(modifiedStartCommand);
			// TODO if a Procfile exists we should modify it as well

			// Write the modified files to the app folder
			//			File file = PackageUtils.getApplicationPackage(app.getAppStore());
			//			File modifiedFile = getModifiedZip(file, modifiedPackageJSON, modifiedManifest);
			//			file.delete(); // delete the old one

			return status;
		} catch (Exception e) {
			String msg = NLS.bind("An exception occurred while performing operation {0}", commandName);
			logger.error(msg, e);
			status.add(new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e));
			return status;
		}
	}

	private PackageJSON getModifiedPackageJSON(PackageJSON originalPackage, String modifiedStartCommand) {
		JSONObject original = originalPackage.getJSON();
		try {
			JSONObject modified = new JSONObject(original, JSONObject.getNames(original));

			// Add dependency
			JSONObject deps = modified.optJSONObject(CFNodeJSConstants.KEY_NPM_DEPENDENCIES);
			if (deps == null)
				deps = new JSONObject();
			deps.put(CFNodeJSConstants.KEY_CF_LAUNCHER_PACKAGE, CFNodeJSConstants.VALUE_CF_LAUNCHER_VERSION);
			modified.put(CFNodeJSConstants.KEY_NPM_DEPENDENCIES, deps);

			// Add or update npm start script
			JSONObject scripts = modified.optJSONObject(CFNodeJSConstants.KEY_NPM_SCRIPTS);
			if (scripts == null)
				scripts = new JSONObject();
			scripts.put(CFNodeJSConstants.KEY_NPM_SCRIPTS_START, modifiedStartCommand);
			modified.put(CFNodeJSConstants.KEY_NPM_SCRIPTS, scripts);
			return new PackageJSON(modified);
		} catch (JSONException e) {
			// Cannot happen
			return null;
		}
	}

	private String getModifiedManifest(String modifiedStartCommand) throws InvalidAccessException {
		// TODO create setters on ManifestParseTree. Then clone app.getManifest() and call setters instead
		String manifest = app.getManifest().toString();
		return manifest.replaceFirst(" command: .+$", " command: " + modifiedStartCommand);
	}

	private String getDebugStartCommand(String command) {
		return MessageFormat.format(CFNodeJSConstants.CF_LAUNCHER_COMMAND_FORMAT, password, urlPrefix, command);
	}

}
