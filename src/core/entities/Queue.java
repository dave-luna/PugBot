package core.entities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import core.util.Utils;
import core.Database;
import core.util.Trigger;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;

public class Queue {
	private Integer maxPlayers;
	private String name;
	private long serverId;
	private int id;
	private List<Member> playersInQueue = new ArrayList<Member>();;
	private List<Game> games = new ArrayList<Game>();;
	private List<Member> waitList = new ArrayList<Member>();
	private HashMap<Integer, List<Member>> notifications = new HashMap<Integer, List<Member>>();
	private Trigger t;
	public QueueSettings settings;
	
	public Queue(String name, int maxPlayers, long guildId, int id) {
		this.name = name;
		this.maxPlayers = maxPlayers;
		this.serverId = guildId;
		this.id = id;
		this.settings = new QueueSettings(guildId, id);
	}

	/**
	 * Adds player to queue
	 * Checks to fire notifications
	 * Pops queue if at max capacity
	 * 
	 * @param player the player to be added
	 */
	public void add(Member player) {
		if (!playersInQueue.contains(player) && !getManager().isPlayerIngame(player)) {
			if(!getManager().hasPlayerJustFinished(player)){
				playersInQueue.add(player);
				Database.insertPlayerInQueue(serverId, id, player.getUser().getIdLong());
				checkNotifications();
				
				if (playersInQueue.size() == maxPlayers) {
					popQueue();
				}
			}else{
				addToWaitList(player);
			}
		}
	}
	
	public void addPlayerToQueueDirectly(Member player){
		playersInQueue.add(player);
	}

	/**
	 * Returns the name of the queue
	 * 
	 * @return the name of the queue
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the queue
	 * 
	 * @param s the new queue name
	 */
	public void setName(String s) {
		name = s;
	}

	/**
	 * Returns the max capacity of the queue
	 * 
	 * @return the number of max players the queue can hold
	 */
	public Integer getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * Sets the max capacity of the queue
	 * 
	 * @param num the new maxPlayers value
	 */
	public void setMaxPlayers(Integer num) {
		maxPlayers = num;
	}

	/**
	 * Returns the current number of players in queue
	 * 
	 * @return
	 */
	public Integer getCurrentPlayersCount() {
		return playersInQueue.size();
	}

	/**
	 * Get the amount of currently active games for this queue
	 * 
	 * @return the number of active games
	 */
	public Integer getNumberOfGames() {
		return games.size();
	}

	/**
	 * Returns the List of active games for this queue
	 * 
	 * @return List containing the active games
	 */
	public List<Game> getGames() {
		return games;
	}

	/**
	 * Returns List of players currently in queue
	 * 
	 * @return List of players currently in queue
	 */
	public List<Member> getPlayersInQueue() {
		return playersInQueue;
	}

	/**
	 * Creates a new game.
	 * Removes players in-game from other queues.
	 * Sends notification to the pug channel and to each player in queue.
	 */
	private void popQueue() {
		String names = "";
		List<Member> players = new ArrayList<Member>(playersInQueue);
		TextChannel pugChannel = ServerManager.getServer(serverId).getPugChannel();
		
		// Send alert to players and compile their names
		for(Member m : players){
			names += m.getEffectiveName() + ", ";
			try{
				PrivateChannel c = m.getUser().openPrivateChannel().complete();
				c.sendMessage(String.format("`Your game: %s has started!`", name)).queue();
			}catch(Exception ex){
				System.out.println("Error sending private message.\n" + ex.getMessage());
			}
		}
		names = names.substring(0, names.lastIndexOf(","));
		
		// Create Game and add to the list of active games
		Game newGame = new Game(this, serverId, players);
		games.add(newGame);
		
		// Remove players from all other queues
		ServerManager.getServer(serverId).getQueueManager().purgeQueue(players);
		Database.deletePlayersInQueueFromQueue(serverId, id);
		
		// Generate captain string
		String captainString = "";
		if(settings.randomizeCaptains()){
			captainString = String.format("**Captains:** <@%s> & <@%s>", newGame.getCaptain1().getUser().getId(), newGame.getCaptain2().getUser().getId());
		}
		
		// Send game start message to pug channel
		pugChannel.sendMessage(Utils.createMessage(String.format("Game: %s starting%n", name), String.format("%s%n%s", names, captainString), Color.YELLOW)).queueAfter(2, TimeUnit.SECONDS);
		
		/*String servers = new CmdPugServers().getServers(guildId, null);
		if(!servers.equals("N/A")){
			pugChannel.sendMessage(Utils.createMessage("`Pug servers:`", servers, true)).queueAfter(2, TimeUnit.SECONDS);
		}*/
	}

	/**
	 * Ends game, adds players to justFinished list, starts finish timer
	 * 
	 * @param g the game to finish
	 */
	public void finish(Game g) {
		List<Member> players = new ArrayList<Member>(g.getPlayers());
		ServerManager.getServer(serverId).getQueueManager().addToJustFinished(players);
		g.finish();
		games.remove(g);
		t = () -> ServerManager.getServer(serverId).getQueueManager().timerEnd(players);
		Timer timer = new Timer(ServerManager.getServer(serverId).getSettings().getQueueFinishTimer(), t);
		timer.start();
	}

	/**
	 * Removes player from queue or waitList
	 * 
	 * @param member the player to remove
	 */
	public void delete(Member member) {
		if(playersInQueue.contains(member)){
			playersInQueue.remove(member);
			Database.deletePlayerInQueue(serverId, id, member.getUser().getIdLong());
		}else if(waitList.contains(member)){
			waitList.remove(member);
		}
	}

	/**
	 * Removes all players in a list from queue
	 * 
	 * @param players List of players to remove from queue
	 */
	public void purge(List<Member> players) {
		for(Member player : players){
			if(playersInQueue.contains(player) || waitList.contains(player)){
				delete(player);
			}
		}
	}

	/**
	 * Returns boolean based on if a player is matched to the provided name or not
	 * 
	 * @param name the name of the player to check for
	 * @return true if player matches the name provided
	 */
	public boolean containsPlayer(String name) {
		for (Member u : playersInQueue) {
			if (u.getEffectiveName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a Member object matching the provided name
	 * 
	 * @param name the name to match to a player
	 * @return Member object matching the provided name, null if no matches
	 */
//	public Member getPlayer(String name) {
//		for (Member u : playersInQueue) {
//			if (u.getEffectiveName().equalsIgnoreCase(name)) {
//				return u;
//			}
//		}
//		return null;
//	}

	/**
	 * Randomizes players waiting after finishing a game into queue
	 * 
	 * @param players the players to allow to add to queue
	 */
	public void addPlayersWaiting(List<Member> players) {
		if(waitList.size() > 0){
			Random random = new Random();
			List<Member> playersToAdd = new ArrayList<Member>(players);
			while(playersToAdd.size() > 0){
				Integer i = random.nextInt(playersToAdd.size());
				Member player = playersToAdd.get(i);
				if(waitList.contains(player)){
					add(player);
					waitList.remove(player);
				}
				playersToAdd.remove(player);
			}
		}
	}
	
	/**
	 * Adds a player that has just finished to the wait list
	 * 
	 * @param player the player to add to the wait list
	 */
	public void addToWaitList(Member player){
		if(!waitList.contains(player)){
			waitList.add(player);
		}
	}
	
	/**
	 * Checks if the specified player is in the wait list
	 * 
	 * @param player the player to check
	 * @return true if the player is in the wait list
	 */
	public boolean isPlayerWaiting(Member player){
		return waitList.contains(player);
	}

	/**
	 * Adds a notification to alert a player if a playerCount threshold is met in this queue
	 * 
	 * @param player the player associated with the notification
	 * @param playerCount the threshold to alert to player
	 */
	public void addNotification(Member player, Integer playerCount) {
		if(notifications.containsKey(playerCount)){
			if(!notifications.get(playerCount).contains(player)){
				notifications.get(playerCount).add(player);
			}
		}else{
			notifications.put(playerCount, new ArrayList<Member>());
			notifications.get(playerCount).add(player);
		}
		Database.insertQueueNotification(serverId, id, player.getUser().getIdLong(), playerCount);
	}
	
	/**
	 * Checks if notifications should be sent
	 */
	private void checkNotifications(){
		if(notifications.containsKey(playersInQueue.size())){
			notify(notifications.get(playersInQueue.size()));
		}
	}

	/**
	 * Sends alerts to the list of users that have notifications
	 * 
	 * @param users the users to send alerts to
	 */
	private void notify(List<Member> users) {
		for(Member m : users){
			if(!playersInQueue.contains(m) && (m.getOnlineStatus().equals(OnlineStatus.ONLINE) || m.getOnlineStatus().equals(OnlineStatus.IDLE))){
				try{
					m.getUser().openPrivateChannel().complete()
						.sendMessage(String.format("Queue: %s is at %d players!", name, playersInQueue.size())).complete();
				}catch(Exception ex){
					System.out.println("Error sending private message.\n" + ex.getMessage());
				}
			}
		}
	}

	/**
	 * Removes all notifications from this queue
	 * 
	 * @param player the player to remove notifications for
	 */
	public void removeNotification(Member player) {
		for(List<Member> list : notifications.values()){
			list.remove(player);
		}
		Database.deleteQueueNotification(serverId, id, player.getUser().getIdLong());
	}
	
	/**
	 * Returns HashMap containing all notifications in this queue
	 * 
	 * @return HashMap of notifications in this queue
	 */
	public HashMap<Integer, List<Member>> getNotifications(){
		return notifications;
	}
	
	public int getId(){
		return id;
	}
	
	private QueueManager getManager(){
		return ServerManager.getServer(serverId).getQueueManager();
	}
}
