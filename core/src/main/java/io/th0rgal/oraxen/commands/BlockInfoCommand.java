package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;

import java.util.Map;

public class BlockInfoCommand {

    CommandAPICommand getBlockInfoCommand() {
        return new CommandAPICommand("blockinfo")
                .withPermission("oraxen.command.blockinfo")
                .withArguments(new StringArgument("itemid").replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())))
                .executes((commandSender, args) -> {
                    String argument = (String) args.get("itemid");
                    Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
                    if (argument == null) return;
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            if (!OraxenBlocks.isOraxenBlock(entry.getKey())) continue;
                            sendBlockInfo(audience, entry.getKey());
                        }
                    } else {
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if (ib == null)
                            audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<prefix><red>No block found with item-id <white>" + argument + "<red>."));
                        else sendBlockInfo(audience, argument);
                    }
                });
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
}
