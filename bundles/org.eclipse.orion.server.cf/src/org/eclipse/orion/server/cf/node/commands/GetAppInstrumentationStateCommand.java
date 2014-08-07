package org.eclipse.orion.server.cf.node.commands;

import org.eclipse.orion.server.cf.objects.App;
import org.eclipse.orion.server.cf.objects.Target;
import org.eclipse.orion.server.core.ServerStatus;

public class GetAppInstrumentationStateCommand extends AbstractNodeCFCommand {

	protected GetAppInstrumentationStateCommand(Target target, App app) {
		super(target, app);
	}

	@Override
	protected ServerStatus _doIt() {
		// TODO Auto-generated method stub
		return null;
	}

}
