package io.th0rgal.oraxen.painting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class PaintingDatapack extends OraxenDatapack {

    public static final Key DATAPACK_KEY = Key.key("minecraft:file/oraxen_paintings");
    private final Collection<CustomPainting> paintings;

    public PaintingDatapack(Collection<CustomPainting> paintings) {
        super("oraxen_paintings",
                "Datapack for Oraxen's Custom Paintings",
                ResourcePackFormatUtil.getCurrentDataPackFormat());
        this.paintings = paintings;
    }

    @Override
    protected Key getDatapackKey() {
        return DATAPACK_KEY;
    }

    @Override
    public void generateAssets(List<VirtualFile> output) {
        if (paintings.isEmpty()) {
            return;
        }

        if (!writeMCMeta() || !writePaintingVariants() || !writePlaceableTag()) {
            return;
        }

        if (isFirstInstall || !datapackEnabled) {
            Message.DATAPACK_GENERATED.send(Bukkit.getConsoleSender(),
                    TagResolver.resolver(Placeholder.parsed("datapack_name", "Paintings")));
        }

        enableDatapack(true);
    }

    private boolean writePaintingVariants() {
        for (CustomPainting painting : paintings) {
            File paintingFile = datapackFolder.toPath()
                    .resolve("data/" + painting.variantKey().namespace()
                            + "/painting_variant/" + painting.variantKey().value() + ".json")
                    .toFile();

            try {
                paintingFile.getParentFile().mkdirs();
                paintingFile.createNewFile();
                FileUtils.writeStringToFile(paintingFile, painting.toJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private boolean writePlaceableTag() {
        JsonArray values = new JsonArray();
        paintings.stream()
                .filter(CustomPainting::includeInRandom)
                .map(painting -> painting.variantKey().asString())
                .forEach(values::add);

        if (values.isEmpty()) {
            return true;
        }

        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);

        File tagFile = datapackFolder.toPath()
                .resolve("data/minecraft/tags/painting_variant/placeable.json")
                .toFile();

        try {
            tagFile.getParentFile().mkdirs();
            tagFile.createNewFile();
            FileUtils.writeStringToFile(tagFile, tag.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
