package net.asodev.islandutils.mixins;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.asodev.islandutils.options.IslandOptions;
import net.asodev.islandutils.state.COSMETIC_TYPE;
import net.asodev.islandutils.state.CosmeticState;
import net.asodev.islandutils.state.MccIslandState;
import net.asodev.islandutils.util.ChatUtils;
import net.fabricmc.fabric.mixin.client.rendering.InGameHudMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ContainerScreen.class)
public abstract class UIMixin extends AbstractContainerScreen<ChestMenu> {
    public UIMixin(ChestMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    public void renderBg(PoseStack poseStack, float f, int i, int j, CallbackInfo ci) {
        if (!MccIslandState.isOnline()) return;

        IslandOptions options = IslandOptions.getOptions();
        if (!options.isShowPlayerPreview()) return;
        if (IslandOptions.getOptions().isShowOnOnlyCosmeticMenus() && !CosmeticState.isCosmeticMenu(this.menu)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack hatSlot = CosmeticState.hatSlot;
        ItemStack accSlot = CosmeticState.accSlot;

        // 11 - Head
        // 9 - Hat
        // 18 - Accessory
        Slot inspectHeadSlot = this.menu.getSlot(11);
        Slot inspectHatSlot = this.menu.getSlot(9);
        Slot inspectAccSlot = this.menu.getSlot(18);
        boolean isInspectHead = inspectHeadSlot.hasItem() && inspectHeadSlot.getItem().is(Items.PLAYER_HEAD);
        boolean isInspectHat = inspectHeadSlot.hasItem() &&
                (CosmeticState.getType(inspectHatSlot.getItem()) != null) || (inspectHatSlot.getItem().getDisplayName().getString().contains("Hat"));
        if (isInspectHead && isInspectHat) {
            try {
                if (CosmeticState.inspectingPlayer == null) {
                    UUID uuid = inspectHeadSlot.getItem().getTag().getCompound("SkullOwner").getUUID("Id");
                    player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                    CosmeticState.inspectingPlayer = player;
                } else {
                    player = CosmeticState.inspectingPlayer;
                }
                if (player == null) return;

                player.getInventory().selected = 8;

                hatSlot = inspectHatSlot.getItem();
                accSlot = inspectAccSlot.getItem();
            } catch (Exception e) { return; }
        } else {
            ItemStack hover = CosmeticState.getLastHoveredItem();
            COSMETIC_TYPE type = CosmeticState.getLastHoveredItemType();

            if (hatSlot == null || hatSlot.is(Items.AIR)) {
                if (type == COSMETIC_TYPE.HAT && options.isShowOnHover()) hatSlot = hover;
                else hatSlot = CosmeticState.prevHatSlot != null ? CosmeticState.prevHatSlot : ItemStack.EMPTY;
            }
            if (accSlot == null || accSlot.is(Items.AIR)) {
                if (type == COSMETIC_TYPE.ACCESSORY && options.isShowOnHover()) accSlot = hover;
                else accSlot = CosmeticState.prevAccSlot != null ? CosmeticState.prevAccSlot : ItemStack.EMPTY;
            }
            if (hatSlot == null) hatSlot = ItemStack.EMPTY;
            if (accSlot == null) accSlot = ItemStack.EMPTY;

            player.getInventory().armor.set(3, hatSlot);
            player.getInventory().offhand.set(0, accSlot);
        }

        float animPos = player.animationPosition;
        float animSpeed = player.animationSpeed;
        float attackAnim = player.attackAnim;
        player.animationPosition = 0;
        player.animationSpeed = 0;
        player.attackAnim = 0;

        int size = Double.valueOf(Math.ceil(this.imageHeight / 2.5)).intValue();
        int x = (this.width - this.imageWidth) / 4;
        int y = (this.height / 2) + size;

        this.renderPlayerInInventory(
                x, // x
                y,  // y
                size , // size
                player); // Entity

        player.animationPosition = animPos;
        player.animationSpeed = animSpeed;
        player.attackAnim = attackAnim;

        // this code is so ugly omfg
        int itemPos = x+(size / 2) - 18;

        y += 8;
        int backgroundColor = 0x60000000;
        fill(poseStack, x-(size / 2) - 2, y, x+(size / 2)+2, y + 19, backgroundColor);
        drawString(poseStack, this.font, CosmeticState.HAT_COMP, x-(size / 2) + 4, y + 6, 16777215 | 255 << 24);
        this.itemRenderer.renderAndDecorateItem(this.minecraft.player, hatSlot, itemPos, y+2, x + y * this.imageWidth);

        y += 19 + 4;
        fill(poseStack, x-(size / 2) - 2, y, x+(size / 2)+2, y + 19, backgroundColor);
        drawString(poseStack, this.font, CosmeticState.ACCESSORY_COMP, x-(size / 2) + 4, y + 6, 16777215 | 255 << 24);
        this.itemRenderer.renderAndDecorateItem(this.minecraft.player, accSlot, itemPos, y+2, x + y * this.imageWidth);

        if (this.hoveredSlot != null && this.hoveredSlot.getItem() != null) {
            ItemStack currHover = this.hoveredSlot.getItem();
            if (!currHover.is(Items.GHAST_TEAR) & !currHover.is(Items.AIR)) {
                CosmeticState.setLastHoveredItem(this.hoveredSlot.getItem());
            }
        }
    }

    private void renderPlayerInInventory(int x, int y, int size, LivingEntity livingEntity) {
        float yRot = CosmeticState.yRot;
        float xRot = (float)Math.atan(CosmeticState.xRot / 40.0f);;

        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.translate((double)x, (double)y, 1050.0);
        poseStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack poseStack2 = new PoseStack();
        poseStack2.translate(0.0, 0.0, 1000.0);
        poseStack2.scale((float)size, (float)size, (float)size);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(xRot * 20F);
        quaternion.mul(quaternion2);
        poseStack2.mulPose(quaternion);
        float m = livingEntity.yBodyRot;
        float n = livingEntity.getYRot();
        float o = livingEntity.getXRot();
        float p = livingEntity.yHeadRotO;
        float q = livingEntity.yHeadRot;
        livingEntity.yBodyRot = yRot;
        livingEntity.setYRot(yRot);
        livingEntity.yHeadRot = livingEntity.yBodyRot;
        livingEntity.yHeadRotO = livingEntity.yBodyRot;

        livingEntity.setXRot(-xRot * 20F);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(livingEntity, 0.0, 0.0, 0.0, 0.0F, 1.0F, poseStack2, bufferSource, 15728880);
        });
        bufferSource.endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        livingEntity.yBodyRot = m;
        livingEntity.setYRot(n);
        livingEntity.setXRot(o);
        livingEntity.yHeadRotO = p;
        livingEntity.yHeadRot = q;
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}