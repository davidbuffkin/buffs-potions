package com.buffspotions.advancement;

import com.buffspotions.BuffsPotions;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;

/** Keeps Expelliarmus in the row directly under Fishy Business. */
public final class ExpelliarmusPlacement {
    private static final Identifier FISHY = Identifier.withDefaultNamespace("husbandry/fishy_business");
    private static final Identifier EXPELLIARMUS = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "husbandry/expelliarmus");
    private static final float ROW = 0.001F;

    private ExpelliarmusPlacement() {
    }

    public static void place(AdvancementTree tree) {
        AdvancementNode root = tree.get(Identifier.withDefaultNamespace("husbandry/root"));
        AdvancementNode fishy = tree.get(FISHY);
        AdvancementNode ours = tree.get(EXPELLIARMUS);
        if (root == null || fishy == null || ours == null) {
            return;
        }
        DisplayInfo fishyDisplay = fishy.advancement().display().orElse(null);
        DisplayInfo ourDisplay = ours.advancement().display().orElse(null);
        if (fishyDisplay == null || ourDisplay == null) {
            return;
        }
        float fishyY = fishyDisplay.getY();
        float oldY = ourDisplay.getY();
        if (oldY > fishyY + ROW) {
            nudge(root, ours, fishyY, oldY, true);
            ourDisplay.setLocation(fishyDisplay.getX(), fishyY + 1.0F);
        } else if (oldY < fishyY - ROW) {
            nudge(root, ours, fishyY, oldY, false);
            ourDisplay.setLocation(fishyDisplay.getX(), fishyY);
        } else {
            nudgeBelow(root, ours, fishyY);
            ourDisplay.setLocation(fishyDisplay.getX(), fishyY + 1.0F);
        }
    }

    /** Opens the row under Fishy Business, and closes the row this advancement leaves behind. */
    private static void nudge(AdvancementNode node, AdvancementNode ours, float fishyY, float oldY, boolean fromBelow) {
        if (node != ours && node.parent() != null) {
            node.advancement().display().ifPresent(display -> {
                float y = display.getY();
                float delta = 0.0F;
                if (fromBelow) {
                    if (y > fishyY + ROW && y < oldY - ROW) {
                        delta = 1.0F;
                    }
                } else if (y > oldY + ROW && y <= fishyY + ROW) {
                    delta = -1.0F;
                }
                if (delta != 0.0F) {
                    display.setLocation(display.getX(), y + delta);
                }
            });
        }
        for (AdvancementNode child : node.children()) {
            nudge(child, ours, fishyY, oldY, fromBelow);
        }
    }

    private static void nudgeBelow(AdvancementNode node, AdvancementNode ours, float fishyY) {
        if (node != ours && node.parent() != null) {
            node.advancement().display().ifPresent(display -> {
                if (display.getY() > fishyY + ROW) {
                    display.setLocation(display.getX(), display.getY() + 1.0F);
                }
            });
        }
        for (AdvancementNode child : node.children()) {
            nudgeBelow(child, ours, fishyY);
        }
    }
}
