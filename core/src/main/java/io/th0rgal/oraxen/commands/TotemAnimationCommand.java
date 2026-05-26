package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class TotemAnimationCommand {

    private static volatile Object cachedDeathProtectionType;
    private static final Object DEATH_PROTECTION_INIT_LOCK = new Object();
    private static volatile Class<?> cachedDataComponentTypeClass;
    private static volatile Class<?> cachedValuedDataComponentTypeClass;
    private static volatile Method cachedSetDataMethod;
    private static volatile Method cachedHasDataMethod;
    private static volatile Method cachedDeathProtectionMethod;
    private static volatile boolean deathProtectionTypeInitialized;
    private static volatile boolean dataComponentTypeClassInitialized;
    private static volatile boolean valuedDataComponentTypeClassInitialized;
    private static volatile boolean setDataMethodInitialized;
    private static volatile boolean hasDataMethodInitialized;
    private static volatile boolean deathProtectionMethodInitialized;
    private static final Object PACKET_EVENTS_INIT_LOCK = new Object();
    private static volatile Method cachedPacketEventsGetAPIMethod;
    private static volatile Method cachedPacketEventsGetPlayerManagerMethod;
    private static volatile Method cachedPacketEventsSendPacketMethod;
    private static volatile Constructor<?> cachedEntityStatusPacketConstructor;
    private static volatile boolean packetEventsMethodsInitialized;
    private static volatile boolean loggedDeathProtectionFailure;
    private static volatile boolean loggedPacketEventsFailure;

    CommandAPICommand getTotemAnimationCommand() {
        return new CommandAPICommand("totem-animation")
                .withPermission("oraxen.command.totem-animation")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> getItemSuggestions()))
                )
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String itemId = (String) args.get("item");
                    ItemStack itemStack = parseItem(itemId);
                    if (itemStack == null) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemId));
                        return;
                    }

                    playAnimation(target, addDeathProtection(itemStack));
                    Message.TOTEM_ANIMATION_SUCCESS.send(sender,
                            AdventureUtils.tagResolver("player", target.getName()),
                            AdventureUtils.tagResolver("item", itemId));
                });
    }

    private String[] getItemSuggestions() {
        return Stream.concat(
                Arrays.stream(OraxenItems.getItemNames()),
                Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .map(material -> "minecraft:" + material.name().toLowerCase(Locale.ROOT))
        ).toArray(String[]::new);
    }

    private ItemStack parseItem(String itemId) {
        ItemBuilder itemBuilder = OraxenItems.getItemById(itemId);
        if (itemBuilder != null) {
            return itemBuilder.build();
        }

        String materialName = itemId.toUpperCase(Locale.ROOT);
        if (materialName.startsWith("MINECRAFT:")) {
            materialName = materialName.substring("MINECRAFT:".length());
        }

        Material material = Material.matchMaterial(materialName);
        return material != null && material.isItem() ? new ItemStack(material) : null;
    }

    private void playAnimation(Player target, ItemStack animationItem) {
        ItemStack previousMainHand = target.getInventory().getItemInMainHand().clone();
        ItemStack previousOffHand = target.getInventory().getItemInOffHand().clone();
        boolean mainHandIsTotem = isDeathProtectionItem(previousMainHand);

        if (mainHandIsTotem) {
            target.sendEquipmentChange(target, EquipmentSlot.HAND, null);
        }

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, animationItem);
        sendTotemStatus(target);

        if (mainHandIsTotem) {
            target.sendEquipmentChange(target, EquipmentSlot.HAND, previousMainHand);
        }

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, previousOffHand);
    }

    @SuppressWarnings("deprecation")
    private void sendTotemStatus(Player target) {
        if (VersionUtil.isPaperServer()) {
            target.sendEntityEffect(EntityEffect.PROTECTED_FROM_DEATH, target);
        } else if (PacketAdapter.isPacketEventsEnabled() && sendPacketEventsTotemStatus(target)) {
            return;
        } else {
            target.playEffect(EntityEffect.TOTEM_RESURRECT);
        }
    }

    private static boolean sendPacketEventsTotemStatus(Player target) {
        try {
            if (!initializePacketEventsMethods()) return false;
            Object packetEventsAPI = cachedPacketEventsGetAPIMethod.invoke(null);
            Object playerManager = cachedPacketEventsGetPlayerManagerMethod.invoke(packetEventsAPI);
            Object packet = cachedEntityStatusPacketConstructor.newInstance(target.getEntityId(), 35);
            cachedPacketEventsSendPacketMethod.invoke(playerManager, target, packet);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            logPacketEventsFailure(e);
            return false;
        }
    }

    private static boolean initializePacketEventsMethods() throws ReflectiveOperationException {
        if (packetEventsMethodsInitialized) return hasPacketEventsMethods();

        synchronized (PACKET_EVENTS_INIT_LOCK) {
            if (packetEventsMethodsInitialized) return hasPacketEventsMethods();

            Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Class<?> packetEventsAPIClass = Class.forName("com.github.retrooper.packetevents.PacketEventsAPI");
            Class<?> playerManagerClass = Class.forName("com.github.retrooper.packetevents.manager.player.PlayerManager");
            Class<?> packetWrapperClass = Class.forName("com.github.retrooper.packetevents.wrapper.PacketWrapper");
            Class<?> entityStatusPacketClass = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus");

            cachedPacketEventsGetAPIMethod = packetEventsClass.getMethod("getAPI");
            cachedPacketEventsGetPlayerManagerMethod = packetEventsAPIClass.getMethod("getPlayerManager");
            cachedPacketEventsSendPacketMethod = playerManagerClass.getMethod("sendPacket", Object.class, packetWrapperClass);
            cachedEntityStatusPacketConstructor = entityStatusPacketClass.getConstructor(int.class, int.class);
            packetEventsMethodsInitialized = true;
            return true;
        }
    }

    private static boolean hasPacketEventsMethods() {
        return cachedPacketEventsGetAPIMethod != null
                && cachedPacketEventsGetPlayerManagerMethod != null
                && cachedPacketEventsSendPacketMethod != null
                && cachedEntityStatusPacketConstructor != null;
    }

    private static void logPacketEventsFailure(Throwable throwable) {
        if (!loggedPacketEventsFailure) {
            synchronized (PACKET_EVENTS_INIT_LOCK) {
                if (!loggedPacketEventsFailure) {
                    Logs.logWarning("Failed to send totem animation via PacketEvents; falling back to Bukkit's deprecated totem effect. See debug log for details.");
                    loggedPacketEventsFailure = true;
                }
            }
        }
        Logs.debug(throwable);
    }

    private ItemStack addDeathProtection(ItemStack itemStack) {
        if (!supportsDeathProtectionComponent() || itemStack.getType() == Material.AIR) {
            return itemStack;
        }

        try {
            Object deathProtectionType = getDeathProtectionType();
            if (deathProtectionType == null) return itemStack;

            Method deathProtectionMethod = getDeathProtectionMethod();
            Method setDataMethod = getSetDataMethod();
            if (deathProtectionMethod == null || setDataMethod == null) return itemStack;

            Object deathProtection = deathProtectionMethod.invoke(null);
            setDataMethod.invoke(itemStack, deathProtectionType, deathProtection);
        } catch (ReflectiveOperationException | LinkageError e) {
            logDeathProtectionFailure(e);
        }

        return itemStack;
    }

    private boolean isDeathProtectionItem(ItemStack itemStack) {
        if (itemStack.getType() == Material.TOTEM_OF_UNDYING) {
            return true;
        }

        if (!supportsDeathProtectionComponent()) {
            return false;
        }

        try {
            Object deathProtectionType = getDeathProtectionType();
            Method hasDataMethod = getHasDataMethod();
            if (deathProtectionType == null || hasDataMethod == null) return false;
            return (boolean) hasDataMethod.invoke(itemStack, deathProtectionType);
        } catch (ReflectiveOperationException | LinkageError e) {
            Logs.debug(e);
            return false;
        }
    }

    private boolean supportsDeathProtectionComponent() {
        return VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.21.2");
    }

    public static void clearReflectionCaches() {
        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            cachedDeathProtectionType = null;
            cachedDataComponentTypeClass = null;
            cachedValuedDataComponentTypeClass = null;
            cachedSetDataMethod = null;
            cachedHasDataMethod = null;
            cachedDeathProtectionMethod = null;
            deathProtectionTypeInitialized = false;
            dataComponentTypeClassInitialized = false;
            valuedDataComponentTypeClassInitialized = false;
            setDataMethodInitialized = false;
            hasDataMethodInitialized = false;
            deathProtectionMethodInitialized = false;
            loggedDeathProtectionFailure = false;
        }

        synchronized (PACKET_EVENTS_INIT_LOCK) {
            cachedPacketEventsGetAPIMethod = null;
            cachedPacketEventsGetPlayerManagerMethod = null;
            cachedPacketEventsSendPacketMethod = null;
            cachedEntityStatusPacketConstructor = null;
            packetEventsMethodsInitialized = false;
            loggedPacketEventsFailure = false;
        }
    }

    private static void logDeathProtectionFailure(Throwable throwable) {
        if (!loggedDeathProtectionFailure) {
            synchronized (DEATH_PROTECTION_INIT_LOCK) {
                if (!loggedDeathProtectionFailure) {
                    Logs.logWarning("Failed to apply Paper death-protection component for totem animation; the animation item may not trigger the protected-from-death effect on this server build. See debug log for details.");
                    loggedDeathProtectionFailure = true;
                }
            }
        }
        Logs.debug(throwable);
    }

    private static Object getDeathProtectionType() throws ReflectiveOperationException {
        if (deathProtectionTypeInitialized) return cachedDeathProtectionType;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (deathProtectionTypeInitialized) return cachedDeathProtectionType;

            Field deathProtectionType = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes")
                    .getField("DEATH_PROTECTION");
            cachedDeathProtectionType = deathProtectionType.get(null);
            deathProtectionTypeInitialized = true;
            return cachedDeathProtectionType;
        }
    }

    private static Method getDeathProtectionMethod() throws ReflectiveOperationException {
        if (deathProtectionMethodInitialized) return cachedDeathProtectionMethod;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (deathProtectionMethodInitialized) return cachedDeathProtectionMethod;

            cachedDeathProtectionMethod = Class.forName("io.papermc.paper.datacomponent.item.DeathProtection")
                    .getMethod("deathProtection");
            deathProtectionMethodInitialized = true;
            return cachedDeathProtectionMethod;
        }
    }

    private static Method getSetDataMethod() throws ReflectiveOperationException {
        if (setDataMethodInitialized) return cachedSetDataMethod;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (setDataMethodInitialized) return cachedSetDataMethod;

            Class<?> valuedDataComponentTypeClass = getValuedDataComponentTypeClass();
            if (valuedDataComponentTypeClass == null) return null;
            cachedSetDataMethod = ItemStack.class.getMethod("setData", valuedDataComponentTypeClass, Object.class);
            setDataMethodInitialized = true;
            return cachedSetDataMethod;
        }
    }

    private static Method getHasDataMethod() throws ReflectiveOperationException {
        if (hasDataMethodInitialized) return cachedHasDataMethod;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (hasDataMethodInitialized) return cachedHasDataMethod;

            Class<?> dataComponentTypeClass = getDataComponentTypeClass();
            if (dataComponentTypeClass == null) return null;
            cachedHasDataMethod = ItemStack.class.getMethod("hasData", dataComponentTypeClass);
            hasDataMethodInitialized = true;
            return cachedHasDataMethod;
        }
    }

    private static Class<?> getDataComponentTypeClass() throws ClassNotFoundException {
        if (dataComponentTypeClassInitialized) return cachedDataComponentTypeClass;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (dataComponentTypeClassInitialized) return cachedDataComponentTypeClass;

            cachedDataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
            dataComponentTypeClassInitialized = true;
            return cachedDataComponentTypeClass;
        }
    }

    private static Class<?> getValuedDataComponentTypeClass() throws ClassNotFoundException {
        if (valuedDataComponentTypeClassInitialized) return cachedValuedDataComponentTypeClass;

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (valuedDataComponentTypeClassInitialized) return cachedValuedDataComponentTypeClass;

            cachedValuedDataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            valuedDataComponentTypeClassInitialized = true;
            return cachedValuedDataComponentTypeClass;
        }
    }
}
