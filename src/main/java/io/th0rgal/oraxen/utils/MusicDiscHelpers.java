package io.th0rgal.oraxen.utils;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanic;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.logging.Level;

public class MusicDiscHelpers {
    public static final NamespacedKey MUSIC_DISC_KEY = new NamespacedKey(OraxenPlugin.get(), "music_disc");

    public static boolean hasMusicDisc(final PersistentDataContainer pdc) {
        return pdc.has(MUSIC_DISC_KEY, DataType.ITEM_STACK);
    }

    public static ItemStack getMusicDisc(final PersistentDataContainer pdc) {
        try {
            return pdc.get(MUSIC_DISC_KEY, DataType.ITEM_STACK);
        } catch (final Exception e) {
            // music disc is saved as an older format, that is no longer supported.
            // TODO: maybe try recovering from the old format
            // possible causes item was saved on paper pre 1.21.5 and now its 1.21.5 or later
            // or it was saved on spigot and now loaded on paper.
            OraxenPlugin.get().getLogger().log(Level.SEVERE, "Failed to read Music disc from pdc! " + pdc.toString(), e);
            return null;
        }
    }

    public static void setAndPlayMusicDisc(final Entity entity, final ItemStack record, final float volume, final float pitch) {
        final var pdc = entity.getPersistentDataContainer();
        final var song = MusicDiscHelpers.getSong(record);
        if (song == null) return;
        final Key songKey = Key.key(song);
        final Key soundId = Key.key(OraxenPlugin.get().getSoundManager().songKeyToSoundId(songKey));

        entity.getWorld().playSound(Sound.sound(soundId, Sound.Source.RECORD, volume, pitch));
        pdc.set(MUSIC_DISC_KEY, DataType.ITEM_STACK, record);
    }

    public static ItemStack stopJukeboxAt(final Entity entity, final float volume, final float pitch) {
        final var pdc = entity.getPersistentDataContainer();
        final ItemStack record = getMusicDisc(pdc);
        if (record == null) return null;
        pdc.remove(MUSIC_DISC_KEY);
        final var song = MusicDiscHelpers.getSong(record);
        if (song == null) return record;
        final Key songKey = Key.key(song);
        final Key soundId = Key.key(OraxenPlugin.get().getSoundManager().songKeyToSoundId(songKey));

        entity.getWorld().getNearbyEntities(entity.getLocation(), 64, 64, 64, e -> e instanceof Player)
                .stream().map(Player.class::cast)
                .forEach(player -> player.stopSound(SoundStop.namedOnSource(soundId, Sound.Source.RECORD)));
        return record;
    }

    public static boolean isVanillaJukeboxWithVanillaDisc(final Block block) {
        if (!block.getType().equals(Material.JUKEBOX)) return false;
        final var blockState = BlockHelpers.getState(block);
        if (blockState instanceof final Jukebox jukebox) {
            return ItemUtils.isMusicDisc(jukebox.getRecord());
        }
        return false;
    }

    @Nullable
    public static String getSong(final ItemStack record, final MechanicFactory factory) {
        if (ItemUtils.isInvalidItem(record)) return null;

        final var song = getSong(record);
        if (song != null) return song;

        final String itemID = OraxenItems.getIdByItem(record);
        final Mechanic mechanic = factory.getMechanic(itemID);
        if (mechanic instanceof final MusicDiscMechanic musicDiscMechanic && !musicDiscMechanic.hasNoSong()) {
            return musicDiscMechanic.getSong();
        }
        return null;
    }

    /**
     * Gets the vanilla song from the ItemStack
     *
     * @param record the record who could hold a song
     * @return the song from the playable component or record
     */
    @Nullable
    public static String getSong(final ItemStack record) {
        if (ItemUtils.isInvalidItem(record)) return null;
        // native disks don't seem to have jukebox playable set to true
        if (VersionUtil.atOrAbove("1.21") && record.hasItemMeta() && record.getItemMeta().hasJukeboxPlayable()) {
            return record.getItemMeta().getJukeboxPlayable().getSongKey().toString();
        } else if (record.getType().isRecord()) {
            return record.getType().toString().toLowerCase(Locale.ROOT)
                    .replaceFirst("music_disc_", "minecraft:music_disc.");
        }
        return null;
    }
}
