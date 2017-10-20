package bullybot.classfiles.functions;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class Stuff {
	private static ArrayList<String> admins = new ArrayList<String>();
	
	public static Message createMessage(String title, String description, boolean success){
		MessageBuilder embed = new MessageBuilder();
		Color color;
		if(success){
			color = Color.green;
		}else{
			color = Color.red;
		}
		if(!title.isEmpty() && title != null){
			embed.append(String.format("`%s`", title));
		}
		if(!description.isEmpty() && description != null){
			embed.setEmbed(new EmbedBuilder().setDescription(description).setColor(color).build());
		}
		
		return embed.build();
	}
	
	public static Message createMessage(String title, String description, Color color){
		MessageBuilder embed = new MessageBuilder();
		if(!title.isEmpty() && title != null){
			embed.append(String.format("`%s`", title));
		}
		embed.setEmbed(new EmbedBuilder().setDescription(description).setColor(color).build());
		return embed.build();
	}
	
	public static Message createMessage(String title){
		MessageBuilder embed = new MessageBuilder();
		embed.append(title);
		
		return embed.build();
	}
	
	public static boolean isAdmin(Member m){
		if(admins.contains(m.getUser().getId()) || m.getPermissions().contains(Permission.KICK_MEMBERS)){
			return true;
		}
		return false;
	}
	
	public static void createFiles() {
		try{
			File dir = new File("app_data");
			File games = new File("app_data/games.txt");
			if(!dir.exists()){
				dir.mkdir();
			}
		
			if(!games.exists()){
				games.createNewFile();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	public static void loadAdminList(){
		if (new File("app_data/admins.txt").exists()) {
			try {
				System.out.println("Loading admin list...");
				Scanner reader = new Scanner(new FileInputStream("app_data/admins.txt"));

				while (reader.hasNextLine()) {
					String id = reader.next();
					if (Pattern.matches("\\d{15,}", id)){
						admins.add(id);
					}
				}
				
				reader.close();
				System.out.println("Admin list loaded");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
