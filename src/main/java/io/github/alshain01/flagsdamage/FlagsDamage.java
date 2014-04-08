/* Copyright 2013 Kevin Seiden. All rights reserved.

 This works is licensed under the Creative Commons Attribution-NonCommercial 3.0

 You are Free to:
    to Share: to copy, distribute and transmit the work
    to Remix: to adapt the work

 Under the following conditions:
    Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
    Non-commercial: You may not use this work for commercial purposes.

 With the understanding that:
    Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
    Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
    Other Rights: In no way are any of the following rights affected by the license:
        Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
        The author's moral rights;
        Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.

 Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 http://creativecommons.org/licenses/by-nc/3.0/
 */
package io.github.alshain01.flagsdamage;

import io.github.alshain01.flags.api.Flag;
import io.github.alshain01.flags.api.FlagsAPI;
import io.github.alshain01.flags.api.area.Area;
import io.github.alshain01.flags.api.area.Siegeable;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Flags Damage - Module that adds damage flags to the plug-in Flags.
 */
@SuppressWarnings("unused")
public class FlagsDamage extends JavaPlugin {
	/**
	 * Called when this module is enabled
	 */
	@Override
	public void onEnable() {
		final PluginManager pm = Bukkit.getServer().getPluginManager();

		if (!pm.isPluginEnabled("Flags")) {
			getLogger().severe("Flags was not found. Shutting down.");
			pm.disablePlugin(this);
            return;
		}

		// Connect to the data file and register the flags
        YamlConfiguration flagConfig = YamlConfiguration.loadConfiguration(getResource("flags.yml"));
        Set<Flag> flags = FlagsAPI.getRegistrar().register(flagConfig, "Damage");
        Map<String, Flag> flagMap = new HashMap<String, Flag>();
        for(Flag f : flags) {
            flagMap.put(f.getName(), f);
        }

		// Load plug-in events and data
		Bukkit.getServer().getPluginManager().registerEvents(new EntityDamageListener(flagMap), this);
	}

	/*
	 * The event handler for the flags we created earlier
	 */
	private class EntityDamageListener implements Listener {
        final Map<String, Flag> flags;

        private EntityDamageListener (Map<String, Flag> flags) {
            this.flags = flags;
        }

		@EventHandler(ignoreCancelled = true)
		private void onEntityDamage(EntityDamageEvent e) {
			// If the damage is not a to a player, we do nothing.
			if (!(e.getEntity() instanceof Player)) {
				return;
			}

			if (e.getCause() == DamageCause.ENTITY_ATTACK
					|| e.getCause() == DamageCause.ENTITY_EXPLOSION
					|| e.getCause() == DamageCause.PROJECTILE) {
				// Handled by subclass events EntityDamageByEntity so that we
				// can retrieve the damager.
				return;
			}

			Flag flag;

			switch (e.getCause()) {
			case BLOCK_EXPLOSION:
				flag = flags.get("DamageBlockExplode");
				break;
			case CONTACT:
				flag = flags.get("DamageBlockContact");
				break;
			case DROWNING:
				flag = flags.get("DamageDrown");
				break;
			case FALL:
				flag = flags.get("DamageFall");
				break;
            case FALLING_BLOCK:
                flag = flags.get("DamageBlockFall");
                break;
			case FIRE:
				flag = flags.get("DamageFire");
				break;
			case FIRE_TICK:
				flag = flags.get("DamageBurn");
				break;
			case LAVA:
				flag = flags.get("DamageLava");
				break;
			case LIGHTNING:
				flag = flags.get("DamageLightning");
				break;
			case MAGIC:
				flag = flags.get("DamageMagic");
				break;
			case MELTING:
				flag = flags.get("DamageMelting");
				break;
			case POISON:
				flag = flags.get("DamagePoison");
				break;
			case STARVATION:
				flag = flags.get("DamageStarve");
				break;
			case SUFFOCATION:
				flag = flags.get("DamageSuffocate");
				break;
			case SUICIDE:
				flag = flags.get("DamageSuicide");
				break;
            case THORNS:
                flag = flags.get("DamageThorns");
                break;
            case WITHER:
                flag = flags.get("DamageWither");
                break;
			case VOID:
				flag = flags.get("DamageVoid");
				break;
			default:
                flag = flags.get("DamageOther");
			}

			// Always guard this, even when it really can't happen.
			if (flag != null) { 
				e.setCancelled(!FlagsAPI.getAreaAt(e.getEntity().getLocation()).getState(flag, false));
			}
		}

		@EventHandler(ignoreCancelled = true)
		private void onEntityDamagedByEntity(EntityDamageByEntityEvent e) {
			// If the damage is not a to a player, we do nothing.
			if (!(e.getEntity() instanceof Player)) { return; }

			Flag flag = null;
			final Entity damager = e.getDamager();
			if (damager instanceof Monster
                    || damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Monster) {
				flag = flags.get("DamageMonster");
			} else if (damager instanceof Player
					|| damager instanceof Projectile
					   && ((Projectile) damager).getShooter() instanceof Player) {
				if (!FlagsAPI.inPvpCombat((Player) e.getEntity())) {
					// Don't interfere with a battle, you can't attack and then
					// retreat to a protected area (that's cheating)
					// Uses GriefPrevention's timer (15 second default). Not supported by
					// other systems.
					final Area area = FlagsAPI.getAreaAt(e.getEntity().getLocation());
					if (!(area instanceof Siegeable) || !((Siegeable) area).isUnderSiege()) {
						// If your under siege, your on your own.
						// That's part of the game.
						// Only supported by GP.
						flag = flags.get("Pvp");
					}
				}
			} else {
				// Not really sure how you got here, but if you did...
				flag = flags.get("DamageOther");
			}

			// Always guard this, even when it really can't happen.
			if (flag != null) { 
				e.setCancelled(!FlagsAPI.getAreaAt(e.getEntity().getLocation()).getState(flag, false));
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onPotionSplash(PotionSplashEvent e) {
			final Flag flag = FlagsAPI.getRegistrar().getFlag("PotionSplash");
			if (flag == null) {	return;	}

			for (final LivingEntity entity : e.getAffectedEntities()) {
				if (entity instanceof Player) {
    				if (!FlagsAPI.getAreaAt(e.getEntity().getLocation()).getState(flag, false)) {
                        // Essentially cancels it.
                        // Only way to cancel on a player by player basis instead of
                        // the whole effect.
                        e.setIntensity(entity, 0);
                    }
				}
			}
		}
	}
}
