package com.github.dsh105.echopet.listeners;

import java.util.Iterator;

import com.github.dsh105.echopet.data.PetType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.dsh105.echopet.EchoPet;
import com.github.dsh105.echopet.data.PetData;
import com.github.dsh105.echopet.entity.pet.Pet;
import com.github.dsh105.echopet.menu.DataMenu;
import com.github.dsh105.echopet.menu.DataMenu.DataMenuType;
import com.github.dsh105.echopet.menu.DataMenuItem;
import com.github.dsh105.echopet.menu.MenuItem;
import com.github.dsh105.echopet.menu.PetMenu;
import com.github.dsh105.echopet.menu.WaitingMenuData;
import com.github.dsh105.echopet.util.EnumUtil;
import com.github.dsh105.echopet.util.Lang;
import com.github.dsh105.echopet.util.MenuUtil;
import com.github.dsh105.echopet.util.Particle;
import com.github.dsh105.echopet.util.StringUtil;

public class MenuListener implements Listener {
	
	private EchoPet ec;
	
	public MenuListener(EchoPet ec) {
		this.ec = ec;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryOpen(InventoryOpenEvent event) {
		Player player = (Player) event.getPlayer();
		final Pet pet = EchoPet.getPluginInstance().PH.getPet(player);
		if (pet == null) {
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		final Pet pet = EchoPet.getPluginInstance().PH.getPet(player);
		if (pet == null) {
			return;
		}
		
		Inventory inv = event.getInventory();
		String title = event.getView().getTitle();
		int slot = event.getRawSlot();
		int size = (title.equals("EchoPet DataMenu - Color") || pet.getPetType() == PetType.HORSE) ? 18 : 9;

		WaitingMenuData wmd = WaitingMenuData.waiting.get(pet);
		if (wmd == null) {
			wmd = new WaitingMenuData(pet);
		}

		try {
			if (slot < 0) {
				return;
			}
		} catch (Exception e) {}
		if (slot <= size && inv.getItem(slot) != null) {
			if (title.equals("EchoPet DataMenu")) {
				if (inv.getItem(slot).equals(DataMenuItem.CLOSE.getItem())) {
					player.closeInventory();
					event.setCancelled(true);
					return;
				}
				for (final MenuItem mi : MenuItem.values()) {
					if (inv.getItem(slot).equals(mi.getItem()) || inv.getItem(slot).equals(mi.getBoolean(true)) || inv.getItem(slot).equals(mi.getBoolean(false))) {
						EchoPet.getPluginInstance().log("test1");
						if (mi.getMenuType() == DataMenuType.BOOLEAN) {
							EchoPet.getPluginInstance().log("test2");
							if (EnumUtil.isEnumType(PetData.class, mi.toString())) {
								PetData pd = PetData.valueOf(mi.toString());
								if (pet.getAllData(true).contains(pd)) {
									wmd.petDataFalse.add(pd);
								}
								else {
									wmd.petDataTrue.add(pd);
								}
							}
							else {
								if (mi.toString().equals("HAT")) {
									if (StringUtil.hpp("echopet.pet", "hat", player)) {
										if (!pet.isPetHat()) {
											pet.setAsHat(true);
											pet.getOwner().sendMessage(Lang.HAT_PET_ON.toString());
										}
										else {
											pet.setAsHat(false);
											pet.getOwner().sendMessage(Lang.HAT_PET_OFF.toString());
										}
									}
								}
								if (mi.toString().equals("RIDE")) {
									if (StringUtil.hpp("echopet.pet", "ride", player)) {
										if (!pet.isOwnerRiding()) {
											pet.ownerRidePet(true);
											inv.setItem(slot, mi.getBoolean(false));
											pet.getOwner().sendMessage(Lang.RIDE_PET_ON.toString());
										}
										else {
											pet.ownerRidePet(false);
											inv.setItem(slot, mi.getBoolean(true));
											pet.getOwner().sendMessage(Lang.RIDE_PET_OFF.toString());
										}
									}
								}
							}
						}
						else {
							player.closeInventory();
							new BukkitRunnable() {
								public void run() {
									DataMenu dm = new DataMenu(mi, pet);
									dm.open(false);
								}
							}.runTaskLater(ec, 1L);
						}
					}
				}
				event.setCancelled(true);
			}
			else if (title.startsWith("EchoPet DataMenu - ")) {
				if (inv.getItem(slot).equals(DataMenuItem.BACK.getItem())) {
					player.closeInventory();
					new BukkitRunnable() {
						public void run() {
							int size = pet.getPetType() == PetType.HORSE ? 18 : 9;
							PetMenu menu = new PetMenu(pet, MenuUtil.createOptionList(pet.getPetType()), size);
							menu.open(false);
						}
					}.runTaskLater(ec, 1L);
					event.setCancelled(true);
					return;
				}
				for (DataMenuItem dmi : DataMenuItem.values()) {
					if (inv.getItem(slot).equals(dmi.getItem())) {
						wmd.petDataTrue.add(dmi.getDataLink());
					}
				}
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		Pet pet = EchoPet.getPluginInstance().PH.getPet(player);
		if (pet == null) {
			return;
		}
		
		if (!event.getView().getTitle().contains("EchoPet DataMenu")) {
			return;
		}
		
		WaitingMenuData wmd = WaitingMenuData.waiting.get(pet);
		
		if (wmd != null) {
			if (!wmd.petDataTrue.isEmpty()) {
				Iterator<PetData> i = wmd.petDataTrue.listIterator();
				while (i.hasNext()) {
					PetData dataTemp = i.next();
					if (!StringUtil.hpp("echopet.pet.data", dataTemp.getConfigOptionString().toLowerCase(), player)) {
						i.remove();
					}
				}

				ec.PH.setData(pet, wmd.petDataTrue.toArray(new PetData[wmd.petDataTrue.size()]), true);
				try {
					Particle.FIRE.sendToLocation(pet.getLocation());
				} catch (Exception e) {
					ec.debug(e, "Particle Effect failed.");
				}
			}
			
			if (!wmd.petDataFalse.isEmpty()) {
				Iterator<PetData> i2 = wmd.petDataFalse.listIterator();
				while (i2.hasNext()) {
					PetData dataTemp = i2.next();
					if (!StringUtil.hpp("echopet.pet.data", dataTemp.getConfigOptionString().toLowerCase(), player)) {
						i2.remove();
					}
				}

				ec.PH.setData(pet, wmd.petDataFalse.toArray(new PetData[wmd.petDataFalse.size()]), false);
				try {
					Particle.RAINBOW_SMOKE.sendToLocation(pet.getLocation());
				} catch (Exception e) {
					ec.debug(e, "Particle Effect failed.");
				}
			}

			WaitingMenuData.waiting.remove(wmd);
		}
		
	}
}