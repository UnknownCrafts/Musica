package wraith.musica.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import wraith.musica.Utils;

import java.util.List;

public class SongMixerScreen extends HandledScreen<ScreenHandler> {

    private static final Identifier TEXTURE = Utils.ID("textures/gui/song_mixer.png");
    private float scrollAmount;
    private boolean mouseClicked;
    private int scrollOffset;
    private boolean canCraft;

    public SongMixerScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        ((SongMixerScreenHandler)handler).setContentsChangedListener(this::onInventoryChange);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        this.renderBackground(matrices);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = this.x;
        int j = this.y;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        int k = (int)(41.0F * this.scrollAmount);
        this.drawTexture(matrices, i + 119, j + 15 + k, 176 + (this.shouldScroll() ? 0 : 12), 0, 12, 15);
        int l = this.x + 52;
        int m = this.y + 14;
        int n = this.scrollOffset + 12;
        this.renderRecipeBackground(matrices, mouseX, mouseY, l, m, n);
        this.renderRecipeIcons(l, m, n);
    }

    @Override
    protected void drawMouseoverTooltip(MatrixStack matrices, int x, int y) {
        super.drawMouseoverTooltip(matrices, x, y);
        if (this.canCraft) {
            int i = this.x + 52;
            int j = this.y + 14;
            int k = this.scrollOffset + 12;
            List<Item> list = SongMixerScreenHandler.availableDiscs;

            for(int l = this.scrollOffset; l < k && l < SongMixerScreenHandler.availableDiscs.size(); ++l) {
                int m = l - this.scrollOffset;
                int n = i + m % 4 * 16;
                int o = j + m / 4 * 18 + 2;
                if (x >= n && x < n + 16 && y >= o && y < o + 18) {
                    this.renderTooltip(matrices, new ItemStack(list.get(l)), x, y);
                }
            }
        }

    }

    private void renderRecipeBackground(MatrixStack matrixStack, int i, int j, int k, int l, int m) {
        if (!((SongMixerScreenHandler)handler).canCraft()) {
            return;
        }
        for(int n = this.scrollOffset; n < m && n < SongMixerScreenHandler.availableDiscs.size(); ++n) {
            int o = n - this.scrollOffset;
            int p = k + o % 4 * 16;
            int q = o / 4;
            int r = l + q * 18 + 2;
            int s = this.backgroundHeight;
            if (n == ((SongMixerScreenHandler)this.handler).getSelectedRecipe()) {
                s += 18;
            } else if (i >= p && j >= r && i < p + 16 && j < r + 18) {
                s += 36;
            }

            this.drawTexture(matrixStack, p, r - 1, 0, s, 16, 18);
        }

    }

    private void renderRecipeIcons(int x, int y, int scrollOffset) {
        if (!((SongMixerScreenHandler)handler).canCraft()) {
            return;
        }
        List<Item> list = SongMixerScreenHandler.availableDiscs;

        for(int i = this.scrollOffset; i < scrollOffset && i < list.size(); ++i) {
            int j = i - this.scrollOffset;
            int k = x + j % 4 * 16;
            int l = j / 4;
            int m = y + l * 18 + 2;
            this.client.getItemRenderer().renderInGuiWithOverrides(new ItemStack(list.get(i)), k, m);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.mouseClicked = false;
        if (this.canCraft) {
            int i = this.x + 52;
            int j = this.y + 14;
            int k = this.scrollOffset + 12;

            for(int l = this.scrollOffset; l < k; ++l) {
                int m = l - this.scrollOffset;
                double d = mouseX - (double)(i + m % 4 * 16);
                double e = mouseY - (double)(j + m / 4 * 18);
                if (d >= 0.0D && e >= 0.0D && d < 16.0D && e < 18.0D && this.handler.onButtonClick(this.client.player, l)) {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    NbtCompound tag = new NbtCompound();
                    tag.putInt("sync_id", handler.syncId);
                    tag.putInt("click_slot", l);
                    PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer()).writeNbt(tag);
                    ClientPlayNetworking.send(Utils.ID("song_mixer.click_disc"), packet);
                    return true;
                }
            }

            i = this.x + 119;
            j = this.y + 9;
            if (mouseX >= (double)i && mouseX < (double)(i + 12) && mouseY >= (double)j && mouseY < (double)(j + 54)) {
                this.mouseClicked = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.mouseClicked && this.shouldScroll()) {
            int i = this.y + 14;
            int j = i + 54;
            this.scrollAmount = ((float)mouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0F, 1.0F);
            this.scrollOffset = (int)((double)(this.scrollAmount * (float)this.getMaxScroll()) + 0.5D) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.shouldScroll()) {
            int i = this.getMaxScroll();
            this.scrollAmount = (float)((double)this.scrollAmount - amount / (double)i);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0F, 1.0F);
            this.scrollOffset = (int)((double)(this.scrollAmount * (float)i) + 0.5D) * 4;
        }

        return true;
    }

    private boolean shouldScroll() {
        return this.canCraft && SongMixerScreenHandler.availableDiscs.size() > 12;
    }

    protected int getMaxScroll() {
        return (SongMixerScreenHandler.availableDiscs.size() + 4 - 1) / 4 - 3;
    }

    private void onInventoryChange() {
        this.canCraft = ((SongMixerScreenHandler)this.handler).canCraft();
        if (!this.canCraft) {
            this.scrollAmount = 0.0F;
            this.scrollOffset = 0;
        }

    }

}