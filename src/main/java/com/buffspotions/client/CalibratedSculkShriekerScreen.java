package com.buffspotions.client;

import com.buffspotions.network.ShriekerSavePayload;
import com.buffspotions.shrieker.ShriekerText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** One text field, Save, and Cancel. Leaving without Save drops the draft. */
public final class CalibratedSculkShriekerScreen extends Screen {
    private final BlockPos pos;
    private final String saved;
    private FormattingEditBox input;

    public CalibratedSculkShriekerScreen(BlockPos pos, String saved) {
        super(Component.translatable("block.buffs_potions.calibrated_sculk_shrieker"));
        this.pos = pos;
        this.saved = saved;
    }

    @Override
    protected void init() {
        this.input = new FormattingEditBox(
                this.font,
                this.width / 2 - 150,
                64,
                300,
                20,
                Component.translatable("block.buffs_potions.calibrated_sculk_shrieker")
        );
        this.input.setMaxLength(ShriekerText.MAX_LENGTH);
        this.input.setValue(this.saved);
        this.addWidget(this.input);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.buffs_potions.save"), button -> this.save())
                .bounds(this.width / 2 - 154, 100, 150, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(this.width / 2 + 4, 100, 150, 20)
                .build());
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(int width, int height) {
        String draft = this.input.getValue();
        this.init(width, height);
        this.input.setValue(draft);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.isConfirmation()) {
            this.save();
            return true;
        }
        return false;
    }

    private void save() {
        ClientPacketDistributor.sendToServer(new ShriekerSavePayload(this.pos, this.input.getValue()));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 40, -1);
        this.input.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
