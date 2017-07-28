package io.github.skepter.itemchat;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
		getServer().getPluginManager().registerEvents(this, this);
	}
	

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		if (event.getMessage().contains("[item]")) {
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(this, new Runnable() {

				@Override
				public void run() {
					ItemStack is = event.getPlayer().getInventory().getItemInMainHand();
					
					if(is.getType().equals(Material.AIR)) {
						event.getPlayer().sendMessage(ChatColor.RED + "You gotta hold an item in your hand to say [item], silly!");
						return;
					}
					
					
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
					
					String rawMessage = event.getFormat().replace("%1$s", event.getPlayer().getName()).replace("%2$s", event.getMessage());
					
					String[] rawMessageArr = rawMessage.split("\\[item\\]");
					
					boolean appendItemAtEnd = false;
					if(countOccurances(rawMessage, "[item]") > 1 && rawMessage.endsWith("[item]")) {
						appendItemAtEnd = true;
					}
					
					TextComponent builder = new TextComponent();
					ChatColor tempChatColor = ChatColor.RESET;
					for (int i = 0; i < rawMessageArr.length; i++) {
						String messagePart = rawMessageArr[i];
						TextComponent messagePartComponent = new TextComponent(messagePart);
						messagePartComponent.setColor(tempChatColor);
						builder.addExtra(messagePartComponent);
						
						//Add [item]
						if(i != rawMessageArr.length - 1 || rawMessageArr.length == 1) {
							builder.addExtra(getItemTextComponent(displayName, is));
						}
						
						//trawl back through message to find first & chatcolor and apply it if necessary
						tempChatColor = getChatColorFromTrawl(rawMessage, i);
					}
					
					if(appendItemAtEnd) {
						builder.addExtra(getItemTextComponent(displayName, is));
					}
					
					Bukkit.spigot().broadcast(builder);
					
				}

			});
		}
	}	
	
	private int countOccurances(String str, String subStr) {
		int count = 0;
		while (str.indexOf(subStr) > -1){
		    str = str.replaceFirst(subStr, "");
		    count++;
		}
		return count;
	}
	
	private ChatColor getChatColorFromTrawl(String rawMessage, int currentIndex) {
		int stringIndex = rawMessage.indexOf("[item]", rawMessage.indexOf("[item]") + currentIndex);
		for(int i = stringIndex; i >= 0; i--) {
			if(rawMessage.charAt(i) == '§') {
				return getColor(rawMessage.charAt(i + 1));
			}
		}
		return ChatColor.RESET;
	}
	
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
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string for
	 * sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
	 *
	 * @param itemStack
	 *            the item to convert
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
