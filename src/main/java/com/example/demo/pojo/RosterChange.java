package com.example.demo.pojo;

import java.util.Collection;
import java.util.Map;

public class RosterChange {
	private Map<String, String> presenceChanged;
	private Collection<User> entriesAdded;
	private Collection<String> entriesUpdated;
	private Collection<String> entriesDeleted;

	public Map<String, String> getPresenceChanged() {
		return presenceChanged;
	}

	public void setPresenceChanged(Map<String, String> presenceChanged) {
		this.presenceChanged = presenceChanged;
	}

	public Collection<User> getEntriesAdded() {
		return entriesAdded;
	}

	public void setEntriesAdded(Collection<User> entriesAdded) {
		this.entriesAdded = entriesAdded;
	}

	public Collection<String> getEntriesUpdated() {
		return entriesUpdated;
	}

	public void setEntriesUpdated(Collection<String> entriesUpdated) {
		this.entriesUpdated = entriesUpdated;
	}

	public Collection<String> getEntriesDeleted() {
		return entriesDeleted;
	}

	public void setEntriesDeleted(Collection<String> entriesDeleted) {
		this.entriesDeleted = entriesDeleted;
	}
}
