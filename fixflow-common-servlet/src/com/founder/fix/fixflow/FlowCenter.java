/**
 *  Copyright 1996-2013 Founder International Co.,Ltd.
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
 * @author shao
 */
package com.founder.fix.fixflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.founder.fix.fixflow.core.impl.bpmn.behavior.TaskCommandInst;
import com.founder.fix.fixflow.core.impl.util.StringUtil;
import com.founder.fix.fixflow.service.FlowCenterService;
import com.founder.fix.fixflow.util.CurrentThread;
import com.founder.fix.fixflow.util.SpringConfigLoadHelper;

/**
 * Servlet implementation class FlowCenter
 */
public class FlowCenter extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FlowCenter() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("ISGET", "ISGET");
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		CurrentThread.init();
		ServletOutputStream out = null;
		String action = StringUtil.getString(request.getParameter("action"));
		if (StringUtil.isEmpty(action)) {
			action = StringUtil.getString(request.getAttribute("action"));
		}
		RequestDispatcher rd = null;
		try {
			Map<String, Object> filter = new HashMap<String, Object>();
			// Enumeration enums = request.getParameterNames();
			// while (enums.hasMoreElements()) {
			// String paramName = (String) enums.nextElement();
			//
			// String paramValue = request.getParameter(paramName);
			//
			// // 形成键值对应的map
			// filter.put(paramName, paramValue);
			//
			// }
			if (ServletFileUpload.isMultipartContent(request)) {
				ServletFileUpload Uploader = new ServletFileUpload(
						new DiskFileItemFactory());
				// Uploader.setSizeMax("); // 设置最大文件尺寸
				Uploader.setHeaderEncoding("utf-8");
				List<FileItem> fileItems = Uploader.parseRequest(request);
				for (FileItem item : fileItems) {
					filter.put(item.getFieldName(), item);
					if(item.getFieldName().equals("action"))
						action = item.getString();
				}
			} else {
				Enumeration enu = request.getParameterNames();
				while (enu.hasMoreElements()) {
					Object tmp = enu.nextElement();
					Object obj = request
							.getParameter(StringUtil.getString(tmp));

					if (request.getAttribute("ISGET") != null)
						obj = new String(obj.toString().getBytes("ISO8859-1"),
								"utf-8");

					filter.put(StringUtil.getString(tmp), obj);
				}
			}

			Enumeration attenums = request.getAttributeNames();
			while (attenums.hasMoreElements()) {
				String paramName = (String) attenums.nextElement();

				Object paramValue = request.getAttribute(paramName);

				// 形成键值对应的map
				filter.put(paramName, paramValue);

			}

			String userId = StringUtil.getString(request.getSession()
					.getAttribute(FlowCenterService.LOGIN_USER_ID));
			filter.put("userId", userId);
			if (action.equals("getMyProcess")) {
				List<Map<String, String>> result = getFlowCenter()
						.queryStartProcess(userId);
				request.setAttribute("result", result);
				request.setAttribute("userId", userId); //返回userId add Rex
				rd = request.getRequestDispatcher("startTask.jsp");
			} else if (action.equals("getMyTask")) {
				Map<String, Object> pageResult = getFlowCenter()
						.queryMyTaskNotEnd(filter);
				filter.putAll(pageResult);
				request.setAttribute("result", filter);
				rd = request.getRequestDispatcher("/todoTask.jsp");
			} else if (action.equals("getProcessImage")) {
				response.getOutputStream();
			} else if (action.equals("getInitorTask")) {
				Map<String, Object> pageResult = getFlowCenter()
						.queryTaskInitiator(filter);
				filter.putAll(pageResult);
				request.setAttribute("result", filter);
				rd = request.getRequestDispatcher("/queryTask.jsp");
			} else if (action.equals("getParticipantsTask")) {
				Map<String, Object> pageResult = getFlowCenter()
						.queryTaskParticipants(filter);
				filter.putAll(pageResult);
				request.setAttribute("result", filter);
				rd = request.getRequestDispatcher("/queryTask.jsp");
			} else if (action.equals("getTaskDetailInfo")) {
				Map<String, Object> pageResult = getFlowCenter()
						.getTaskDetailInfo(filter);
				filter.putAll(pageResult);
				request.setAttribute("result", filter);
				rd = request.getRequestDispatcher("/flowGraphic.jsp");
			} else if (action.equals("getFlowGraph")) {
				InputStream is = getFlowCenter().getFlowGraph(filter);
				out = response.getOutputStream();
				response.setContentType("application/octet-stream;charset=UTF-8");
				byte[] buff = new byte[2048];
				int size = 0;
				while (is != null && (size = is.read(buff)) != -1) {
					out.write(buff, 0, size);
				}
			} else if (action.equals("getUserInfo")) {
				filter.put("path", request.getSession().getServletContext().getRealPath("/"));
				Map<String, Object> pageResult = getFlowCenter().getUserInfo(filter);
				filter.putAll(pageResult);
				request.setAttribute("result", filter);
				rd = request.getRequestDispatcher("/userOperation.jsp");
			} else if (action.equals("updateUserIcon")) {
				filter.put("path", request.getSession().getServletContext().getRealPath("/"));
				getFlowCenter().saveUserIcon(filter);
				rd = request.getRequestDispatcher("/FlowCenter?action=getMyProcess");
			} else if(action.equals("startOneTask")){  //仅实现获取按钮功能  add by Rex
				filter.put("path", request.getSession().getServletContext().getRealPath("/"));
				List<TaskCommandInst> list = getFlowCenter().getSubTaskTaskCommandByKey(filter);
				request.setAttribute("result", list);
				rd = request.getRequestDispatcher("/startOneTask.jsp");
			} else if(action.equals("doTask")){
				filter.put("path", request.getSession().getServletContext().getRealPath("/"));
				List<TaskCommandInst> list = getFlowCenter().GetTaskCommandByTaskId(filter);
				request.setAttribute("result", list);
				rd = request.getRequestDispatcher("/doTask.jsp");
			}
			if (rd != null)
				rd.forward(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
			CurrentThread.clear();
		}
	}

	public FlowCenterService getFlowCenter() {
		return (FlowCenterService) SpringConfigLoadHelper
				.getBean("flowCenterServiceImpl");
	}

}