package io.github.skepter.itemchat;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener {
	
	@Override
	public void onEnable() {
		//load chat event. nothing else needed here.
		getServer().getPluginManager().registerEvents(this, this);
	}
	

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		if (event.getMessage().contains("[item]")) {
			event.setCancelled(true);
			//Prepare to handle the event synchronously
			Bukkit.getScheduler().runTask(this, new Runnable() {

				@Override
				public void run() {
					ItemStack is = event.getPlayer().getInventory().getItemInMainHand();
					
					//Air can't be said in chat since it's not technically an item
					if(is.getType().equals(Material.AIR)) {
						event.getPlayer().sendMessage(ChatColor.RED + "You gotta hold an item in your hand to say [item], silly!");
						return;
					}
					
					//Get the name using reflection
					String displayName = getItemName(is);
					
					//Get name using old method if displayName fails
					if (displayName == null) {
						displayName = is.getType().name().toLowerCase().replace("_", " ");
					}
					
					//Overwrite name if it has a custom name
					if (is.hasItemMeta()) {
						if (is.getItemMeta().hasDisplayName()) {
							displayName = is.getItemMeta().getDisplayName();
						}
					}
					
					//get the raw message, replacing player's name and message to fit the formatting
					String rawMessage = event.getFormat().replace("%1$s", event.getPlayer().getName()).replace("%2$s", event.getMessage());
					
					//converts it to an array
					String[] rawMessageArr = rawMessage.split("\\[item\\]");
					
					/*
					 * It won't display more than three [item] if they end with [item] more than twice.
					 * e.g:
					 * [item][item] - this works
					 * [item][item][item] - this is replaced with [item][item]
					 * 
					 * blah [item][item][item] - replaced with blah [item][item]
					 * blah [item][item][item]. - works, since it doesn't end with [item]
					 * blah [item][item].[item] - works, since it doesn't end with two [item]'s
					 */
					
					//Adds [item] at the end if there's more than 1 occurance (and it's not at the end)
					//Quick fix for some bug
					boolean appendItemAtEnd = false;
					if(countOccurances(rawMessage) > 1 && rawMessage.endsWith("[item]")) {
						appendItemAtEnd = true;
					}
					
					//Builder to recreate the chat message
					TextComponent builder = new TextComponent();
					
					//temporary color (see getChatColorFromTrawl below)
					ChatColor tempChatColor = ChatColor.RESET;
					
					for (int i = 0; i < rawMessageArr.length; i++) {
						//Get the text part before [item]
						String messagePart = rawMessageArr[i];
						TextComponent messagePartComponent = new TextComponent(messagePart);
						
						//Add the tempColor
						messagePartComponent.setColor(tempChatColor);
						builder.addExtra(messagePartComponent);
						
						//Add [item]
						if(i != rawMessageArr.length - 1 || rawMessageArr.length == 1) {
							builder.addExtra(getItemTextComponent(displayName, is));
						}
						
						//trawl back through message to find the latest chatcolor
						tempChatColor = getChatColorFromTrawl(rawMessage, i);
					}
					
					//add [item] to the end if necessary
					if(appendItemAtEnd) {
						builder.addExtra(getItemTextComponent(displayName, is));
					}
					
					//Broadcast the message
					for(Player target : event.getRecipients()) {
						target.spigot().sendMessage(builder);
					}
				}
			});
		}
	}	
	
	/**
	 * Counts the occurances of [item] in a string
	 */
	private int countOccurances(String str) {
		int count = 0;
		while (str.indexOf("[item]") > -1){
		    str = str.replaceFirst("\\[item\\]", "");
		    count++;
		}
		return count;
	}
	
	/**
	 * Looks back through the message to find the previous chatcolor used to maintain
	 * continuity through item formatting
	 * 
	 * for example:
	 * &cblah [item] blah - makes the second blah also have red text
	 */
	private ChatColor getChatColorFromTrawl(String rawMessage, int currentIndex) {
		int stringIndex = rawMessage.indexOf("[item]", rawMessage.indexOf("[item]") + currentIndex);
		for(int i = stringIndex; i >= 0; i--) {
			if(rawMessage.charAt(i) == '§') {
				return getColor(rawMessage.charAt(i + 1));
			}
		}
		return ChatColor.RESET;
	}
	
	/** 
	 * Gets the chatcolor of a character. I'm sure this already works with ChatColor.getChar(), but I wanna be certain
	 * @param c the character to get the chatcolor from
	 * @return a ChatColor from the respective character
	 */
	private ChatColor getColor(char c) {
		switch(c) {
			case '0':
				return ChatColor.BLACK;
			case '1':
				return ChatColor.DARK_BLUE;
			case '2':
				return ChatColor.DARK_GREEN;
			case '3':
				return ChatColor.DARK_AQUA;
			case '4':
				return ChatColor.DARK_RED;
			case '5':
				return ChatColor.DARK_PURPLE;
			case '6':
				return ChatColor.GOLD;
			case '7':
				return ChatColor.GRAY;
			case '8':
				return ChatColor.DARK_GRAY;
			case '9':
				return ChatColor.BLUE;
			case 'a':
				return ChatColor.GREEN;
			case 'b':
				return ChatColor.AQUA;
			case 'c':
				return ChatColor.RED;
			case 'd':
				return ChatColor.LIGHT_PURPLE;
			case 'e':
				return ChatColor.YELLOW;
			case 'f':
				return ChatColor.WHITE;
			default:
				return ChatColor.RESET;
		}
	}
	
	/**
	 * Creates a text component for a specific item. Puts blue [ ] brackets around the item and adds
	 * a hover event to the main item inside.
	 */
	private TextComponent getItemTextComponent(String displayName, ItemStack is) {
		
		TextComponent header = new TextComponent("[");
		header.setColor(ChatColor.AQUA);
		
		TextComponent footer = new TextComponent("]");
		footer.setColor(ChatColor.AQUA);
		
		TextComponent item = new TextComponent(displayName);
		item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[] { new TextComponent(convertItemStackToJson(is)) }));
		item.setColor(ChatColor.AQUA);
		
		header.addExtra(item);
		header.addExtra(footer);
		
		return header;
	}

	/**
	 * Gets the item name from an item using reflection
	 * @param is the itemstack to get the name from
	 * @return the Minecraft "human friendly" name of an item
	 */
	private String getItemName(ItemStack is) {
		try {
			Object nmsCopy = ReflectionUtil.getOBCClass("inventory.CraftItemStack").getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, is);
			return String.valueOf(nmsCopy.getClass().getDeclaredMethod("getName").invoke(nmsCopy));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Resource found from https://www.spigotmc.org/threads/tut-item-tooltips-with-the-chatcomponent-api.65964/
	 * 
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string for
	 * sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
	 *
	 * @param itemStack the item to convert
	 * @return the Json string representation of the item
	 */
	private String convertItemStackToJson(ItemStack itemStack) {
		// ItemStack methods to get a net.minecraft.server.ItemStack object for
		// serialization
		Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
		Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

		// NMS Method to serialize a net.minecraft.server.ItemStack to a valid
		// Json string
		Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
		Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
		Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

		Object nmsNbtTagCompoundObj; // This will just be an empty
										// NBTTagCompound instance to invoke the
										// saveNms method
		Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack
								// object received from the asNMSCopy method
		Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack
									// after being put through saveNmsItem
									// method

		try {
			nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
			nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
			itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
		} catch (Throwable t) {
			Bukkit.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
			return null;
		}

		// Return a string representation of the serialized object
		return itemAsJsonObject.toString();
	}
}
