/* *******************************************
 * Copyright (c) 2011
 * HT srl,   All rights reserved.
 * Project      : RCS, AndroidService
 * File         : StopAgentAction.java
 * Created      : 6-mag-2011
 * Author		: zeno
 * *******************************************/

package com.android.service.action;

import com.android.service.agent.AgentManager;
import com.android.service.auto.Cfg;
import com.android.service.util.Check;

public class StopAgentAction extends AgentAction {
	public StopAgentAction(int type, byte[] confParams) {
		super(type, confParams);
	}

	private static final String TAG = "StopAgentAction";

	@Override
	public boolean execute() {
		if(Cfg.DEBUG) Check.log( TAG + " (execute): " + agentId);
		final AgentManager agentManager = AgentManager.self();

		agentManager.stop(agentId);
		return true;
	}

}