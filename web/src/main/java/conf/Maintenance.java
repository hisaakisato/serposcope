package conf;

import com.google.inject.Singleton;

@Singleton
public class Maintenance {

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
