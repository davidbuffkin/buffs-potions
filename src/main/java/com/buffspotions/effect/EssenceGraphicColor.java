package com.buffspotions.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Potion color for an essence, taken from the item or block graphic that was brewed.
 * Transparent pixels are skipped. Biome and constant tints are applied to the faces that use them.
 */
public final class EssenceGraphicColor {
    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private EssenceGraphicColor() {
    }

    public static int of(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return CACHE.computeIfAbsent(id.toString(), key -> compute(item, id));
    }

    private static int compute(Item item, Identifier id) {
        try {
            int graphic = averageGraphic(id);
            if (graphic >= 0) {
                return graphic;
            }
        } catch (RuntimeException | IOException ignored) {
            // A missing model falls through to the map color.
        }
        if (item instanceof BlockItem blockItem) {
            int color = blockItem.getBlock().defaultMapColor().col;
            if (color != 0) {
                return color;
            }
        }
        return EssenceSubject.ITEM_COLOR;
    }

    private static int averageGraphic(Identifier itemId) throws IOException {
        JsonObject itemJson = readJson(asset(itemId, "items", ".json"));
        if (itemJson == null) {
            itemJson = new JsonObject();
        }
        ModelChoice choice = findModel(itemJson);
        String model = choice == null ? itemId.getNamespace() + ":item/" + itemId.getPath() : choice.model;
        Integer tint = choice == null ? null : choice.tint;
        Map<String, String> textures = textureMap(model);
        String layer = resolve(textures, "layer0");
        if (layer != null) {
            return averageTexture(layer, tint);
        }
        List<Face> faces = faces(model);
        long red = 0;
        long green = 0;
        long blue = 0;
        int count = 0;
        for (Face face : faces) {
            String texture = resolve(textures, face.texture);
            if (texture == null) {
                continue;
            }
            int color = averageTexture(texture, face.tinted ? tint : null);
            if (color < 0) {
                continue;
            }
            red += (color >> 16) & 0xFF;
            green += (color >> 8) & 0xFF;
            blue += color & 0xFF;
            count++;
        }
        if (count == 0) {
            String particle = resolve(textures, "particle");
            return particle == null ? -1 : averageTexture(particle, null);
        }
        return (int) ((red / count) << 16 | (green / count) << 8 | (blue / count));
    }

    private static Map<String, String> textureMap(String modelId) throws IOException {
        Map<String, String> textures = new LinkedHashMap<>();
        String current = modelId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current) && !current.contains("builtin/")) {
            JsonObject model = readJson(asset(identifier(current), "models", ".json"));
            if (model == null) {
                break;
            }
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : model.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        textures.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            current = model.has("parent") && model.get("parent").isJsonPrimitive() ? model.get("parent").getAsString() : null;
        }
        return textures;
    }

    private static List<Face> faces(String modelId) throws IOException {
        String current = modelId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current) && !current.contains("builtin/")) {
            JsonObject model = readJson(asset(identifier(current), "models", ".json"));
            if (model == null) {
                break;
            }
            if (model.has("elements") && model.get("elements").isJsonArray()) {
                List<Face> faces = new ArrayList<>();
                for (JsonElement element : model.getAsJsonArray("elements")) {
                    if (!element.isJsonObject() || !element.getAsJsonObject().has("faces")) {
                        continue;
                    }
                    JsonElement faceElement = element.getAsJsonObject().get("faces");
                    if (!faceElement.isJsonObject()) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> face : faceElement.getAsJsonObject().entrySet()) {
                        if (!face.getValue().isJsonObject()) {
                            continue;
                        }
                        JsonObject faceObject = face.getValue().getAsJsonObject();
                        if (!faceObject.has("texture") || !faceObject.get("texture").isJsonPrimitive()) {
                            continue;
                        }
                        faces.add(new Face(faceObject.get("texture").getAsString(), faceObject.has("tintindex")));
                    }
                }
                return faces;
            }
            current = model.has("parent") && model.get("parent").isJsonPrimitive() ? model.get("parent").getAsString() : null;
        }
        return List.of();
    }

    private static String resolve(Map<String, String> textures, String name) {
        if (name == null) {
            return null;
        }
        String current = textures.getOrDefault(name, name);
        for (int guard = 0; current.startsWith("#") && guard < 8; guard++) {
            String next = textures.get(current.substring(1));
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current.startsWith("#") ? null : current;
    }

    private static int averageTexture(String texture, Integer tint) throws IOException {
        BufferedImage image = readImage(asset(identifier(texture), "textures", ".png"));
        if (image == null) {
            return -1;
        }
        long red = 0;
        long green = 0;
        long blue = 0;
        long weight = 0;
        int tintRed = tint == null ? 255 : (tint >> 16) & 0xFF;
        int tintGreen = tint == null ? 255 : (tint >> 8) & 0xFF;
        int tintBlue = tint == null ? 255 : tint & 0xFF;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < 16) {
                    continue;
                }
                red += ((argb >>> 16) & 0xFF) * tintRed / 255 * (long) alpha;
                green += ((argb >>> 8) & 0xFF) * tintGreen / 255 * (long) alpha;
                blue += (argb & 0xFF) * tintBlue / 255 * (long) alpha;
                weight += alpha;
            }
        }
        if (weight == 0) {
            return -1;
        }
        return (int) ((red / weight) << 16 | (green / weight) << 8 | (blue / weight));
    }

    private static ModelChoice findModel(JsonElement element) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                ModelChoice found = findModel(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("model") && object.get("model").isJsonPrimitive()) {
            return new ModelChoice(object.get("model").getAsString(), tintOf(object));
        }
        if (object.has("base") && object.get("base").isJsonPrimitive()) {
            return new ModelChoice(object.get("base").getAsString(), tintOf(object));
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            ModelChoice found = findModel(entry.getValue());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Integer tintOf(JsonObject object) {
        if (!object.has("tints") || !object.get("tints").isJsonArray()) {
            return null;
        }
        JsonArray tints = object.getAsJsonArray("tints");
        if (tints.isEmpty() || !tints.get(0).isJsonObject()) {
            return null;
        }
        JsonObject tint = tints.get(0).getAsJsonObject();
        if (!tint.has("type") || !tint.get("type").isJsonPrimitive()) {
            return null;
        }
        String type = tint.get("type").getAsString();
        try {
            if (type.endsWith("constant") || type.endsWith("dye") || type.endsWith("potion")) {
                String field = tint.has("value") ? "value" : "default";
                if (!tint.has(field)) {
                    return null;
                }
                return tint.get(field).getAsInt() & 0xFFFFFF;
            }
            if (type.endsWith("grass") || type.endsWith("foliage")) {
                double temperature = tint.has("temperature") ? tint.get("temperature").getAsDouble() : 0.5;
                double downfall = tint.has("downfall") ? tint.get("downfall").getAsDouble() : 1.0;
                String colormap = type.endsWith("dry_foliage")
                        ? "dry_foliage"
                        : type.endsWith("foliage") ? "foliage" : "grass";
                return sampleColormap(colormap, temperature, downfall);
            }
        } catch (RuntimeException | IOException ignored) {
            return null;
        }
        return null;
    }

    private static Integer sampleColormap(String name, double temperature, double downfall) throws IOException {
        BufferedImage image = readImage("/assets/minecraft/textures/colormap/" + name + ".png");
        if (image == null) {
            return null;
        }
        downfall *= temperature;
        int x = Mth.clamp((int) ((1.0 - temperature) * (image.getWidth() - 1)), 0, image.getWidth() - 1);
        int y = Mth.clamp((int) ((1.0 - downfall) * (image.getHeight() - 1)), 0, image.getHeight() - 1);
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream stream = open(path)) {
            if (stream == null) {
                return null;
            }
            JsonElement parsed = JsonParser.parseString(new String(stream.readAllBytes()));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        }
    }

    private static BufferedImage readImage(String path) throws IOException {
        try (InputStream stream = open(path)) {
            return stream == null ? null : ImageIO.read(stream);
        }
    }

    private static InputStream open(String path) {
        InputStream stream = EssenceGraphicColor.class.getResourceAsStream(path);
        if (stream != null) {
            return stream;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? null : loader.getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
    }

    private static String asset(Identifier id, String folder, String suffix) {
        return "/assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + suffix;
    }

    private static Identifier identifier(String raw) {
        int split = raw.indexOf(':');
        if (split < 0) {
            return Identifier.withDefaultNamespace(raw);
        }
        return Identifier.fromNamespaceAndPath(raw.substring(0, split), raw.substring(split + 1));
    }

    private record ModelChoice(String model, Integer tint) {
    }

    private record Face(String texture, boolean tinted) {
    }
}
