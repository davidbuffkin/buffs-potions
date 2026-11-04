package com.buffspotions.advancement;

import com.buffspotions.BuffsPotions;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.Identifier;

/**
 * Lays the Brewing tab out the way the advancement screen draws lines: every child is one
 * column to the right of its parent, and each branch occupies its own rows so a connector
 * never runs down an unrelated icon.
 */
public final class BrewingTabPlacement {
    private static final Identifier ROOT = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "brewing/root");

    private BrewingTabPlacement() {
    }

    public static void place(AdvancementTree tree) {
        AdvancementNode root = tree.get(ROOT);
        if (root == null || root.advancement().display().isEmpty()) {
            return;
        }
        Group brewing = group(
                "stay_still",
                group("still_waters", group("its_elementary", leaf("vermillion"))),
                group("double_time", leaf("metal_worker")),
                group(
                        "questionable",
                        group("end_of_the_rainbow", leaf("dont_count_your_chickens"), leaf("before_they_hatch"))
                ),
                group("doctor", leaf("pasteurization")),
                group(
                        "stay_standing",
                        leaf("brightest"),
                        leaf("slow_and_steady"),
                        group("begone", leaf("confuse_ray")),
                        group("stay_stacked", leaf("thoughtful"), leaf("seeing_long"), leaf("happy_medium")),
                        group(
                                "see_some_id",
                                group(
                                        "base_spirits",
                                        group("do_i_know_you", leaf("halloween")),
                                        leaf("hard_times"),
                                        leaf("just_why")
                                )
                        )
                )
        );
        Group belts = group(
                "hold_me",
                leaf("hot_swapping"),
                group(
                        "glass_menagerie",
                        leaf("perfect_timing"),
                        group("air_bender", group("furious_cocktail", leaf("how_did_we_get_here")))
                ),
                group("protracted", leaf("nether_again")),
                group("amplified", group("touch_faith", leaf("stacks_on_stacks"))),
                group("whos_hungry", leaf("sweet_tooth"))
        );
        Group thirst = group(
                "whos_thirsty",
                group(
                        "nice_buckle",
                        group(
                                "nice_buckle_question",
                                group("thought_flagon", leaf("mobs_on_demand"), leaf("tip_jar"))
                        )
                ),
                group("elixir_of_life", leaf("anchors_aweigh"))
        );
        Group[] branches = {brewing, belts, thirst};
        float row = 0.0F;
        int usedRows = 0;
        for (Group branch : branches) {
            int used = place(tree, branch, 1.0F, row);
            row += used;
            usedRows += used;
        }
        float rootY = (usedRows - 1) / 2.0F;
        root.advancement().display().ifPresent(display -> display.setLocation(0.0F, rootY));
    }

    /** Places a branch and returns how many rows it occupies. */
    private static int place(AdvancementTree tree, Group group, float x, float y) {
        if (group.children.length == 0) {
            icon(tree, group.name, x, y);
            return 1;
        }
        int height = 0;
        float row = y;
        for (Group child : group.children) {
            int used = place(tree, child, x + 1.0F, row);
            row += used;
            height += used;
        }
        icon(tree, group.name, x, y + (height - 1) / 2.0F);
        return height;
    }

    private static void icon(AdvancementTree tree, String name, float x, float y) {
        AdvancementNode node = tree.get(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "brewing/" + name));
        if (node != null) {
            node.advancement().display().ifPresent(display -> display.setLocation(x, y));
        }
    }

    private static Group leaf(String name) {
        return new Group(name);
    }

    private static Group group(String name, Group... children) {
        return new Group(name, children);
    }

    private static final class Group {
        private final String name;
        private final Group[] children;

        private Group(String name, Group... children) {
            this.name = name;
            this.children = children;
        }
    }
}
