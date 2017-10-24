package bullybot.commands;

import java.util.ArrayList;

import bullybot.classfiles.QueueManager;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public abstract class Command {
	protected String name;
	protected String helpMsg;
	protected String successMsg;
	protected String description;
	protected Message response;
	protected String lastResponseId = null;
	protected boolean dm = false;
	protected boolean adminRequired = false;
	protected boolean pugCommand = true;
	
	public abstract void execCommand(QueueManager qm, Member member, ArrayList<String> args);
	
	public String help(){
		return this.helpMsg;
	}
	
	public Message getResponse(){
		return this.response;
	}
	
	public boolean getDM(){
		return dm;
	}
	
	public boolean getAdminRequired(){
		return adminRequired;
	}
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return description;
	}
	
	public boolean getPugCommand(){
		return pugCommand;
	}
	
	public void setLastResponseId(String id){
		lastResponseId = id;
	}
}