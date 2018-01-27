package org.lechisoft.minifw.mybaits;

import org.lechisoft.minifw.log.MiniLog;

public class Pageable implements Cloneable {

	public Object clone() {
		Object o = null;
		try {
			o = (Object) super.clone();
		} catch (CloneNotSupportedException e) {
			MiniLog.error("clone " + this.getClass().getName() + " failed.", e);
		}
		return o;
	}

	private Paging paging = new Paging();

	public Paging getPaging() {
		return paging;
	}

	public void setPaging(Paging paging) {
		this.paging = paging;
	}

}
