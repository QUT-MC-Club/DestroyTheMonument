package eu.pb4.destroythemonument.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.*;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DtmTridentItem extends TridentItem implements PolymerItem {

    public DtmTridentItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.minecraft.trident");
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.TRIDENT;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return PolymerItem.super.getPolymerItemModel(Items.TRIDENT.getDefaultStack(), context);
    }

    private static final Map<PlayerEntity, UUID> lastThrownTridentUUID = new HashMap<>();

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        ItemStack stackInHand = user.getStackInHand(user.getActiveHand());
        if (user instanceof PlayerEntity playerEntity) {
            int i = this.getMaxUseTime(stack, user) - remainingUseTicks;
            if (i < 10) {
                return false;
            } else {
                float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, playerEntity);
                if (f > 0.0F && !playerEntity.isTouchingWaterOrRain()) {
                    return false;
                } else if (stack.willBreakNextUse()) {
                    return false;
                } else {
                    RegistryEntry<SoundEvent> registryEntry = (RegistryEntry)EnchantmentHelper.getEffect(stack, EnchantmentEffectComponentTypes.TRIDENT_SOUND).orElse(SoundEvents.ITEM_TRIDENT_THROW);
                    playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
                    if (world instanceof ServerWorld) {
                        ServerWorld serverWorld = (ServerWorld) world;
                        stack.damage(1, playerEntity);

                        if (f == 0.0F) {
                            removeLastThrownTrident(playerEntity, serverWorld);
                            TridentEntity tridentEntity = (TridentEntity) ProjectileEntity.spawnWithVelocity(TridentEntity::new, serverWorld, stack, playerEntity, 0.0F, 2.5F, 1.0F);
                            if (playerEntity.isInCreativeMode()) {
                                tridentEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                            } else {
                                tridentEntity.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                                ((PlayerEntity) user).getItemCooldownManager().set(stackInHand, 80);
                            }

                            lastThrownTridentUUID.put(playerEntity, tridentEntity.getUuid());

                            world.playSoundFromEntity((PlayerEntity) null, tridentEntity, (SoundEvent) registryEntry.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                            return true;
                        }
                    }

                    if (f > 0.0F) {
                        float g = playerEntity.getYaw();
                        float h = playerEntity.getPitch();
                        float j = -MathHelper.sin(g * ((float)Math.PI / 180F)) * MathHelper.cos(h * ((float)Math.PI / 180F));
                        float k = -MathHelper.sin(h * ((float)Math.PI / 180F));
                        float l = MathHelper.cos(g * ((float)Math.PI / 180F)) * MathHelper.cos(h * ((float)Math.PI / 180F));
                        float m = MathHelper.sqrt(j * j + k * k + l * l);
                        j *= f / m;
                        k *= f / m;
                        l *= f / m;
                        playerEntity.addVelocity((double)j, (double)k, (double)l);
                        playerEntity.useRiptide(20, 8.0F, stack);
                        if (playerEntity.isOnGround()) {
                            float n = 1.1999999F;
                            playerEntity.move(MovementType.SELF, new Vec3d((double)0.0F, (double)1.1999999F, (double)0.0F));
                        }

                        world.playSoundFromEntity((PlayerEntity)null, playerEntity, (SoundEvent)registryEntry.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }


    private void removeLastThrownTrident(PlayerEntity playerEntity, ServerWorld serverWorld) {
        UUID lastTridentUuid = lastThrownTridentUUID.get(playerEntity);

        if (lastTridentUuid != null) {
            Entity entity = serverWorld.getEntity(lastTridentUuid);
            if (entity instanceof TridentEntity tridentEntity && tridentEntity.getOwner() == playerEntity) {
                tridentEntity.discard();
            }
        }
    }
}
