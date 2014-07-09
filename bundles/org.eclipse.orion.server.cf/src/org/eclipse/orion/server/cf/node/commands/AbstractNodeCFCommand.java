package org.eclipse.orion.server.cf.node.commands;

import org.eclipse.orion.server.cf.commands.AbstractCFCommand;
import org.eclipse.orion.server.cf.objects.App;
import org.eclipse.orion.server.cf.objects.Target;

public abstract class AbstractNodeCFCommand extends AbstractCFCommand {

	protected App app;

	protected AbstractNodeCFCommand(Target target, App app) {
		super(target);
		this.app = app;
	}

}
