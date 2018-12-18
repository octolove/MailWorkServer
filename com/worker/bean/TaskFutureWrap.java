package com.worker.bean;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * 包装异步结果future和参数对象dto
 * @author JC
 *
 */
public class TaskFutureWrap implements Serializable {

	private static final long serialVersionUID = 1941686312178619568L;
	//调用workr提交的数据对象
	private ResultDTO dto = null;
	//线程异步提交的结果集
	private Future<TaskBean[]> future = null;

	public TaskFutureWrap() {
		super();
	}
	
	/**
	 * 
	 * @param dto
	 * @param future
	 */
	public TaskFutureWrap(ResultDTO dto, Future<TaskBean[]> future) {
		super();
		this.dto = dto;
		this.future = future;
	}
	
	/**
	 * @return
	 * 	ResultDTO
	 */
	public ResultDTO getDto() {
		return dto;
	}
	
	/**
	 * 
	 * @param dto
	 */
	public void setDto(ResultDTO dto) {
		this.dto = dto;
	}
	
	/**
	 * @return
	 * 	Future<TaskBean[]>
	 */
	public Future<TaskBean[]> getFuture() {
		return future;
	}
	
	/**
	 * 
	 * @param future
	 */
	public void setFuture(Future<TaskBean[]> future) {
		this.future = future;
	}

}
