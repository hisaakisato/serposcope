package com.serphacker.serposcope.util;

import com.serphacker.serposcope.models.base.Group;

public abstract class TaskRescanRunnable implements Runnable {

	private Group group;

	public TaskRescanRunnable(Group group) {
			this.group = group;
		}

	public Group getGroup() {
		return group;
	}
}
