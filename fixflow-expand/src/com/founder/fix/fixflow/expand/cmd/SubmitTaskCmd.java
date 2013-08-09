/**
 * Copyright 1996-2013 Founder International Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author kenshin
 */
package com.founder.fix.fixflow.expand.cmd;

import java.util.List;

import com.founder.fix.fixflow.core.exception.FixFlowException;
import com.founder.fix.fixflow.core.factory.ProcessObjectFactory;
import com.founder.fix.fixflow.core.impl.bpmn.behavior.ProcessDefinitionBehavior;
import com.founder.fix.fixflow.core.impl.bpmn.behavior.TaskCommandInst;
import com.founder.fix.fixflow.core.impl.bpmn.behavior.UserTaskBehavior;
import com.founder.fix.fixflow.core.impl.cmd.AbstractExpandTaskCmd;
import com.founder.fix.fixflow.core.impl.expression.ExpressionMgmt;
import com.founder.fix.fixflow.core.impl.identity.Authentication;
import com.founder.fix.fixflow.core.impl.interceptor.CommandContext;
import com.founder.fix.fixflow.core.impl.persistence.ProcessDefinitionManager;
import com.founder.fix.fixflow.core.impl.persistence.ProcessInstanceManager;
import com.founder.fix.fixflow.core.impl.persistence.TaskManager;
import com.founder.fix.fixflow.core.impl.runtime.ProcessInstanceEntity;
import com.founder.fix.fixflow.core.impl.runtime.TokenEntity;
import com.founder.fix.fixflow.core.impl.task.TaskInstanceEntity;
import com.founder.fix.fixflow.core.impl.util.StringUtil;
import com.founder.fix.fixflow.core.runtime.ExecutionContext;
import com.founder.fix.fixflow.core.task.TaskInstance;
import com.founder.fix.fixflow.expand.command.SubmitTaskCommand;

public class SubmitTaskCmd extends AbstractExpandTaskCmd<SubmitTaskCommand, Void> {
	


	public SubmitTaskCmd(SubmitTaskCommand submitTaskCommand) {
		super(submitTaskCommand);

	}

	public Void execute(CommandContext commandContext) {
		
		if (taskId == null||taskId.equals("")) {
			throw new FixFlowException("任务编号为空！");
		}
		
		TaskManager taskManager = commandContext.getTaskManager();

		TaskInstance taskInstanceQuery = taskManager.findTaskById(taskId);

		String tokenId = taskInstanceQuery.getTokenId();
		String nodeId = taskInstanceQuery.getNodeId();
		String processDefinitionId = taskInstanceQuery.getProcessDefinitionId();
		ProcessInstanceManager processInstanceManager = commandContext.getProcessInstanceManager();

		String processInstanceId = taskInstanceQuery.getProcessInstanceId();

		ProcessDefinitionManager processDefinitionManager = commandContext.getProcessDefinitionManager();

		ProcessDefinitionBehavior processDefinition = processDefinitionManager.findLatestProcessDefinitionById(processDefinitionId);

		UserTaskBehavior userTask = (UserTaskBehavior) processDefinition.getDefinitions().getElement(nodeId);

		TaskCommandInst taskCommand = userTask.getTaskCommandsMap().get(userCommandId);

		ProcessInstanceEntity processInstanceImpl = processInstanceManager.findProcessInstanceById(processInstanceId, processDefinition);

		processInstanceImpl.getContextInstance().addTransientVariable("fixVariable_userCommand", userCommandId);

		//processInstanceImpl.getContextInstance().setVariableMap(variables);

		// processInstanceImpl.setBizKey(this.businessKey);

		TokenEntity token = processInstanceImpl.getTokenMap().get(tokenId);

		processInstanceImpl.getContextInstance().setTransientVariableMap(transientVariables);
		processInstanceImpl.getContextInstance().setVariableMap(variables);

		ExecutionContext executionContext = ProcessObjectFactory.FACTORYINSTANCE.createExecutionContext(token);
		if(businessKey!=null){
			processInstanceImpl.setBizKeyWithoutCascade(this.businessKey);
		}
		
		if(initiator!=null){
			processInstanceImpl.setInitiatorWithoutCascade(this.initiator);
		}
		
		if (processDefinition.getTaskSubject() != null && processDefinition.getTaskSubject().getExpressionValue() != null) {

			Object result = ExpressionMgmt.execute(token.getProcessInstance().getProcessDefinition().getTaskSubject().getExpressionValue(),
					executionContext);

			if (result != null) {
				processInstanceImpl.setSubject(result.toString());
			}
		} else {
			processInstanceImpl.setSubject(processDefinition.getName());
		}

		

		List<TaskInstanceEntity> taskInstances = processInstanceImpl.getTaskMgmtInstance().getTaskInstanceEntitys();

		TaskInstanceEntity taskInstanceImpl = null;

		for (TaskInstanceEntity taskInstance : taskInstances) {
			if (taskInstance.getId().equals(taskId)) {

				taskInstanceImpl = taskInstance;

			}
		}
		
		if (taskCommand != null && taskCommand.getExpression() != null) {
			try {
				executionContext.setTaskInstance(taskInstanceImpl);
				ExpressionMgmt.execute(taskCommand.getExpression(), executionContext);
			} catch (Exception e) {
				throw new FixFlowException("用户命令表达式执行异常!", e);
			}
		}

		if (taskInstanceImpl != null) {
			if(this.agent!=null&&!this.agent.equals("")){
				taskInstanceImpl.setAgent(Authentication.getAuthenticatedUserId());
				taskInstanceImpl.setAssigneeWithoutCascade(this.agent);
			}else{
				taskInstanceImpl.setAssigneeWithoutCascade(Authentication.getAuthenticatedUserId());
				taskInstanceImpl.setAgent(null);
			}
			taskInstanceImpl.end();
			taskInstanceImpl.setCommandId(taskCommand.getId());
			taskInstanceImpl.setCommandType(StringUtil.getString(taskCommand.getTaskCommandType()));
			taskInstanceImpl.setCommandMessage(taskCommand.getName());
			taskInstanceImpl.setTaskComment(this.taskComment);
			
			
		} else {
			throw new FixFlowException("没有找到id为: " + taskId + " 的任务");
		}

		try {

			processInstanceManager.saveProcessInstance(processInstanceImpl);
		} catch (Exception e) {
			throw new FixFlowException("流程实例持久化失败!", e);
		}
		return null;
	}

}