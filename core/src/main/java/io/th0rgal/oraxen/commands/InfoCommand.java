package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.StringArgument;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class InfoCommand {

    OraxenCommand getInfoCommand() {
        return new OraxenCommand("info")
                .withPermission("oraxen.command.info")
                .withSubcommands(
                        getInfoSubcommand(InfoType.ITEM),
                        getInfoSubcommand(InfoType.GLYPH),
                        getInfoSubcommand(InfoType.BLOCK));
    }

    private OraxenCommand getInfoSubcommand(InfoType type) {
        return new OraxenCommand(type.commandName())
                .withArguments(new StringArgument(type.argumentName())
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> getSuggestions(type))))
                .executes((commandSender, args) -> handleInfo(commandSender, type, (String) args.get(type.argumentName())));
    }

    private String[] getSuggestions(InfoType type) {
        Stream<String> suggestions = switch (type) {
            case ITEM -> Arrays.stream(OraxenItems.getItemNames());
            case GLYPH -> OraxenPlugin.get().getFontManager().getGlyphs().stream().map(Glyph::getName);
            case BLOCK -> OraxenBlocks.getBlockIDs().stream();
        };
        return Stream.concat(Stream.of("all"), suggestions).distinct().toArray(String[]::new);
    }

    private void handleInfo(CommandSender commandSender, InfoType type, String argument) {
        if (argument == null) return;
        Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
        if (argument.equalsIgnoreCase("all")) {
            sendAllInfo(audience, type);
            return;
        }
        sendInfo(audience, type, argument);
    }

    private void sendAllInfo(Audience audience, InfoType type) {
        switch (type) {
            case ITEM -> {
                for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                    sendItemInfo(audience, entry.getValue(), entry.getKey());
                }
            }
            case GLYPH -> OraxenPlugin.get().getFontManager().getGlyphs()
                    .forEach(glyph -> sendGlyphInfo(audience, glyph, glyph.getName()));
            case BLOCK -> {
                for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                    if (!OraxenBlocks.isOraxenBlock(entry.getKey())) continue;
                    sendBlockInfo(audience, entry.getKey());
                }
            }
        }
    }

    private void sendInfo(Audience audience, InfoType type, String argument) {
        switch (type) {
            case ITEM -> {
                ItemBuilder itemBuilder = OraxenItems.getItemById(argument);
                if (itemBuilder == null) sendNotFound(audience, type, argument);
                else sendItemInfo(audience, itemBuilder, argument);
            }
            case GLYPH -> {
                Glyph glyph = OraxenPlugin.get().getFontManager().getGlyphFromID(argument);
                if (glyph == null) sendNotFound(audience, type, argument);
                else sendGlyphInfo(audience, glyph, argument);
            }
            case BLOCK -> {
                if (!OraxenBlocks.isOraxenBlock(argument)) sendNotFound(audience, type, argument);
                else sendBlockInfo(audience, argument);
            }
        }
    }

    private void sendNotFound(Audience audience, InfoType type, String argument) {
        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<prefix><red>No " + type.commandName() + " found with " + type.lookupName() + " <white>")
                .append(Component.text(argument, NamedTextColor.WHITE))
                .append(AdventureUtils.MINI_MESSAGE.deserialize("<red>.")));
    }

    private void sendItemInfo(Audience sender, ItemBuilder builder, String itemId) {
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();

        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>ItemID ⏵ <white>" + itemId));
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Material ⏵ <white>" + item.getType()));

        // Basic meta info
        if (meta != null) {
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize(""));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Item Meta"));
            if (meta.hasCustomModelData()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<gray>CustomModelData ⏵ <white>" + meta.getCustomModelData()));
            }
            if (meta.hasItemModel()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<gray>ItemModel ⏵ <white>" + meta.getItemModel()));
            }
            if (meta.hasDisplayName()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<gray>DisplayName ⏵ <white>" + meta.getDisplayName()));
            }
        }

        // OraxenMeta info
        OraxenMeta oraxenMeta = builder.getOraxenMeta();
        if (oraxenMeta != null) {
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize(""));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Oraxen Meta"));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE
                    .deserialize("<gray>ModelName ⏵ <white>" + oraxenMeta.getModelName()));
            if (oraxenMeta.hasPackInfos()) {
                sender.sendMessage(AdventureUtils.MINI_MESSAGE
                        .deserialize("<gray>ModelPath ⏵ <white>" + oraxenMeta.getGeneratedModelPath()));
            }
        }
    }

    private void sendGlyphInfo(Audience audience, Glyph glyph, String glyphId) {
        audience.sendMessage(
                Component.empty()
                        .append(Component.newline())
                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>GlyphID ⏵ <white>"))
                        .append(Component.text(glyphId, NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Texture ⏵ <white>" + glyph.getTexture()))
                        .append(Component.newline())
                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Unicode(s) ⏵ <white>"))
                        .append(Component.newline())
                        .append(glyph.getGlyphComponent().color(NamedTextColor.WHITE)
                                .hoverEvent(HoverEvent.showText(AdventureUtils.MINI_MESSAGE.deserialize("<gold>Click to copy to clipboard.")))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.getCharacters())))
        );
    }

    private void sendBlockInfo(Audience sender, String itemId) {
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>ItemID ⏵ <white>" + itemId));
        if (OraxenBlocks.isOraxenNoteBlock(itemId)) {
            NoteBlockMechanic mechanic = (NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(itemId);
            if (mechanic == null) return;
            NoteBlock data = NoteBlockMechanicFactory.createNoteBlockData(mechanic.getCustomVariation());
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Instrument ⏵ <white>" + data.getInstrument()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Note ⏵ <white>" + data.getNote().getId()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Powered ⏵ <white>" + data.isPowered()));
        } else if (OraxenBlocks.isOraxenStringBlock(itemId)) {
            StringBlockMechanic mechanic = (StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(itemId);
            if (mechanic == null) return;
            Tripwire data = (Tripwire) StringBlockMechanicFactory.createTripwireData(mechanic.getCustomVariation());
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Facing ⏵ <white>" + data.getFaces()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Powered ⏵ <white>" + data.isPowered()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Disarmed ⏵ <white>" + data.isDisarmed()));
        } else {
            ShapedBlockMechanic mechanic = OraxenBlocks.getShapedMechanic(itemId);
            if (mechanic != null) {
                BlockData data = mechanic.getPlacedMaterial().createBlockData();
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Type ⏵ <white>" + mechanic.getBlockType()));
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Variation ⏵ <white>" + mechanic.getCustomVariation()));
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Material ⏵ <white>" + mechanic.getPlacedMaterial()));
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<gray>BlockData ⏵ <white>" + data.getAsString()));
            }
        }
    }

    private enum InfoType {
        ITEM("item", "itemid", "item-id"),
        GLYPH("glyph", "glyphid", "glyph-id"),
        BLOCK("block", "itemid", "item-id");

        private final String commandName;
        private final String argumentName;
        private final String lookupName;

        InfoType(String commandName, String argumentName, String lookupName) {
            this.commandName = commandName;
            this.argumentName = argumentName;
            this.lookupName = lookupName;
        }

        private String commandName() {
            return commandName;
        }

        private String argumentName() {
            return argumentName;
        }

        private String lookupName() {
            return lookupName;
        }
    }
}
