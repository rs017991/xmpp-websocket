package com.example.demo.pojo;

public class User implements Comparable<User> {
	private String jid;
	private String name;
	private String presence;

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPresence() {
		return presence;
	}

	public void setPresence(String presence) {
		this.presence = presence;
	}

	@Override
	public int compareTo(User o) {
		return name.compareTo(o.getName());
	}
}
