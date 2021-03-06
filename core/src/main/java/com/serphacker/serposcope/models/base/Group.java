/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.models.base;


public class Group {
    
    public enum Module {
        GOOGLE,
        TWITTER,
        GITHUB;
        
        public static Module getByOrdinal(Integer moduleId){
            if(moduleId == null || moduleId < 0 || moduleId >= Module.values().length ){
                return null;
            }

            return values()[moduleId];
        }
    }

    int id;
    Module module;
    String name;
    User owner;
    boolean shared;
    boolean sundayEnabled;
    boolean mondayEnabled;
    boolean tuesdayEnabled;
    boolean WednesdayEnabled;
    boolean thursdayEnabled;
    boolean fridayEnabled;
    boolean saturdayEnabled;
    Integer targets;
    Integer searches;

    public Group(int id, Module module, String name) {
        this.id = id;
        this.module = module;
        this.name = name;
    }

    public Group(Module module, String name) {
        this.module = module;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public boolean isOwner(User user) {
    	if (user == null) {
    		return false;
    	}
    	return isOwner(user.getId());    	
    }

    public boolean isOwner(int userId) {
    	if (this.owner == null) {
    		return false;
    	}
    	return userId == this.owner.getId();    	
    }

    public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

    public boolean isSundayEnabled() {
		return sundayEnabled;
	}

	public void setSundayEnabled(boolean sundayEnabled) {
		this.sundayEnabled = sundayEnabled;
	}

	public boolean isMondayEnabled() {
		return mondayEnabled;
	}

	public void setMondayEnabled(boolean mondayEnabled) {
		this.mondayEnabled = mondayEnabled;
	}

	public boolean isTuesdayEnabled() {
		return tuesdayEnabled;
	}

	public void setTuesdayEnabled(boolean tuesdayEnabled) {
		this.tuesdayEnabled = tuesdayEnabled;
	}

	public boolean isWednesdayEnabled() {
		return WednesdayEnabled;
	}

	public void setWednesdayEnabled(boolean wednesdayEnabled) {
		WednesdayEnabled = wednesdayEnabled;
	}

	public boolean isThursdayEnabled() {
		return thursdayEnabled;
	}

	public void setThursdayEnabled(boolean thursdayEnabled) {
		this.thursdayEnabled = thursdayEnabled;
	}

	public boolean isFridayEnabled() {
		return fridayEnabled;
	}

	public void setFridayEnabled(boolean fridayEnabled) {
		this.fridayEnabled = fridayEnabled;
	}

	public boolean isSaturdayEnabled() {
		return saturdayEnabled;
	}

	public void setSaturdayEnabled(boolean saturdayEnabled) {
		this.saturdayEnabled = saturdayEnabled;
	}

	public Integer getTargets() {
		return targets;
	}

	public void setTargets(Integer targets) {
		this.targets = targets;
	}

	public Integer getSearches() {
		return searches;
	}

	public void setSearches(Integer searches) {
		this.searches = searches;
	}

	@Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Group other = (Group) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
    
}
